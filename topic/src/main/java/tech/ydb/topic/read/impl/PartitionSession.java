package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final OffsetsRange partitionOffsets;
    private final Executor decompressionExecutor;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);

    private final Queue<Batch> decodingBatches = new LinkedList<>();
    private final Queue<Batch> readingQueue = new ConcurrentLinkedQueue<>();
    private final Function<DataReceivedEvent, CompletableFuture<Void>> dataEventCallback;
    private final AtomicBoolean isReadingNow = new AtomicBoolean(false);
    private final BiConsumer<Long, OffsetsRange> commitFunction;
    private final Map<Long, CompletableFuture<Void>> commitFutures = new TreeMap<>();

    private long lastCommittedOffset;

    private PartitionSession(Builder builder) {
        this.id = builder.id;
        this.path = builder.path;
        this.partitionId = builder.partitionId;
        this.lastCommittedOffset = builder.committedOffset;
        this.partitionOffsets = builder.partitionOffsets;
        this.decompressionExecutor = builder.decompressionExecutor;
        this.dataEventCallback = builder.dataEventCallback;
        this.commitFunction = builder.commitFunction;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public CompletableFuture<Void> addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batches) {
        if (!isWorking.get()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> batchFutures = new LinkedList<>();
        batches.forEach(batch -> {
            BatchMeta batchMeta = new BatchMeta(batch);
            Batch newBatch = new Batch(batchMeta);
            batch.getMessageDataList().forEach(messageData -> {
                long commitOffsetFrom = lastCommittedOffset;
                long messageOffset = messageData.getOffset();
                long newCommittedOffset = messageOffset + 1;
                if (newCommittedOffset > lastCommittedOffset) {
                    lastCommittedOffset = newCommittedOffset;
                } else {
                    logger.error("Received a message with offset {} which is less than last read committedOffset {}",
                            messageOffset, lastCommittedOffset);
                }
                lastCommittedOffset = Math.max(messageData.getOffset() + 1, lastCommittedOffset);
                newBatch.addMessage(new MessageImpl.Builder()
                        .setBatchMeta(batchMeta)
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
                                        logger.trace("Adding message to reading queue");
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
            synchronized (commitFutures) {
                commitFutures.put(offsets.getEnd(), resultFuture);
            }
            commitFunction.accept(getId(), offsets);
        } else {
            resultFuture.completeExceptionally(new RuntimeException("Partition session is already closed"));
        }
        return resultFuture;
    }

    public void handleCommitResponse(long committedOffset) {
        synchronized (commitFutures) {
            commitFutures.entrySet().iterator();
            for (Iterator<Map.Entry<Long, CompletableFuture<Void>>> it = commitFutures.entrySet().iterator();
                 it.hasNext(); ) {
                Map.Entry<Long, CompletableFuture<Void>> entry = it.next();
                if (entry.getKey() <= committedOffset) {
                    entry.getValue().complete(null);
                    it.remove();
                } else {
                    return;
                }
            }
        }
    }

    private void decode(Batch batch) {
        if (logger.isTraceEnabled()) {
            logger.trace("Started decoding batch");
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
            logger.trace("Finished decoding batch");
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
            if (logger.isTraceEnabled()) {
                logger.trace("reading message batch");
            }
            List<MessageImpl> messageImplList = batchToRead.getMessages();
            List<Message> messagesToRead = new ArrayList<>(messageImplList);
            DataReceivedEvent event = new DataReceivedEventImpl(messagesToRead,
                    () -> commitOffset(new OffsetsRange(messageImplList.get(0).getCommitOffsetFrom(),
                            messageImplList.get(messageImplList.size() - 1).getOffset() + 1)));
            dataEventCallback.apply(event)
                    .whenComplete((res, th) -> {
                        if (th != null) {
                            logger.error("Error in DataReceivedEvent callback: ", th);
                        }
                        isReadingNow.set(false);
                        sendDataToReadersIfNeeded();
                    });
            batchToRead.complete();
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("No need to send data to readers: reading is already being performed");
            }
        }
    }

    public void shutdown() {
        isWorking.set(false);
        synchronized (commitFutures) {
            commitFutures.values().forEach(f ->
                    f.completeExceptionally(new RuntimeException("Partition session closed")));
        }
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
