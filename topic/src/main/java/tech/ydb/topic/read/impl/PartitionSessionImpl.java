package tech.ydb.topic.read.impl;

import java.io.IOException;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.rpc.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.utils.Encoder;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSessionImpl {
    private static final Logger logger = LoggerFactory.getLogger(PartitionSessionImpl.class);

    private final long id;
    private final String fullId;
    private final String topicPath;
    private final String consumerName;
    private final long partitionId;
    private final PartitionSession sessionInfo;
    private final Executor decompressionExecutor;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);

    private final Queue<Batch> decodingBatches = new LinkedList<>();
    private final ReentrantLock decodingBatchesLock = new ReentrantLock();
    private final Queue<Batch> readingQueue = new ConcurrentLinkedQueue<>();
    private final Function<DataReceivedEvent, CompletableFuture<Void>> dataEventCallback;
    private final AtomicBoolean isReadingNow = new AtomicBoolean();
    private final Consumer<List<OffsetsRange>> commitFunction;
    private final NavigableMap<Long, CompletableFuture<Void>> commitFutures = new ConcurrentSkipListMap<>();
    private final ReentrantLock commitFuturesLock = new ReentrantLock();
    private final CodecRegistry codecRegistry;
    // Offset of the last read message + 1
    private long lastReadOffset;
    private long lastCommittedOffset;
 

    private PartitionSessionImpl(Builder builder) {
        this.id = builder.id;
        this.fullId = builder.fullId;
        this.topicPath = builder.topicPath;
        this.consumerName = builder.consumerName;
        this.partitionId = builder.partitionId;
        this.sessionInfo = new PartitionSession(id, partitionId, topicPath);
        this.lastReadOffset = builder.committedOffset;
        this.lastCommittedOffset = builder.committedOffset;
        this.decompressionExecutor = builder.decompressionExecutor;
        this.dataEventCallback = builder.dataEventCallback;
        this.commitFunction = builder.commitFunction;
        this.codecRegistry = builder.codecRegistry;
        logger.info("[{}] Partition session is started for Topic \"{}\" and Consumer \"{}\". CommittedOffset: {}. " +
                "Partition offsets: {}-{}", fullId, topicPath, consumerName, lastReadOffset,
                builder.partitionOffsets.getStart(), builder.partitionOffsets.getEnd());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getId() {
        return id;
    }

    public long getFullId() {
        return id;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public String getTopicPath() {
        return topicPath;
    }

    public PartitionSession getSessionInfo() {
        return sessionInfo;
    }

    public void setLastReadOffset(long lastReadOffset) {
        this.lastReadOffset = lastReadOffset;
    }

    public void setLastCommittedOffset(long lastCommittedOffset) {
        this.lastCommittedOffset = lastCommittedOffset;
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
                    logger.debug("[{}] Received a batch of {} messages (offsets {} - {})", fullId, batchMessages.size(),
                            batchMessages.get(0).getOffset(), batchMessages.get(batchMessages.size() - 1).getOffset());
                }
            } else {
                logger.error("[{}] Received empty batch. This shouldn't happen", fullId);
            }
            batchMessages.forEach(messageData -> {
                long commitOffsetFrom = lastReadOffset;
                long messageOffset = messageData.getOffset();
                long newReadOffset = messageOffset + 1;
                if (newReadOffset > lastReadOffset) {
                    lastReadOffset = newReadOffset;
                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Received a message with offset {}. lastReadOffset is now {}", fullId,
                                messageOffset, lastReadOffset);
                    }
                } else {
                    logger.error("[{}] Received a message with offset {} which is less than last read offset {} ",
                            fullId, messageOffset, lastReadOffset);
                }
                newBatch.addMessage(new MessageImpl.Builder()
                        .setBatchMeta(batchMeta)
                        .setPartitionSession(this)
                        .setData(messageData.getData().toByteArray())
                        .setOffset(messageOffset)
                        .setSeqNo(messageData.getSeqNo())
                        .setCommitOffsetFrom(commitOffsetFrom)
                        .setCreatedAt(ProtobufUtils.protoToInstant(messageData.getCreatedAt()))
                        .setMessageGroupId(messageData.getMessageGroupId())
                        .setMetadataItems(messageData.getMetadataItemsList()
                                .stream()
                                .map(metadataItem -> new MetadataItem(metadataItem.getKey(),
                                        metadataItem.getValue().toByteArray()))
                                .collect(Collectors.toList()))
                        .build()
                );
            });
            batchFutures.add(newBatch.getReadFuture());

            decodingBatchesLock.lock();

            try {
                decodingBatches.add(newBatch);
            } finally {
                decodingBatchesLock.unlock();
            }

            CompletableFuture.runAsync(() -> decode(newBatch), decompressionExecutor)
                    .thenRun(() -> {
                        boolean haveNewBatchesReady = false;
                        decodingBatchesLock.lock();

                        try {
                            // Taking all encoded messages to sending queue
                            while (true) {
                                Batch decodingBatch = decodingBatches.peek();
                                if (decodingBatch != null
                                        && (decodingBatch.isDecompressed() || decodingBatch.getCodec() == Codec.RAW)) {
                                    decodingBatches.remove();
                                    if (logger.isTraceEnabled()) {
                                        List<MessageImpl> messages = decodingBatch.getMessages();
                                        logger.trace("[{}] Adding batch with offsets {}-{} to reading queue", fullId,
                                                messages.get(0).getOffset(),
                                                messages.get(messages.size() - 1).getOffset());
                                    }
                                    readingQueue.add(decodingBatch);
                                    haveNewBatchesReady = true;
                                } else {
                                    break;
                                }
                            }
                        } finally {
                            decodingBatchesLock.unlock();
                        }

                        if (haveNewBatchesReady) {
                            sendDataToReadersIfNeeded();
                        }
                    });
        });
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture<?>[0]));
    }

    // Commit single offset range with result future
    public CompletableFuture<Void> commitOffsetRange(OffsetsRange rangeToCommit) {
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        commitFuturesLock.lock();

        try {
            if (isWorking.get()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[{}] Offset range [{}, {}) is requested to be committed. Last committed offset is" +
                                    " {} (commit lag is {})", fullId, rangeToCommit.getStart(), rangeToCommit.getEnd(),
                            lastCommittedOffset, rangeToCommit.getStart() - lastCommittedOffset);
                }
                commitFutures.put(rangeToCommit.getEnd(), resultFuture);
            } else {
                logger.info("[{}] Offset range [{}, {}) is requested to be committed, but partition session " +
                        "is already closed", fullId, rangeToCommit.getStart(), rangeToCommit.getEnd());
                resultFuture.completeExceptionally(new RuntimeException("Partition session " + id + " (partition " +
                        partitionId + ") for " + topicPath + " is already closed"));
                return resultFuture;
            }
        } finally {
            commitFuturesLock.unlock();
        }
        List<OffsetsRange> rangeWrapper = new ArrayList<>(1);
        rangeWrapper.add(rangeToCommit);
        commitFunction.accept(rangeWrapper);
        return resultFuture;
    }

    // Bulk commit without result future
    public void commitOffsetRanges(List<OffsetsRange> rangesToCommit) {
        if (isWorking.get()) {
            if (logger.isInfoEnabled()) {
                StringBuilder message = new StringBuilder("[").append(fullId)
                        .append("] Sending CommitRequest with offset ranges ");
                addRangesToString(message, rangesToCommit);
                logger.debug(message.toString());
            }
            commitFunction.accept(rangesToCommit);
        } else if (logger.isInfoEnabled()) {
            StringBuilder message = new StringBuilder("[").append(fullId).append("] Offset ranges ");
            addRangesToString(message, rangesToCommit);
            message.append(" are requested to be committed, but partition session is already closed");
            logger.info(message.toString());
        }
    }

    private static void addRangesToString(StringBuilder stringBuilder, List<OffsetsRange> ranges) {
        for (int i = 0; i < ranges.size(); i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            OffsetsRange range = ranges.get(i);
            stringBuilder.append("[").append(range.getStart()).append(",").append(range.getEnd()).append(")");
        }
    }

    public void handleCommitResponse(long committedOffset) {
        if (committedOffset <= lastCommittedOffset) {
            logger.error("[{}] Commit response received. Committed offset: {} which is less than previous " +
                    "committed offset: {}.", fullId, committedOffset, lastCommittedOffset);
            return;
        }
        Map<Long, CompletableFuture<Void>> futuresToComplete = commitFutures.headMap(committedOffset, true);
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Commit response received. Committed offset: {}. Previous committed offset: {} " +
                            "(diff is {} message(s)). Completing {} commit futures", fullId, committedOffset,
                    lastCommittedOffset, committedOffset - lastCommittedOffset, futuresToComplete.size());
        }
        lastCommittedOffset = committedOffset;
        futuresToComplete.values().forEach(future -> future.complete(null));
        futuresToComplete.clear();
    }

    private void decode(Batch batch) {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Started decoding batch", fullId);
        }
        if (batch.getCodec() == Codec.RAW) {
            return;
        }

        batch.getMessages().forEach(message -> {
            try {
                message.setData(Encoder.decode(batch.getCodec(), this.codecRegistry, message.getData()));
                message.setDecompressed(true);
            } catch (IOException exception) {
                message.setException(exception);
                logger.warn("[{}] Exception was thrown while decoding a message: ", fullId, exception);
            }
        });
        batch.setDecompressed(true);

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Finished decoding batch", fullId);
        }
    }

    private void sendDataToReadersIfNeeded() {
        if (!isWorking.get()) {
            return;
        }
        if (isReadingNow.compareAndSet(false, true)) {
            Batch batchToRead = readingQueue.poll();
            if (batchToRead == null) {
                isReadingNow.set(false);
                return;
            }
            // Should be called maximum in 1 thread at a time
            List<MessageImpl> messageImplList = batchToRead.getMessages();
            List<Message> messagesToRead = new ArrayList<>(messageImplList);
            OffsetsRange offsetsToCommit = new OffsetsRangeImpl(messageImplList.get(0).getCommitOffsetFrom(),
                    messageImplList.get(messageImplList.size() - 1).getOffset() + 1);
            DataReceivedEvent event = new DataReceivedEventImpl(this, messagesToRead, offsetsToCommit);
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) is about " +
                                "to be called...", fullId, messagesToRead.size(), messagesToRead.get(0).getOffset(),
                        messagesToRead.get(messagesToRead.size() - 1).getOffset());
            }
            dataEventCallback.apply(event)
                    .whenComplete((res, th) -> {
                        if (th != null) {
                            logger.error("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) finished" +
                                            " with error: ", fullId, messagesToRead.size(),
                                    messagesToRead.get(0).getOffset(),
                                    messagesToRead.get(messagesToRead.size() - 1).getOffset(), th);
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) " +
                                            "successfully finished", fullId, messagesToRead.size(),
                                    messagesToRead.get(0).getOffset(),
                                    messagesToRead.get(messagesToRead.size() - 1).getOffset());
                        }
                        isReadingNow.set(false);
                        batchToRead.complete();
                        sendDataToReadersIfNeeded();
                    });
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] No need to send data to readers: reading is already being performed", fullId);
            }
        }
    }

    public void shutdown() {
        commitFuturesLock.lock();

        try {
            isWorking.set(false);
            logger.info("[{}] Partition session for {} is shutting down. Failing {} commit futures...", fullId,
                    topicPath, commitFutures.size());
            commitFutures.values().forEach(f -> f.completeExceptionally(new RuntimeException("Partition session " + id +
                    " (partition " + partitionId + ") for " + topicPath + " is closed")));
        } finally {
            commitFuturesLock.unlock();
        }

        decodingBatchesLock.lock();

        try {
            decodingBatches.forEach(Batch::complete);
            readingQueue.forEach(Batch::complete);
        } finally {
            decodingBatchesLock.unlock();
        }
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private long id;
        private String fullId;
        private String topicPath;
        private String consumerName;
        private long partitionId;
        private long committedOffset;
        private OffsetsRange partitionOffsets;
        private Executor decompressionExecutor;
        private Function<DataReceivedEvent, CompletableFuture<Void>> dataEventCallback;
        private Consumer<List<OffsetsRange>> commitFunction;
        private ReaderSettings readerSettings;
        private CodecRegistry codecRegistry;

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setFullId(String fullId) {
            this.fullId = fullId;
            return this;
        }

        public Builder setTopicPath(String topicPath) {
            this.topicPath = topicPath;
            return this;
        }

        public Builder setConsumerName(String consumerName) {
            this.consumerName = consumerName;
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

        public Builder setCommitFunction(Consumer<List<OffsetsRange>> commitFunction) {
            this.commitFunction = commitFunction;
            return this;
        }

        public PartitionSessionImpl build() {
            return new PartitionSessionImpl(this);
        }

        public Builder setCodecRegistry(CodecRegistry codecRegistry) {
            this.codecRegistry = codecRegistry;
            return this;
        }
    }
}
