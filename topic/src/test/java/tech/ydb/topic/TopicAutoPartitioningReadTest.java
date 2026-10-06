package tech.ydb.topic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.AutoPartitioningWriteStrategySettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

/**
 * Reading a topic that has already been split by auto-partitioning.
 * With auto_partitioning_support the server assigns only the root partition first
 * and sends StartPartitionSession for child partitions after the reader starts reading the parent.
 */
public class TopicAutoPartitioningReadTest {
    private static final Logger logger = LoggerFactory.getLogger(TopicAutoPartitioningReadTest.class);

    private static final String TOPIC = "topic_auto_partitioning_read";
    private static final String CONSUMER = "consumer";
    private static final int SOURCE_COUNT = 4;
    private static final int MIN_MESSAGE_SIZE = 1;
    private static final int MAX_MESSAGE_SIZE = 4 * 1024 * 1024;
    private static final long WRITE_SPEED_BYTES_PER_SECOND = 4L * 1024 * 1024;
    private static final long WRITE_BURST_BYTES = 8L * 1024 * 1024;

    @ClassRule
    public static final GrpcTransportRule YDB = new GrpcTransportRule();

    @Test(timeout = 300000)
    public void childPartitionsStartAfterReadingParent() throws Exception {
        try (TopicClient client = TopicClient.newClient(YDB).build()) {
            client.dropTopic(TOPIC).join();
            try {
                createTopic(client);

                writeUntilSplit(client);
                TopicDescription description = client.describeTopic(TOPIC).join().getValue();
                logger.info("Topic after split: {}", formatPartitions(description.getPartitions()));

                PartitionInfo root = rootPartition(description.getPartitions());
                Assert.assertEquals("original partition must be the one that split", 0L, root.getPartitionId());
                Assert.assertFalse("split parent must become inactive", root.isActive());
                Set<Long> childIds = new HashSet<>(root.getChildPartitionIds());
                Assert.assertFalse("topic must split at least once", childIds.isEmpty());

                readParentThenChildren(client, root.getPartitionId(), childIds);
            } finally {
                client.dropTopic(TOPIC).join();
            }
        }
    }

    private static void createTopic(TopicClient client) {
        client.createTopic(TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(CONSUMER).build())
                .setSupportedCodecs(SupportedCodecs.newBuilder()
                        .addCodec(Codec.GZIP)
                        .build())
                .setPartitionWriteSpeedBytesPerSecond(WRITE_SPEED_BYTES_PER_SECOND)
                .setPartitionWriteBurstBytes(WRITE_BURST_BYTES)
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setMinActivePartitions(1)
                        .setMaxActivePartitions(10)
                        .setAutoPartitioningStrategy(AutoPartitioningStrategy.SCALE_UP)
                        .setWriteStrategySettings(AutoPartitioningWriteStrategySettings.newBuilder()
                                .setStabilizationWindow(Duration.ofSeconds(1))
                                .setUpUtilizationPercent(1)
                                .setDownUtilizationPercent(1)
                                .build())
                        .build())
                .build()
        ).join().expectSuccess("can't create topic");
    }

    private static void writeUntilSplit(TopicClient client) throws InterruptedException {
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<Throwable> writerError = new AtomicReference<>();
        List<Thread> writers = new ArrayList<>();
        for (int idx = 0; idx < SOURCE_COUNT; idx++) {
            String sourceId = "src-" + idx;
            Thread writer = new Thread(() -> writeSource(client, sourceId, stop, writerError), sourceId);
            writer.start();
            writers.add(writer);
        }

        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
            while (System.nanoTime() < deadline) {
                if (writerError.get() != null) {
                    throw new AssertionError("writer failed", writerError.get());
                }
                TopicDescription description = client.describeTopic(TOPIC).join().getValue();
                if (hasSplit(description.getPartitions())) {
                    logger.info("Split observed: {}", formatPartitions(description.getPartitions()));
                    return;
                }
                Thread.sleep(500);
            }
            TopicDescription description = client.describeTopic(TOPIC).join().getValue();
            Assert.fail("topic did not split, partitions: " + formatPartitions(description.getPartitions()));
        } finally {
            stop.set(true);
            for (Thread writer : writers) {
                writer.join(TimeUnit.SECONDS.toMillis(30));
            }
        }
    }

    private static void writeSource(TopicClient client, String sourceId, AtomicBoolean stop,
            AtomicReference<Throwable> writerError) {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TOPIC)
                .setProducerId(sourceId)
                .setMessageGroupId(sourceId)
                .setCodec(Codec.GZIP)
                .setMaxSendBufferMemorySize(WRITE_BURST_BYTES)
                .setMaxSendBufferMessagesCount(2)
                .build();
        SyncWriter writer = null;
        try {
            writer = client.createSyncWriter(settings);
            writer.initAndWait();
            while (!stop.get() && writerError.get() == null) {
                byte[] payload = randomPayload();
                logger.info("Write sourceId={} uncompressedBytes={}", sourceId, payload.length);
                writer.send(Message.of(payload));
            }
        } catch (Throwable th) {
            if (!stop.get()) {
                writerError.compareAndSet(null, th);
            }
        } finally {
            if (writer != null) {
                try {
                    writer.shutdown(30, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    if (!stop.get()) {
                        writerError.compareAndSet(null, ex);
                    }
                }
            }
        }
    }

    private static void readParentThenChildren(TopicClient client, long rootId, Set<Long> childIds)
            throws InterruptedException {
        ConcurrentHashMap<Long, AtomicInteger> messages = new ConcurrentHashMap<>();
        ConcurrentHashMap<Long, StartPartitionSessionEvent> started = new ConcurrentHashMap<>();
        AtomicBoolean reading = new AtomicBoolean();
        AtomicBoolean childDataSeen = new AtomicBoolean();
        List<String> parentDataAfterChild = new ArrayList<>();
        AtomicReference<Throwable> readerError = new AtomicReference<>();
        CountDownLatch rootStarted = new CountDownLatch(1);

        ReaderSettings settings = ReaderSettings.newBuilder()
                .setConsumerName(CONSUMER)
                .setReaderName("auto-partitioning-read")
                .addTopic(TopicReadSettings.newBuilder().setPath(TOPIC).build())
                .setErrorsHandler((status, error) -> {
                    if (error != null) {
                        readerError.compareAndSet(null, error);
                    } else {
                        readerError.compareAndSet(null, new AssertionError("reader failed: " + status));
                    }
                })
                .build();

        AsyncReader reader = client.createAsyncReader(settings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        PartitionSession session = event.getPartitionSession();
                        long partitionId = session.getPartitionId();
                        logger.info("StartPartitionSession partitionId={} partitionSessionId={} committedOffset={}"
                                + " partitionOffsets=[{}, {})",
                                partitionId, session.getId(), event.getCommittedOffset(),
                                event.getPartitionOffsets().getStart(), event.getPartitionOffsets().getEnd());
                        started.put(partitionId, event);
                        if (reading.get()) {
                            logger.info("Confirm StartPartitionSession partitionId={}", partitionId);
                            event.confirm();
                        }
                        if (partitionId == rootId) {
                            rootStarted.countDown();
                        }
                    }

                    @Override
                    public void onStopPartitionSession(StopPartitionSessionEvent event) {
                        logger.info("StopPartitionSession partitionId={} partitionSessionId={} committedOffset={}",
                                event.getPartitionSession().getPartitionId(),
                                event.getPartitionSession().getId(),
                                event.getCommittedOffset());
                        event.confirm();
                    }

                    @Override
                    public void onPartitionSessionClosed(PartitionSessionClosedEvent event) {
                        logger.info("PartitionSessionClosed partitionId={} partitionSessionId={}",
                                event.getPartitionSession().getPartitionId(),
                                event.getPartitionSession().getId());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        // Do not commit. Child sessions are assigned when the parent is read to its end.
                        long partitionId = event.getPartitionSession().getPartitionId();
                        int count = event.getMessages().size();
                        messages.computeIfAbsent(partitionId, ignored -> new AtomicInteger()).addAndGet(count);
                        if (count == 0) {
                            return;
                        }
                        long firstOffset = event.getMessages().get(0).getOffset();
                        long lastOffset = event.getMessages().get(count - 1).getOffset();
                        logger.info("DataReceived partitionId={} messages={} offsets={}-{}",
                                partitionId, count, firstOffset, lastOffset);
                        synchronized (parentDataAfterChild) {
                            if (childIds.contains(partitionId)) {
                                childDataSeen.set(true);
                            } else if (partitionId == rootId && childDataSeen.get()) {
                                String problem = "partitionId=" + partitionId + " offsets="
                                        + firstOffset + "-" + lastOffset;
                                logger.error("Parent data after child data: {}", problem);
                                parentDataAfterChild.add(problem);
                            }
                        }
                    }
                })
                .build());

        reader.init().join();
        try {
            Assert.assertTrue("root partition session was not started", rootStarted.await(30, TimeUnit.SECONDS));
            Thread.sleep(2000);
            Set<Long> assignedBeforeRead = new HashSet<>(started.keySet());
            Set<Long> onlyRoot = new HashSet<>();
            onlyRoot.add(rootId);
            Assert.assertEquals("only the original partition is assigned before reading starts",
                    onlyRoot, assignedBeforeRead);

            reading.set(true);
            started.get(rootId).confirm();

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            while (System.nanoTime() < deadline && !started.keySet().containsAll(childIds)) {
                if (readerError.get() != null) {
                    throw new AssertionError("reader failed before child partitions were assigned", readerError.get());
                }
                Thread.sleep(200);
            }
            Assert.assertTrue("child partitions must be assigned after reading starts, got " + started.keySet()
                    + ", messages " + messages, started.keySet().containsAll(childIds));
            int parentMessages = messages.getOrDefault(rootId, new AtomicInteger()).get();
            Assert.assertTrue("parent partition must be read before child sessions start, messages " + messages,
                    parentMessages > 0);

            long childDataDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (System.nanoTime() < childDataDeadline && messageCount(messages, childIds) == 0) {
                if (readerError.get() != null) {
                    throw new AssertionError("reader failed before child data", readerError.get());
                }
                Thread.sleep(200);
            }
            Assert.assertTrue("child partitions must deliver data, messages " + messages,
                    messageCount(messages, childIds) > 0);
            Thread.sleep(2000);
            synchronized (parentDataAfterChild) {
                Assert.assertTrue("parent partition data arrived after child data: " + parentDataAfterChild,
                        parentDataAfterChild.isEmpty());
            }
        } finally {
            reader.shutdown().join();
        }
    }

    private static byte[] randomPayload() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int size = random.nextInt(MIN_MESSAGE_SIZE, MAX_MESSAGE_SIZE + 1);
        byte[] payload = new byte[size];
        // Incompressible bytes make gzip inflate do real work, so decompression takes time.
        random.nextBytes(payload);
        return payload;
    }

    private static int messageCount(ConcurrentHashMap<Long, AtomicInteger> messages, Set<Long> partitionIds) {
        int total = 0;
        for (Long partitionId : partitionIds) {
            AtomicInteger count = messages.get(partitionId);
            if (count != null) {
                total += count.get();
            }
        }
        return total;
    }

    private static boolean hasSplit(List<PartitionInfo> partitions) {
        for (PartitionInfo partition : partitions) {
            if (!partition.getChildPartitionIds().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static PartitionInfo rootPartition(List<PartitionInfo> partitions) {
        PartitionInfo root = null;
        for (PartitionInfo partition : partitions) {
            if (partition.getParentPartitionIds().isEmpty() && !partition.getChildPartitionIds().isEmpty()) {
                root = partition;
                break;
            }
        }
        Assert.assertNotNull("describe must show a split root, partitions: " + formatPartitions(partitions), root);
        return root;
    }

    private static String formatPartitions(List<PartitionInfo> partitions) {
        StringBuilder formatted = new StringBuilder();
        for (PartitionInfo partition : partitions) {
            formatted.append("{id=").append(partition.getPartitionId())
                    .append(", active=").append(partition.isActive())
                    .append(", parents=").append(partition.getParentPartitionIds())
                    .append(", children=").append(partition.getChildPartitionIds())
                    .append("} ");
        }
        return formatted.toString();
    }
}
