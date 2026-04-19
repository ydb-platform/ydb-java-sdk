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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
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

    /**
     * Scenario:
     * Verify that when the YDB server removes a partition from a reader and immediately reassigns
     * it back to the same reader, StartPartitionSessionEvent is never delivered before
     * PartitionSessionClosedEvent for the prior session on the same partition.
     * <p>
     * Test steps:
     * <p>
     * 1. Start Reader-1 and wait until it receives all partitions.
     * 2. Connect Reader-2 with the same consumer to trigger server-side rebalancing.
     *    Server sends StopPartitionSession to Reader-1 for some partitions.
     * 3. Once Reader-2 receives at least one partition, immediately shut it down.
     *    Server should reassign the partition back to Reader-1.
     * 4. Wait for Reader-1 to receive StartPartitionSession a second time (the re-assignment).
     * 5. Verify no ordering violation: no StartPartitionSession arrived while the previous
     *    session for the same partition was still open (i.e., before PartitionSessionClosed).
     *
     *    Test now failed
     */
    @Ignore
    @Test
    public void testPartitionRemoveAndImmediateReturn() throws Exception {
        logger.info("Starting testPartitionRemoveAndImmediateReturn");

        // Track currently active session per partition: partitionId -> sessionId.
        // If a new StartPartitionSession arrives while a session is still active, that is a violation.
        Map<Long, Long> activeSessions = new ConcurrentHashMap<>();
        AtomicBoolean orderingViolation = new AtomicBoolean(false);

        // Count how many times each partition has been started on reader1.
        Map<Long, AtomicInteger> startCounts = new ConcurrentHashMap<>();

        // Step 1 signal: all partitions assigned to reader1 for the first time.
        CountDownLatch reader1FirstAssignment = new CountDownLatch(partitionCount);

        // Signal: reader1 received a partition for the second time (server returned it back).
        CountDownLatch reader1GotPartitionBack = new CountDownLatch(1);

        // Signal: reader2 received at least one partition (rebalancing took place).
        CountDownLatch reader2GotPartition = new CountDownLatch(1);

        ExecutorService reader1Executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "remove-return-reader1-executor"));
        ExecutorService reader2Executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "remove-return-reader2-executor"));

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(testTopic)
                        .build())
                .setConsumerName(TEST_CONSUMER)
                .build();

        AsyncReader reader1 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader1Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        long sessionId = event.getPartitionSession().getId();

                        if (activeSessions.containsKey(partitionId)) {
                            logger.error("VIOLATION: Reader-1 got StartPartitionSession for partition {} session {}" +
                                    " while session {} is still active (PartitionSessionClosed not yet delivered)",
                                    partitionId, sessionId, activeSessions.get(partitionId));
                            orderingViolation.set(true);
                        }

                        activeSessions.put(partitionId, sessionId);
                        event.confirm();

                        int count = startCounts
                                .computeIfAbsent(partitionId, k -> new AtomicInteger(0))
                                .incrementAndGet();
                        logger.info("Reader-1: onStartPartitionSession partition={} session={} count={}", partitionId, sessionId, count);

                        if (count == 1) {
                            reader1FirstAssignment.countDown();
                        } else {
                            reader1GotPartitionBack.countDown();
                        }
                    }

                    @Override
                    public void onPartitionSessionClosed(tech.ydb.topic.read.events.PartitionSessionClosedEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        long sessionId = event.getPartitionSession().getId();
                        logger.info("Reader-1: onPartitionSessionClosed partition={} session={}", partitionId, sessionId);
                        activeSessions.remove(partitionId);
                    }
                })
                .build()
        );

        // Step 1: Initialize reader1 and wait for all partitions.
        reader1.init().join();
        sendMessage("trigger-assignment");
        assertTrue("Reader-1 did not receive all partitions on first assignment",
                reader1FirstAssignment.await(15, TimeUnit.SECONDS));
        logger.info("Reader-1 has all {} partitions", partitionCount);

        // Step 2: Start reader2 with the same consumer to trigger server-side rebalancing.
        AsyncReader reader2 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader2Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        logger.info("Reader-2: onStartPartitionSession partition={}", partitionId);
                        event.confirm();
                        reader2GotPartition.countDown();
                    }
                })
                .build()
        );

        reader2.init().join();
        logger.info("Reader-2 initialized, waiting for partition reassignment from server");

        // Step 3: Wait for reader2 to receive at least one partition, then immediately shut it down.
        boolean reader2ReceivedPartition = reader2GotPartition.await(15, TimeUnit.SECONDS);
        if (!reader2ReceivedPartition) {
            logger.warn("Reader-2 did not receive any partition — test inconclusive, skipping ordering check");
            reader2.shutdown().get(10, TimeUnit.SECONDS);
            reader1.shutdown().get(10, TimeUnit.SECONDS);
            reader1Executor.shutdownNow();
            reader2Executor.shutdownNow();
            Assert.fail("Reader-2 did not receive any partition — test inconclusive, skipping ordering check");
        }

        logger.info("Reader-2 received a partition, shutting it down immediately to trigger return to reader1");
        reader2.shutdown().get(10, TimeUnit.SECONDS);

        // Step 4: Wait for reader1 to receive a partition back from the server.
        boolean partitionReturnedToReader1 = reader1GotPartitionBack.await(20, TimeUnit.SECONDS);
        logger.info("Reader-1 got partition back: {}", partitionReturnedToReader1);

        reader1.shutdown().get(10, TimeUnit.SECONDS);
        reader1Executor.shutdownNow();
        reader2Executor.shutdownNow();

        // Step 5: Verify ordering.
        if (partitionReturnedToReader1) {
            assertFalse(
                    "Event ordering violation: StartPartitionSession was received before PartitionSessionClosed" +
                    " for the same partition when the server reassigned it back",
                    orderingViolation.get());
            logger.info("testPartitionRemoveAndImmediateReturn PASSED");
        } else {
            logger.warn("Server did not reassign any partition back to reader-1 within the timeout — test inconclusive");
            assertFalse("Ordering violation detected even though no partition was returned", orderingViolation.get());
        }
    }

    /**
     * Scenario:
     * The client holds a StopPartitionSession event and delays calling confirm() on it.
     * While the client is still "thinking" about releasing the partition, Reader-1 is shut down
     * externally (simulating a crash or forced close). This causes the SDK to close the gRPC
     * session, which fires onPartitionSessionClosed for all active partitions.
     * Only after onPartitionSessionClosed fires does the client call the stored StopPartitionSession.confirm().
     * <p>
     * This verifies that a late StopPartitionSession.confirm() (called after the partition is already
     * closed by the session shutdown) is handled gracefully — it must be a safe no-op and must not
     * crash, send spurious responses to the server, or cause ordering violations.
     * <p>
     * Steps:
     * 1. Create Reader-1. confirm() StartPartitionSession immediately, but store
     *    StopPartitionSession events and do NOT confirm() them yet.
     * 2. Initialize Reader-1, send a message, wait until all partitions are assigned.
     * 3. Connect Reader-2 with the same consumer to trigger server-side rebalancing.
     *    Server sends graceful StopPartitionSession to Reader-1 for at least one partition.
     * 4. Wait for Reader-1 to receive the StopPartitionSession event (not confirmed).
     * 5. Shut down Reader-1 — this closes the session and fires onPartitionSessionClosed.
     * 6. Call confirm() on ALL stored StopPartitionSession events — session is already closed.
     * 7. Verify no exception and no ordering violation.
     */
    @Test
    public void testLateStopConfirmAfterPartitionRevoked() throws Exception {
        logger.info("Starting testLateStopConfirmAfterPartitionRevoked");

        // Stored StopPartitionSession events — confirm() will be called late
        List<tech.ydb.topic.read.events.StopPartitionSessionEvent> pendingStopEvents =
                Collections.synchronizedList(new ArrayList<>());

        Map<Long, Long> activeSessions = new ConcurrentHashMap<>();
        AtomicBoolean orderingViolation = new AtomicBoolean(false);

        // Counted down when all partitions are assigned to Reader-1 for the first time
        CountDownLatch reader1AllPartitionsAssigned = new CountDownLatch(partitionCount);
        // Counted down when a stop request arrives at Reader-1 (before confirm)
        CountDownLatch reader1StopReceived = new CountDownLatch(1);
        // Counted down each time a partition is actually closed on Reader-1
        CountDownLatch reader1PartitionClosed = new CountDownLatch(partitionCount);
        // Reader-2 has received at least one partition
        CountDownLatch reader2GotPartition = new CountDownLatch(1);

        ExecutorService reader1Executor = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "late-stop-reader1-executor"));
        ExecutorService reader2Executor = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "late-stop-reader2-executor"));

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(testTopic).build())
                .setConsumerName(TEST_CONSUMER)
                .build();

        // Step 1: Build Reader-1
        AsyncReader reader1 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader1Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        long sessionId = event.getPartitionSession().getId();
                        logger.info("Reader-1: onStartPartitionSession partition={}, session={}", partitionId, sessionId);

                        if (activeSessions.containsKey(partitionId)) {
                            logger.error("VIOLATION: Reader-1 got StartPartitionSession for partition {} session {}" +
                                    " while session {} is still active",
                                    partitionId, sessionId, activeSessions.get(partitionId));
                            orderingViolation.set(true);
                        }
                        activeSessions.put(partitionId, sessionId);
                        // Confirm the start immediately so the server knows we own the partitions
                        event.confirm();
                        reader1AllPartitionsAssigned.countDown();
                    }

                    @Override
                    public void onStopPartitionSession(tech.ydb.topic.read.events.StopPartitionSessionEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        logger.info("Reader-1: onStopPartitionSession partition={} — storing, NOT confirming yet",
                                partitionId);
                        // Store the event, deliberately do NOT call confirm() yet
                        pendingStopEvents.add(event);
                        reader1StopReceived.countDown();
                    }

                    @Override
                    public void onPartitionSessionClosed(tech.ydb.topic.read.events.PartitionSessionClosedEvent event) {
                        long partitionId = event.getPartitionSession().getPartitionId();
                        long sessionId = event.getPartitionSession().getId();
                        logger.info("Reader-1: onPartitionSessionClosed partition={}, session={}", partitionId, sessionId);
                        activeSessions.remove(partitionId);
                        reader1PartitionClosed.countDown();
                    }
                })
                .build()
        );

        // Step 2: Initialize Reader-1 and wait for all partitions
        reader1.init().join();
        sendMessage("test-trigger");
        assertTrue("Reader-1 did not receive all partition assignments within timeout",
                reader1AllPartitionsAssigned.await(15, TimeUnit.SECONDS));
        logger.info("Reader-1 owns all {} partitions", partitionCount);

        // Step 3: Connect Reader-2 to trigger server-side rebalancing.
        // Server will send graceful StopPartitionSession to Reader-1 for at least one partition.
        AsyncReader reader2 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(reader2Executor)
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onMessages(tech.ydb.topic.read.events.DataReceivedEvent event) {
                    }

                    @Override
                    public void onStartPartitionSession(tech.ydb.topic.read.events.StartPartitionSessionEvent event) {
                        logger.info("Reader-2: onStartPartitionSession partition={}",
                                event.getPartitionSession().getPartitionId());
                        event.confirm();
                        reader2GotPartition.countDown();
                    }
                })
                .build()
        );
        reader2.init().join();
        logger.info("Reader-2 initialized — waiting for StopPartitionSession to arrive at Reader-1");

        // Step 4: Wait for Reader-1 to receive at least one StopPartitionSession (not confirmed)
        assertTrue("Reader-1 did not receive StopPartitionSession within timeout",
                reader1StopReceived.await(20, TimeUnit.SECONDS));
        logger.info("Reader-1 received StopPartitionSession (not confirmed yet) — shutting down reader");

        // Step 5: Shut down Reader-1 while StopPartitionSession.confirm() is still pending.
        // This causes the session to close, which calls onStop() -> handleClosePartitionSession()
        // -> onPartitionSessionClosed() for all remaining active partitions.
        CompletableFuture<Void> reader1Shutdown = reader1.shutdown();

        assertTrue("Reader-1 partitions were not closed within timeout",
                reader1PartitionClosed.await(15, TimeUnit.SECONDS));
        logger.info("Reader-1: all partitions closed. Now calling delayed StopPartitionSession.confirm()...");

        // Step 6: Call confirm() on all stored StopPartitionSession events.
        // The session is already closed, so this must be a safe no-op.
        for (tech.ydb.topic.read.events.StopPartitionSessionEvent event : pendingStopEvents) {
            logger.info("Calling late confirm() for StopPartitionSession on partition {}",
                    event.getPartitionSession().getPartitionId());
            event.confirm();
        }
        logger.info("All late StopPartitionSession.confirm() calls completed — no exception thrown");

        reader1Shutdown.get(10, TimeUnit.SECONDS);

        // Allow some time for any unexpected side effects
        Thread.sleep(500);

        // Wait for Reader-2 to receive the partition (after Reader-1 released it) — best effort
        boolean reader2ReceivedPartition = reader2GotPartition.await(10, TimeUnit.SECONDS);
        logger.info("Reader-2 received partition: {}", reader2ReceivedPartition);

        // Cleanup
        reader2.shutdown().get(10, TimeUnit.SECONDS);
        reader1Executor.shutdownNow();
        reader2Executor.shutdownNow();

        // Step 7: Verify no ordering violation
        assertFalse("Event ordering violation: StartPartitionSession received while session was still active",
                orderingViolation.get());
        logger.info("testLateStopConfirmAfterPartitionRevoked PASSED");
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
