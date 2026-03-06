package tech.ydb.topic.impl;

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
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify event ordering guarantees and session close race conditions in Topic API.
 * <p>
 * This test checks for two related problems:
 * 1. Event Ordering: StartPartitionSessionEvent and StopPartitionSessionEvent must be delivered in order,
 *    ensuring stop events are not processed before their corresponding start events.
 * <p>
 * 2. Session Close Race Condition: Server reader sessions should not be closed before onPartitionSessionClosed
 *    and onReaderClosed callbacks complete execution. This prevents partitions from being reassigned to other
 *    readers before the original reader has finished cleaning up its resources.
 *
 * @author Evgeny Kuvardin
 */
public class TopicReaderEventOrderingTest {
    private static final Logger logger = LoggerFactory.getLogger(TopicReaderEventOrderingTest.class);

    @ClassRule
    public static final GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private static final String TEST_CONSUMER = "test-consumer";

    // Be careful to increment partition count!
    // All single threads are stuck for 5 seconds
    // Also should increase value to wait reader2GotPartition
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

    private void sendMessage(String data) {
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
     * Scenario:
     * Verify that StartPartitionSessionEvent is always delivered before
     * PartitionSessionClosedEvent for the same partition session.
     * <p>
     * The test ensures that event ordering is preserved and that the client
     * never receives a "close session" event before the corresponding "start session".
     * <p>
     * Test steps:
     * <p>
     * 1. Create structures to track events per partition.
     * 2. Create AsyncReader with event handlers that log start/close events.
     * 3. Initialize reader.
     * 4. Send a message to trigger partition assignment.
     * 5. Wait until start events are received
     * 6. Shutdown reader to trigger close events
     * 7. Wait until close events are received.
     * 8. Verify that no ordering violation occurred.
     * 9. Verify Start event occurs before Close event
     */
    @Test
    public void testEventOrderingGuarantees() throws Exception {
        logger.info("Starting testEventOrderingGuarantees");

        // Step 1: Create structures to track events per partition.
        StructureTest1 structureTest = getStructureForOrderGarantees();

        // Step 2: Create AsyncReader with event handlers that log start/close events.
        AsyncReader reader = getAsyncReaderForOrderGaran(structureTest.readerSettings, structureTest.executor, structureTest.eventLog, structureTest.activeSessions, structureTest.orderingViolation, structureTest.startReceived, structureTest.closeReceived);

        // Step 3: Initialize reader
        reader.init().join();

        // Step 4: Send message to trigger partition assignment
        sendMessage("test-message");

        // Step 5: Wait until start events are received
        assertTrue("Start event not received", structureTest.startReceived.await(10, TimeUnit.SECONDS));

        // Step 6: Shutdown reader to trigger close events
        logger.info("Shutting down reader");
        reader.shutdown().get(10, TimeUnit.SECONDS);

        // Step 7: Wait for close events
        assertTrue("Close event not received", structureTest.closeReceived.await(10, TimeUnit.SECONDS));

        structureTest.executor.shutdownNow();

        logger.info("Event log: {}", structureTest.eventLog);
        // Step 8: Verify no ordering violations occurred
        assertFalse("Event ordering violation detected", structureTest.orderingViolation.get());

        // Step 9: Verify Start event occurs before Close event
        for (long partitionId = 0; partitionId < partitionCount; partitionId++) {
            // Verify event sequence
            int startIndex = -1;
            int stopIndex = -1;
            for (int i = 0; i < structureTest.eventLog.get(partitionId).size(); i++) {
                if (startIndex == -1 && structureTest.eventLog.get(partitionId).get(i).startsWith("onStartPartitionSession")) {
                    startIndex = i;
                }
                if (stopIndex == -1 && structureTest.eventLog.get(partitionId).get(i).startsWith("onPartitionSessionClosed")) {
                    stopIndex = i;
                }
            }

            assertTrue("Start event should be present", startIndex >= 0);
            assertTrue("Close event should be present", stopIndex >= 0);
            assertTrue("Start event must come before Stop event", startIndex < stopIndex);
        }
    }

    /**
     * Scenario:
     * Verify that partition reassignment does not happen while the previous reader
     * is still executing cleanup logic inside onPartitionSessionClosed.
     * <p>
     * This test simulates a slow cleanup in Reader-1 and starts Reader-2 while
     * Reader-1 is still closing the session.
     * <p>
     * Steps:
     * <p>
     * 1. Start Reader-1 and wait until it receives partitions.
     * 2. Send a message to trigger partition assignment.
     * 3. Shutdown Reader-1 to trigger session close.
     * 4. Block Reader-1 cleanup to simulate slow resource release.
     * 5. Start Reader-2 while Reader-1 cleanup is still in progress.
     * 6. Allow Reader-1 cleanup to finish.
     * 7. Wait for partition reassignment to Reader-2.
     * 8. Verify that reassignment only happened after Reader-1 cleanup finished.
     */
    @Test
    public void testSessionCloseRaceCondition() throws Exception {
        logger.info("Starting testSessionCloseRaceCondition");

        StructureTest2 structureTest = getStructureForRaceCondition();

        // Create Reader-1
        AsyncReader reader1 = getAsyncReader1ForRaceCondition(structureTest.readerSettings, structureTest.reader1Executor, structureTest.reader1PartitionSession, structureTest.reader1Started, structureTest.reader1CleanupInProgress, structureTest.reader1CleanupStarted, structureTest.allowReader1ToFinish, structureTest.reader1CleanupCompleted);

        // Step 1. Start Reader-1 and wait until it receives partitions.
        reader1.init().join();

        // Step 2. Send a message to trigger partition assignment.
        sendMessage("test-message-1");

        // Wait for Reader-1 to receive partition
        assertTrue("Reader-1 did not receive partition", structureTest.reader1Started.await(10, TimeUnit.SECONDS));
        for (Map.Entry<Long, AtomicReference<PartitionSession>> v : structureTest.reader1PartitionSession.entrySet()) {
            assertNotNull("Reader-1 partition session is null", v.getValue().get());
            logger.info("Reader-1 received partition: {}", v.getKey());
        }

        // Step 3.Reader-1 to trigger session close.
        logger.info("Before reader-1 shutdown");
        CompletableFuture<Void> reader1ShutdownFuture = reader1.shutdown();

        // Wait for Reader-1 cleanup to start
        assertTrue("Reader-1 cleanup did not start", structureTest.reader1CleanupStarted.await(15, TimeUnit.SECONDS));
        logger.info("Reader-1 cleanup started");

        // Create Reader-2 while Reader-1 is still cleaning up
        AsyncReader reader2 = getAsyncReader2ForRaceCondition(structureTest.readerSettings, structureTest.reader2Executor, structureTest.reader1CleanupInProgress, structureTest.raceConditionDetected, structureTest.reader1CleanupCompleted, structureTest.reader2Started);

        // Step 5. Start Reader-2 while Reader-1 cleanup is still in progress.
        reader2.init().join();

        // Give some time for Reader-2 to potentially receive the partition during Reader-1's cleanup
        Thread.sleep(500);

        // Step 6. Allow Reader-1 cleanup to finish.
        structureTest.allowReader1ToFinish.countDown();

        // Step 7. Wait for partition reassignment to Reader-2.
        reader1ShutdownFuture.get(10, TimeUnit.SECONDS);
        logger.info("After reader-1 shutdown");

        // Wait a bit more for partition reassignment to Reader-2
        boolean reader2GotPartition = structureTest.reader2Started.await(15, TimeUnit.SECONDS);

        // Cleanup
        reader2.shutdown().get(10, TimeUnit.SECONDS);
        structureTest.reader1Executor.shutdownNow();
        structureTest.reader2Executor.shutdownNow();

        // Step 8. Verify that reassignment only happened after Reader-1 cleanup finished.
        assertFalse("Race condition detected: Reader-2 received partition while Reader-1 was still cleaning up",
                structureTest.raceConditionDetected.get());

        if (reader2GotPartition) {
            for (Map.Entry<Long, AtomicBoolean> v : structureTest.reader1CleanupCompleted.entrySet()) {
                assertTrue("Reader-1 cleanup should be completed before Reader-2 receives the partition : " + v.getKey(),
                        v.getValue().get());
                logger.info("Test passed: Reader-2 received partition only after Reader-1 completed cleanup, partition {}", v.getKey());
            }
        } else {
            logger.warn("Reader-2 did not receive partition within timeout - test inconclusive");
        }
    }

    private @NotNull TopicReaderEventOrderingTest.StructureTest2 getStructureForRaceCondition() {
        // Map for tracking partition and attached sessions
        ConcurrentHashMap<Long, AtomicReference<PartitionSession>> reader1PartitionSession = new ConcurrentHashMap<>();

        // Map for tracking partition and is reader1 in cleanup. false -> reader 1 read partition is in progress
        // true -> reader 1 read partition is detached from partition
        ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupInProgress = new ConcurrentHashMap<>();

        // Map for tracking partition and is reader1 in cleanup. false -> reader1 not started read partition or cleanUp wasn't completed
        // true -> reader1 completed cleanup
        ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupCompleted = new ConcurrentHashMap<>();
        for (long i = 0; i < partitionCount; i++) {
            reader1CleanupCompleted.put(i, new AtomicBoolean(false));
        }

        // Simple value to detect race condition
        AtomicBoolean raceConditionDetected = new AtomicBoolean(false);
        CountDownLatch reader1Started = new CountDownLatch(partitionCount);
        CountDownLatch reader1CleanupStarted = new CountDownLatch(partitionCount);
        CountDownLatch reader2Started = new CountDownLatch(partitionCount);

        // Some latch in which reader1 stuck for 1 minute. Be careful to increment partition count!
        // All single threads are stuck for 5 seconds
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
        StructureTest2 structureTest2 = new StructureTest2(reader1PartitionSession, reader1CleanupInProgress, reader1CleanupCompleted, raceConditionDetected, reader1Started, reader1CleanupStarted, reader2Started, allowReader1ToFinish, reader1Executor, reader2Executor, readerSettings);
        return structureTest2;
    }

    private static class StructureTest2 {
        public final ConcurrentHashMap<Long, AtomicReference<PartitionSession>> reader1PartitionSession;
        public final ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupInProgress;
        public final ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupCompleted;
        public final AtomicBoolean raceConditionDetected;
        public final CountDownLatch reader1Started;
        public final CountDownLatch reader1CleanupStarted;
        public final CountDownLatch reader2Started;
        public final CountDownLatch allowReader1ToFinish;
        public final ExecutorService reader1Executor;
        public final ExecutorService reader2Executor;
        public final ReaderSettings readerSettings;

        public StructureTest2(ConcurrentHashMap<Long, AtomicReference<PartitionSession>> reader1PartitionSession, ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupInProgress, ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupCompleted, AtomicBoolean raceConditionDetected, CountDownLatch reader1Started, CountDownLatch reader1CleanupStarted, CountDownLatch reader2Started, CountDownLatch allowReader1ToFinish, ExecutorService reader1Executor, ExecutorService reader2Executor, ReaderSettings readerSettings) {
            this.reader1PartitionSession = reader1PartitionSession;
            this.reader1CleanupInProgress = reader1CleanupInProgress;
            this.reader1CleanupCompleted = reader1CleanupCompleted;
            this.raceConditionDetected = raceConditionDetected;
            this.reader1Started = reader1Started;
            this.reader1CleanupStarted = reader1CleanupStarted;
            this.reader2Started = reader2Started;
            this.allowReader1ToFinish = allowReader1ToFinish;
            this.reader1Executor = reader1Executor;
            this.reader2Executor = reader2Executor;
            this.readerSettings = readerSettings;
        }
    }

    private @NotNull TopicReaderEventOrderingTest.StructureTest1 getStructureForOrderGarantees() {
        Map<Long, List<String>> eventLog = new ConcurrentHashMap<>();
        for (long i = 0; i < partitionCount; i++) {
            eventLog.put(i, Collections.synchronizedList(new ArrayList<>()));
        }

        Map<Long, Long> activeSessions = new ConcurrentHashMap<>();

        CountDownLatch startReceived = new CountDownLatch(partitionCount);
        CountDownLatch closeReceived = new CountDownLatch(partitionCount);
        AtomicBoolean orderingViolation = new AtomicBoolean(false);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "test-event-executor"));

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(testTopic)
                        .build())
                .setConsumerName(TEST_CONSUMER)
                .build();
        return new StructureTest1(eventLog, activeSessions, startReceived, closeReceived, orderingViolation, executor, readerSettings);
    }

    private static class StructureTest1 {
        public final Map<Long, List<String>> eventLog;
        public final Map<Long, Long> activeSessions;
        public final CountDownLatch startReceived;
        public final CountDownLatch closeReceived;
        public final AtomicBoolean orderingViolation;
        public final ExecutorService executor;
        public final ReaderSettings readerSettings;

        public StructureTest1(Map<Long, List<String>> eventLog, Map<Long, Long> activeSessions, CountDownLatch startReceived, CountDownLatch closeReceived, AtomicBoolean orderingViolation, ExecutorService executor, ReaderSettings readerSettings) {
            this.eventLog = eventLog;
            this.activeSessions = activeSessions;
            this.startReceived = startReceived;
            this.closeReceived = closeReceived;
            this.orderingViolation = orderingViolation;
            this.executor = executor;
            this.readerSettings = readerSettings;
        }
    }

    private AsyncReader getAsyncReaderForOrderGaran(ReaderSettings readerSettings, ExecutorService executor, Map<Long, List<String>> eventLog, Map<Long, Long> activeSessions, AtomicBoolean orderingViolation, CountDownLatch startReceived, CountDownLatch closeReceived) {
        return client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
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

                        // Record start event
                        eventLog.get(partitionId).add("onStartPartitionSession[partitionId = " + partitionId + ",session=" + sessionId + "]");
                        logger.info("onStartPartitionSession: session={}", sessionId);

                        if (activeSessions.get(partitionId) != null) {
                            logger.error("START event received while session {} is still active", activeSessions.get(partitionId));
                            orderingViolation.set(true);
                        }

                        activeSessions.put(partitionId, sessionId);
                        event.confirm();
                        startReceived.countDown();
                    }

                    @Override
                    public void onPartitionSessionClosed(tech.ydb.topic.read.events.PartitionSessionClosedEvent event) {
                        long sessionId = event.getPartitionSession().getId();
                        long partitionId = event.getPartitionSession().getPartitionId();

                        // Record close event
                        eventLog.get(partitionId).add("onPartitionSessionClosed[partitionId =" + partitionId + ",session=" + sessionId + "]");

                        logger.info("onPartitionSessionClosed: session={}", sessionId);
                        activeSessions.remove(partitionId);
                        closeReceived.countDown();
                    }
                })
                .build()
        );
    }

    private AsyncReader getAsyncReader2ForRaceCondition(ReaderSettings readerSettings, ExecutorService reader2Executor, ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupInProgress, AtomicBoolean raceConditionDetected, ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupCompleted, CountDownLatch reader2Started) {
        return client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader2Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                        // No-op
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        PartitionSession session = event.getPartitionSession();
                        logger.info("Reader-2: onStartPartitionSession - partition={}, session={}",
                                session.getPartitionId(), session.getId());

                        // Check if Reader-1 is still cleaning up
                        if (reader1CleanupInProgress.get(partitionId).get()) {
                            logger.error("RACE CONDITION DETECTED: Reader-2 received partition {} while Reader-1 is still cleaning up",
                                    session.getPartitionId());
                            raceConditionDetected.set(true);
                        }

                        if (!reader1CleanupCompleted.get(partitionId).get()) {
                            logger.warn("Reader-2 received partition {} before Reader-1 completed cleanup",
                                    session.getPartitionId());
                        }

                        event.confirm();
                        reader2Started.countDown();
                    }
                })
                .build()
        );
    }

    private AsyncReader getAsyncReader1ForRaceCondition(ReaderSettings readerSettings, ExecutorService reader1Executor, ConcurrentHashMap<Long, AtomicReference<PartitionSession>> reader1PartitionSession, CountDownLatch reader1Started, ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupInProgress, CountDownLatch reader1CleanupStarted, CountDownLatch allowReader1ToFinish, ConcurrentHashMap<Long, AtomicBoolean> reader1CleanupCompleted) {
        return client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader1Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        PartitionSession session = event.getPartitionSession();
                        logger.info("Reader-1: onStartPartitionSession - partition={}, session={}",
                                session.getPartitionId(), session.getId());
                        reader1PartitionSession.compute(partitionId, (k, ref) -> {
                            if (ref == null) {
                                ref = new AtomicReference<>();
                            }
                            ref.set(session);
                            return ref;
                        });

                        event.confirm();
                        reader1Started.countDown();
                    }

                    @Override
                    public void onPartitionSessionClosed(tech.ydb.topic.read.events.PartitionSessionClosedEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        PartitionSession session = event.getPartitionSession();
                        logger.info("Reader-1: onPartitionSessionClosed - partition={}, session={}",
                                session.getPartitionId(), session.getId());
                        logger.info("Reader-1: before closing resources");

                        reader1CleanupInProgress.compute(partitionId, (k, ref) -> {
                            if (ref == null) {
                                ref = new AtomicBoolean();
                            }
                            ref.set(true);
                            return ref;
                        });

                        reader1CleanupStarted.countDown();

                        // Step 4. Block Reader-1 cleanup to simulate slow resource release. (e.g., closing database connections, flushing buffers)
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

                        reader1CleanupInProgress.get(partitionId).set(false);
                        reader1CleanupCompleted.get(partitionId).set(true);

                    }

                    @Override
                    public void onReaderClosed(tech.ydb.topic.read.events.ReaderClosedEvent event) {
                        logger.info("Reader-1: onReaderClosed");
                    }
                })
                .build()
        );
    }
}
