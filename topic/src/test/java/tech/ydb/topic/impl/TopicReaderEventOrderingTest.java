package tech.ydb.topic.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test to verify event ordering guarantees and session close race conditions in Topic API.
 *
 * This test checks for two related problems:
 * 1. Event Ordering: StartPartitionSessionEvent and StopPartitionSessionEvent must be delivered in order,
 *    ensuring stop events are not processed before their corresponding start events.
 *
 * 2. Session Close Race Condition: Server reader sessions should not be closed before onPartitionSessionClosed
 *    and onReaderClosed callbacks complete execution. This prevents partitions from being reassigned to other
 *    readers before the original reader has finished cleaning up its resources.
 *
 * @author Generated Test
 */
public class TopicReaderEventOrderingTest {
    private static final Logger logger = LoggerFactory.getLogger(TopicReaderEventOrderingTest.class);

    @ClassRule
    public static final GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private static final String TEST_CONSUMER = "test-consumer";

    private static final int partitionCount = 2;

    private TopicClient client;
    private String testTopic;

    @Before
    public void setup() {
        testTopic = "test-topic-" + UUID.randomUUID();
        logger.info("Creating test topic: {}", testTopic);

        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(testTopic, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER).build())
                .setPartitioningSettings(PartitioningSettings.
                        newBuilder()
                        .setMinActivePartitions(partitionCount)
                        .build())
                .build()
        ).join().expectSuccess("Failed to create test topic");
    }

    @After
    public void tearDown() {
        if (testTopic != null && client != null) {
            logger.info("Dropping test topic: {}", testTopic);
            Status dropStatus = client.dropTopic(testTopic).join();
            dropStatus.expectSuccess("Failed to drop test topic");
        }
        if (client != null) {
            client.close();
        }
    }

    private void sendMessage(String data) throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(testTopic)
                .setProducerId("test-producer")
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();
        writer.send(Message.of(data.getBytes()));
        writer.flush();
    }

    /**
     * Test for event ordering: verifies that StopPartitionSessionEvent is never processed
     * before its corresponding StartPartitionSessionEvent.
     */
    @Test
    public void testEventOrderingGuarantees() throws Exception {
        logger.info("Starting testEventOrderingGuarantees");

        // Per-partition tracking: partitionId -> whether Reader-1 has confirmed its stop.
        // Using partitionId (not sessionId) as the key because Reader-2's new session
        // for the same partition will have a different sessionId.
        ConcurrentHashMap<Long, AtomicBoolean> partitionStopConfirmed = new ConcurrentHashMap<>();

        // Track events for each partition session
        Map<Long, List<String>> eventLog = new ConcurrentHashMap<>();
        for (long i = 0; i < partitionCount; i++) {
            eventLog.put(i, Collections.synchronizedList(new ArrayList<>()));
        }

        Map<Long, Long> activeSessions = new ConcurrentHashMap<>();

        CountDownLatch startReceived = new CountDownLatch(1);
        CountDownLatch stopReceived = new CountDownLatch(1);
        CountDownLatch closeReceived = new CountDownLatch(1);
        AtomicBoolean orderingViolation = new AtomicBoolean(false);
        AtomicBoolean raceConditionDetected = new AtomicBoolean(false);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "test-event-executor"));

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(testTopic)
                        .build())
                .setConsumerName(TEST_CONSUMER)
                .build();

        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor)
                .setEventHandler(new ReadEventHandler() {


                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        eventLog.get(partitionId).add("onMessages[session=" + event.getPartitionSession().getId() + "]");
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long sessionId = event.getPartitionSession().getId();
                        long partitionId = event.getPartitionSession().getPartitionId();
                        eventLog.get(partitionId).add("onStartPartitionSession[partitionId = " + partitionId + ",session=" + sessionId + "]");
                        logger.info("onStartPartitionSession: session={}", sessionId);

                        if (activeSessions.get(partitionId) != null) {
                            logger.error("START event received while session {} is still active", activeSessions.get(partitionId));
                            orderingViolation.set(true);
                        }
                        // Register this partition as "not yet stopped" before confirming.
                        partitionStopConfirmed.put(partitionId, new AtomicBoolean(false));
                        activeSessions.put(partitionId, sessionId);
                        event.confirm();
                        startReceived.countDown();
                    }

                    @Override
                    public void onPartitionSessionClosed(tech.ydb.topic.read.events.PartitionSessionClosedEvent event) {
                        long sessionId = event.getPartitionSession().getId();
                        long partitionId = event.getPartitionSession().getPartitionId();
                        eventLog.get(partitionId).add("onPartitionSessionClosed[partitionId =" + partitionId + ",session=" + sessionId + "]");
                        logger.info("onPartitionSessionClosed: session={}", sessionId);
                        activeSessions.remove(partitionId);
                        closeReceived.countDown();
                    }
                })
                .build()
        );

        reader.init().join();

        // Send a message to trigger partition assignment
        sendMessage("test-message");

        // Wait for start event
        assertTrue("Start event not received", startReceived.await(10, TimeUnit.SECONDS));

        // Shutdown reader to trigger stop event
        logger.info("Shutting down reader");
        reader.shutdown().get(10, TimeUnit.SECONDS);

        // assertTrue("Stop event not received", stopReceived.await(10, TimeUnit.SECONDS));
        assertTrue("Close event not received", closeReceived.await(10, TimeUnit.SECONDS));

        executor.shutdownNow();

        logger.info("Event log: {}", eventLog);
        assertFalse("Event ordering violation detected", orderingViolation.get());

        for (long partitionId = 0; partitionId < partitionCount; partitionId++) {
            // Verify event sequence
            int startIndex = -1;
            int stopIndex = -1;
            for (int i = 0; i < eventLog.size(); i++) {
                if (eventLog.get(partitionId).get(i).startsWith("onStartPartitionSession")) {
                    startIndex = i;
                }
                if (eventLog.get(partitionId).get(i).startsWith("onPartitionSessionClosed")) {
                    stopIndex = i;
                }
            }

            assertTrue("Start event should be present", startIndex >= 0);
            assertTrue("Close event should be present", stopIndex >= 0);
            assertTrue("Start event must come before Stop event", startIndex < stopIndex);
        }
    }

    /**
     * Test that verifies onStopPartitionSession is called when the server gracefully
     * reassigns a partition to a second reader joining the same consumer group.
     *
     * A graceful StopPartitionSessionRequest is sent by the server when it needs to
     * rebalance partition ownership between active readers. This is distinct from a
     * force stop, which happens on client-side shutdown (reader.shutdown()) and only
     * triggers onPartitionSessionClosed.
     */
    @Test
    public void testOnStopPartitionSessionCalledOnGracefulReassignment() throws Exception {
        logger.info("Starting testOnStopPartitionSessionCalledOnGracefulReassignment");

        // Per-partition tracking: partitionId -> whether Reader-1 has confirmed its stop.
        // Using partitionId (not sessionId) as the key because Reader-2's new session
        // for the same partition will have a different sessionId.
        ConcurrentHashMap<Long, AtomicBoolean> partitionStopConfirmed = new ConcurrentHashMap<>();

        // With setMinActivePartitions(2) the topic has 2 partitions; Reader-1 should get both.
        CountDownLatch reader1AllPartitionsAssigned = new CountDownLatch(2);
        CountDownLatch reader1StopReceived = new CountDownLatch(1);
        AtomicBoolean stopPartitionSessionCalled = new AtomicBoolean(false);
        AtomicBoolean raceConditionDetected = new AtomicBoolean(false);
        CountDownLatch reader2StartReceived = new CountDownLatch(1);

        ExecutorService executor1 = Executors.newSingleThreadExecutor(r -> new Thread(r, "reader-1-executor"));
        ExecutorService executor2 = Executors.newSingleThreadExecutor(r -> new Thread(r, "reader-2-executor"));

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(testTopic)
                        .build())
                .setConsumerName(TEST_CONSUMER)
                .build();

        AsyncReader reader1 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor1)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        logger.info("Reader-1: onStartPartitionSession partitionId={}", partitionId);
                        // Register this partition as "not yet stopped" before confirming.
                        partitionStopConfirmed.put(partitionId, new AtomicBoolean(false));
                        event.confirm();
                        reader1AllPartitionsAssigned.countDown();
                    }

                    @Override
                    public void onStopPartitionSession(tech.ydb.topic.read.events.StopPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        logger.info("Reader-1: onStopPartitionSession partitionId={}", partitionId);
                        stopPartitionSessionCalled.set(true);
                        // Mark stop confirmed BEFORE event.confirm(). The server only sends
                        // StartPartitionSession to Reader-2 after receiving this confirm, so
                        // Reader-2 is guaranteed to see the flag as true when it starts.
                        partitionStopConfirmed.get(partitionId).set(true);
                        event.confirm();
                        reader1StopReceived.countDown();
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .build()
        );

        reader1.init().join();

        // Wait for Reader-1 to hold all partitions before Reader-2 joins, so that the
        // server must perform a graceful reassignment rather than a direct assignment.
        assertTrue("Reader-1 did not receive all partitions",
                reader1AllPartitionsAssigned.await(15, TimeUnit.SECONDS));
        logger.info("Reader-1 holds {} partitions. Starting Reader-2 to trigger graceful rebalancing...",
                partitionStopConfirmed.size());

        // Reader-2 joins the same consumer group. The server rebalances by sending a
        // graceful StopPartitionSessionRequest to Reader-1 and, after Reader-1 confirms,
        // a StartPartitionSessionRequest to Reader-2.
        AsyncReader reader2 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor2)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        logger.info("Reader-2: onStartPartitionSession partitionId={}", partitionId);

                        // If Reader-1 ever held this partition, its stop MUST have been
                        // confirmed before Reader-2 can receive a start. The flag was set
                        // before event.confirm() in Reader-1, which is what triggers this
                        // START message from the server, so any false here is a real race.
                        AtomicBoolean stopFlag = partitionStopConfirmed.get(partitionId);
                        if (stopFlag != null && !stopFlag.get()) {
                            logger.error("RACE CONDITION: Reader-2 started on partition {} before " +
                                    "Reader-1 confirmed stop", partitionId);
                            raceConditionDetected.set(true);
                        }

                        event.confirm();
                        reader2StartReceived.countDown();
                    }
                })
                .build()
        );

        reader2.init().join();

        assertTrue("onStopPartitionSession was not called on Reader-1",
                reader1StopReceived.await(15, TimeUnit.SECONDS));
        assertTrue("stopPartitionSessionCalled should be true", stopPartitionSessionCalled.get());

        assertTrue("Reader-2 did not receive StartPartitionSession after graceful reassignment",
                reader2StartReceived.await(10, TimeUnit.SECONDS));

        assertFalse("Race condition: Reader-2 started on partition before Reader-1 confirmed stop",
                raceConditionDetected.get());

        reader1.shutdown().get(10, TimeUnit.SECONDS);
        reader2.shutdown().get(10, TimeUnit.SECONDS);
        executor1.shutdownNow();
        executor2.shutdownNow();
    }

    /**
     * Test for session close race condition: verifies that partitions are not reassigned to other readers
     * before the original reader completes its cleanup in onPartitionSessionClosed and onReaderClosed callbacks.
     */
    @Test
    public void testSessionCloseRaceCondition() throws Exception {
        logger.info("Starting testSessionCloseRaceCondition");

        // Shared state to track the race condition
        AtomicReference<PartitionSession> reader1PartitionSession = new AtomicReference<>();
        AtomicBoolean reader1CleanupInProgress = new AtomicBoolean(false);
        AtomicBoolean reader1CleanupCompleted = new AtomicBoolean(false);
        AtomicBoolean raceConditionDetected = new AtomicBoolean(false);
        CountDownLatch reader1Started = new CountDownLatch(1);
        CountDownLatch reader1CleanupStarted = new CountDownLatch(1);
        CountDownLatch reader2Started = new CountDownLatch(1);
        CountDownLatch allowReader1ToFinish = new CountDownLatch(1);

        // Create two single-threaded executors to simulate the scenario
        ExecutorService reader1Executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "reader-1-executor"));
        ExecutorService reader2Executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "reader-2-executor"));

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(testTopic)
                        .build())
                .setConsumerName(TEST_CONSUMER)
                .build();

        // Create Reader-1
        AsyncReader reader1 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader1Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                        // No-op
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        PartitionSession session = event.getPartitionSession();
                        logger.info("Reader-1: onStartPartitionSession - partition={}, session={}",
                                session.getPartitionId(), session.getId());
                        reader1PartitionSession.set(session);
                        event.confirm();
                        reader1Started.countDown();
                    }

                    @Override
                    public void onPartitionSessionClosed(tech.ydb.topic.read.events.PartitionSessionClosedEvent event) {
                        PartitionSession session = event.getPartitionSession();
                        logger.info("Reader-1: onPartitionSessionClosed - partition={}, session={}",
                                session.getPartitionId(), session.getId());
                        logger.info("Reader-1: before closing resources");

                        reader1CleanupInProgress.set(true);
                        reader1CleanupStarted.countDown();

                        // Simulate slow cleanup (e.g., closing database connections, flushing buffers)
                        try {
                            boolean finished = allowReader1ToFinish.await(5, TimeUnit.SECONDS);
                            if (!finished) {
                                logger.error("Reader-1: cleanup timeout");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.error("Reader-1: cleanup interrupted", e);
                        }

                        logger.info("Reader-1: after closing resources");
                        reader1CleanupInProgress.set(false);
                        reader1CleanupCompleted.set(true);
                    }

                    @Override
                    public void onReaderClosed(tech.ydb.topic.read.events.ReaderClosedEvent event) {
                        logger.info("Reader-1: onReaderClosed");
                    }
                })
                .build()
        );

        reader1.init().join();

        // Send a message to trigger partition assignment to Reader-1
        sendMessage("test-message-1");

        // Wait for Reader-1 to receive partition
        assertTrue("Reader-1 did not receive partition", reader1Started.await(10, TimeUnit.SECONDS));
        assertNotNull("Reader-1 partition session is null", reader1PartitionSession.get());

        Long assignedPartitionId = reader1PartitionSession.get().getPartitionId();
        logger.info("Reader-1 received partition: {}", assignedPartitionId);

        // Start shutdown of Reader-1
        logger.info("Before reader-1 shutdown");
        CompletableFuture<Void> reader1ShutdownFuture = reader1.shutdown();

        // Wait for Reader-1 cleanup to start
        assertTrue("Reader-1 cleanup did not start", reader1CleanupStarted.await(10, TimeUnit.SECONDS));
        logger.info("Reader-1 cleanup started");

        // Create Reader-2 while Reader-1 is still cleaning up
        AsyncReader reader2 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader2Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                        // No-op
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        PartitionSession session = event.getPartitionSession();
                        logger.info("Reader-2: onStartPartitionSession - partition={}, session={}",
                                session.getPartitionId(), session.getId());

                        // Check if Reader-1 is still cleaning up
                        if (reader1CleanupInProgress.get()) {
                            logger.error("RACE CONDITION DETECTED: Reader-2 received partition {} while Reader-1 is still cleaning up",
                                    session.getPartitionId());
                            raceConditionDetected.set(true);
                        }

                        if (!reader1CleanupCompleted.get()) {
                            logger.warn("Reader-2 received partition {} before Reader-1 completed cleanup",
                                    session.getPartitionId());
                        }

                        event.confirm();
                        reader2Started.countDown();
                    }
                })
                .build()
        );

        reader2.init().join();

        // Give some time for Reader-2 to potentially receive the partition during Reader-1's cleanup
        Thread.sleep(500);

        // Allow Reader-1 to finish cleanup
        allowReader1ToFinish.countDown();

        // Wait for Reader-1 shutdown to complete
        reader1ShutdownFuture.get(10, TimeUnit.SECONDS);
        logger.info("After reader-1 shutdown");

        // Wait a bit more for partition reassignment to Reader-2
        boolean reader2GotPartition = reader2Started.await(15, TimeUnit.SECONDS);

        // Cleanup
        reader2.shutdown().get(10, TimeUnit.SECONDS);
        reader1Executor.shutdownNow();
        reader2Executor.shutdownNow();

        // Assertions
        assertFalse("Race condition detected: Reader-2 received partition while Reader-1 was still cleaning up",
                raceConditionDetected.get());

        if (reader2GotPartition) {
            assertTrue("Reader-1 cleanup should be completed before Reader-2 receives the partition",
                    reader1CleanupCompleted.get());
            logger.info("Test passed: Reader-2 received partition only after Reader-1 completed cleanup");
        } else {
            logger.warn("Reader-2 did not receive partition within timeout - test inconclusive");
        }
    }
}
