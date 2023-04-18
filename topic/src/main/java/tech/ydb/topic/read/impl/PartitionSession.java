package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;
import tech.ydb.topic.utils.Encoder;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSession {
    private static final Logger logger = LoggerFactory.getLogger(PartitionSession.class);

    private final long id;
    private final String path;
    private final long partitionId;
    private final tech.ydb.topic.read.PartitionSession sessionInfo;
    private final OffsetsRange partitionOffsets;
    private final Executor decompressionExecutor;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);

    private final Queue<Batch> decodingBatches = new LinkedList<>();
    private final Queue<Batch> readingQueue = new ConcurrentLinkedQueue<>();
    private final Function<DataReceivedEvent, CompletableFuture<Void>> dataEventCallback;
    private final AtomicBoolean isReadingNow = new AtomicBoolean(false);
    private final BiConsumer<Long, OffsetsRange> commitFunction;
    private final NavigableMap<Long, CompletableFuture<Void>> commitFutures = new ConcurrentSkipListMap<>();

    private long lastCommittedOffset;

    private PartitionSession(Builder builder) {
        this.id = builder.id;
        this.path = builder.path;
        this.partitionId = builder.partitionId;
        this.sessionInfo = new tech.ydb.topic.read.PartitionSession(id, partitionId, path);
        this.lastCommittedOffset = builder.committedOffset;
        this.partitionOffsets = builder.partitionOffsets;
        this.decompressionExecutor = builder.decompressionExecutor;
        this.dataEventCallback = builder.dataEventCallback;
        this.commitFunction = builder.commitFunction;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void init() {
        logger.info("Partition session {} (partition {}) is started", id, partitionId);
    }

    public long getId() {
        return id;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public String getPath() {
        return path;
    }

    public tech.ydb.topic.read.PartitionSession getSessionInfo() {
        return sessionInfo;
    }

    public CompletableFuture<Void> addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batches) {
        if (!isWorking.get()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> batchFutures = new LinkedList<>();
        batches.forEach(batch -> {
            BatchMeta batchMeta = new BatchMeta(batch);
            Batch newBatch = new Batch(batchMeta);
            List<YdbTopic.StreamReadMessage.ReadResponse.MessageData> batchMessages = batch.getMessageDataList();
            if (!batchMessages.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Received a batch of {} messages (offsets {} - {}) for partition session {} " +
                                    "(partition {})", batchMessages.size(), batchMessages.get(0).getOffset(),
                            batchMessages.get(batchMessages.size() - 1).getOffset(), id, partitionId);
                }
            } else {
                logger.error("Received empty batch for partition session {} (partition {}). This shouldn't happen",
                        id, partitionId);
            }
            batchMessages.forEach(messageData -> {
                long commitOffsetFrom = lastCommittedOffset;
                long messageOffset = messageData.getOffset();
                long newCommittedOffset = messageOffset + 1;
                if (newCommittedOffset > lastCommittedOffset) {
                    lastCommittedOffset = newCommittedOffset;
                } else {
                    logger.error("Received a message with offset {} which is less than last read committedOffset {} " +
                                    "for partition session {} (partition {})",
                            messageOffset, lastCommittedOffset, id, partitionId);
                }
                lastCommittedOffset = Math.max(messageData.getOffset() + 1, lastCommittedOffset);
                newBatch.addMessage(new MessageImpl.Builder()
                        .setBatchMeta(batchMeta)
                        .setPartitionSession(sessionInfo)
                        .setData(messageData.getData().toByteArray())
                        .setOffset(messageOffset)
                        .setSeqNo(messageData.getSeqNo())
                        .setCommitOffsetFrom(commitOffsetFrom)
                        .setCreatedAt(ProtobufUtils.protoToInstant(messageData.getCreatedAt()))
                        .setMessageGroupId(messageData.getMessageGroupId())
                        .setCommitFunction(this::commitOffset)
                        .build()
                );
            });
            batchFutures.add(newBatch.getReadFuture());
            decodingBatches.add(newBatch);

            CompletableFuture.runAsync(() -> decode(newBatch), decompressionExecutor)
                    .thenRun(() -> {
                        boolean haveNewBatchesReady = false;
                        synchronized (decodingBatches) {
                            // Taking all encoded messages to sending queue
                            while (true) {
                                Batch decodingBatch = decodingBatches.peek();
                                if (decodingBatch != null
                                        && (decodingBatch.isDecompressed() || decodingBatch.getCodec() == Codec.RAW)) {
                                    decodingBatches.remove();
                                    if (logger.isTraceEnabled()) {
                                        logger.trace("Adding message to reading queue of partition session {} " +
                                                "(partition {})", id, partitionId);
                                    }
                                    readingQueue.add(decodingBatch);
                                    haveNewBatchesReady = true;
                                } else {
                                    break;
                                }
                            }
                        }
                        if (haveNewBatchesReady) {
                            sendDataToReadersIfNeeded();
                        }
                    });
        });
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> commitOffset(OffsetsRange offsets) {
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        if (isWorking.get()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Offset range [{}, {}) is requested to be committed for partition session {} " +
                                "(partition {})", offsets.getStart(), offsets.getEnd(), id, partitionId);
            }
            commitFutures.put(offsets.getEnd(), resultFuture);
            commitFunction.accept(getId(), offsets);
        } else {
            logger.info("Offset range [{}, {}) is requested to be committed, but partition session {} " +
                    "(partition {}) is already closed", offsets.getStart(), offsets.getEnd(), id, partitionId);
            resultFuture.completeExceptionally(new RuntimeException("Partition session " + id + " is already closed"));
        }
        return resultFuture;
    }

    public void handleCommitResponse(long committedOffset) {
        Map<Long, CompletableFuture<Void>> futuresToComplete = commitFutures.headMap(committedOffset, true);
        if (logger.isDebugEnabled()) {
            logger.debug("Commit response received for partition session {} (partition {}). Committed offset: {}. " +
                            "Completing {} commit futures", id, partitionId, committedOffset, futuresToComplete.size());
        }
        futuresToComplete.values().forEach(future -> future.complete(null));
        futuresToComplete.clear();
    }

    private void decode(Batch batch) {
        if (logger.isTraceEnabled()) {
            logger.trace("Started decoding batch for partition session {} (partition {})", id, partitionId);
        }
        if (batch.getCodec() == Codec.RAW) {
            return;
        }

        batch.getMessages().forEach(message -> {
            message.setData(Encoder.decode(batch.getCodec(), message.getData()));
            message.setDecompressed(true);
        });
        batch.setDecompressed(true);

        if (logger.isTraceEnabled()) {
            logger.trace("Finished decoding batch for partition session {} (partition {})", id, partitionId);
        }
    }

    private void sendDataToReadersIfNeeded() {
        if (isReadingNow.compareAndSet(false, true)) {
            Batch batchToRead = readingQueue.poll();
            if (batchToRead == null) {
                isReadingNow.set(false);
                return;
            }
            // Should be called maximum in 1 thread at a time
            List<MessageImpl> messageImplList = batchToRead.getMessages();
            List<Message> messagesToRead = new ArrayList<>(messageImplList);
            DataReceivedEvent event = new DataReceivedEventImpl(messagesToRead, sessionInfo,
                    () -> commitOffset(new OffsetsRange(messageImplList.get(0).getCommitOffsetFrom(),
                            messageImplList.get(messageImplList.size() - 1).getOffset() + 1)));
            if (logger.isDebugEnabled()) {
                logger.debug("DataReceivedEvent callback for partition session {} (partition {}) is about to be " +
                        "called...", id, partitionId);
            }
            dataEventCallback.apply(event)
                    .whenComplete((res, th) -> {
                        if (th != null) {
                            logger.error("DataReceivedEvent callback for partition session {} (partition {}) " +
                                    "finished with error: {}", id, partitionId, th);
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("DataReceivedEvent callback for partition session {} (partition {}) " +
                                            "successfully finished", id, partitionId);
                        }
                        isReadingNow.set(false);
                        batchToRead.complete();
                        sendDataToReadersIfNeeded();
                    });
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Partition session {} (partition {}) - no need to send data to readers: " +
                        "reading is already being performed", id, partitionId);
            }
        }
    }

    public void shutdown() {
        isWorking.set(false);
        logger.info("Partition session {} (partition {}) is shutting down. Failing {} commit futures...", id,
                partitionId, commitFutures.size());
        commitFutures.values().forEach(f -> f.completeExceptionally(new RuntimeException("Partition session closed")));
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private long id;
        private String path;
        private long partitionId;
        private long committedOffset;
        private OffsetsRange partitionOffsets;
        private Executor decompressionExecutor;
        private Function<DataReceivedEvent, CompletableFuture<Void>> dataEventCallback;
        private BiConsumer<Long, OffsetsRange> commitFunction;

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setPartitionId(long partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        public Builder setCommittedOffset(long committedOffset) {
            this.committedOffset = committedOffset;
            return this;
        }

        public Builder setPartitionOffsets(OffsetsRange partitionOffsets) {
            this.partitionOffsets = partitionOffsets;
            return this;
        }

        public Builder setDecompressionExecutor(Executor decompressionExecutor) {
            this.decompressionExecutor = decompressionExecutor;
            return this;
        }

        public Builder setDataEventCallback(Function<DataReceivedEvent, CompletableFuture<Void>> dataEventCallback) {
            this.dataEventCallback = dataEventCallback;
            return this;
        }

        public Builder setCommitFunction(BiConsumer<Long, OffsetsRange> commitFunction) {
            this.commitFunction = commitFunction;
            return this;
        }

        public PartitionSession build() {
            return new PartitionSession(this);
        }
    }
}
