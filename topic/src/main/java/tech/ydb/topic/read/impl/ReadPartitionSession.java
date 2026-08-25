package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.SerialExecutor;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReadPartitionSession {

    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private final String traceID;
    private final ReadSession session;
    private final PartitionSession partition;
    private final int maxBatchSize;
    private final BufferManager bufferManager;
    private final SerialExecutor executor;
    private final MessageCommitterImpl committer;
    private final ReadPartitionDecoder decoder;
    private volatile long lastReadOffset;

    private volatile boolean isStopped = false;

    private final Queue<MessageImpl> readingQueue = new ConcurrentLinkedQueue<>();

    ReadPartitionSession(String traceID, ReadSession session, PartitionSession partition, Executor executor,
            long lastCommittedOffset) {
        this.traceID = traceID;
        this.session = session;
        this.partition = partition;
        this.maxBatchSize = session.getMaxBatchSize();
        this.bufferManager = session.getBufferManager();
        this.executor = new SerialExecutor(executor);
        this.committer = new MessageCommitterImpl(this, lastCommittedOffset);
        this.decoder = new ReadPartitionDecoder(traceID, session.getMessageDecoder(), partition, committer,
                this::sendDataToReaders);
        this.lastReadOffset = lastCommittedOffset;
    }

    public PartitionSession getPartition() {
        return partition;
    }

    @Override
    public String toString() {
        return "[" + traceID + "]";
    }

    public boolean isStopped() {
        return isStopped;
    }

    boolean commitOffsets(List<OffsetsRange> ranges) {
        if (isStopped) {
            logger.info("[{}] Offset ranges {} are requested to be committed, but partition session is already closed",
                    traceID, ranges.stream().map(OffsetsRange::toString).collect(Collectors.joining(",")));
            return false;
        }
        session.sendCommitOffsetRequest(partition, ranges);
        return true;
    }

    void confirmCommit(long committedOffset) {
        committer.confirmCommit(committedOffset);
    }

    public void stop() {
        isStopped = true;
        committer.failPendingCommits();
        decoder.close();
        logger.info("[{}] stopped", traceID);
    }

    public abstract void handleDataReceivedEvent(DataReceivedEvent event);

    public boolean addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
        if (isStopped) {
            return false;
        }

        for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch : batchList) {
            if (batch.getMessageDataCount() == 0) {
                logger.error("[{}] Received empty batch. This shouldn't happen", traceID);
                continue;
            }

            BatchMeta meta = new BatchMeta(batch);
            List<MessageImpl> messages = new ArrayList<>(batch.getMessageDataCount());
            for (YdbTopic.StreamReadMessage.ReadResponse.MessageData msg : batch.getMessageDataList()) {
                if (lastReadOffset > msg.getOffset()) {
                    logger.error("[{}] Received a message with offset {} which is less than last read offset {} ",
                            traceID, msg.getOffset(), lastReadOffset);
                    lastReadOffset = msg.getOffset();
                }

                OffsetsRange commitRange = OffsetsRange.of(lastReadOffset, msg.getOffset() + 1);
                if (meta.getCodec() == Codec.RAW) {
                    messages.add(new RawMessage(meta, commitRange, msg));
                } else {
                    messages.add(decoder.decode(meta, commitRange, msg));
                }

                lastReadOffset = commitRange.getEnd();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("[{}] Received a batch of {} messages (offsets {} - {})", traceID, messages.size(),
                        messages.get(0).getOffset(), messages.get(messages.size() - 1).getOffset());
            }

            readingQueue.addAll(messages);
        }

        sendDataToReaders();
        return !isStopped;
    }

    public void releaseRange(OffsetsRange range) {
        decoder.releaseRange(range);
        bufferManager.releaseRange(partition.getId(), range);
        sendDataToReaders();
    }

    private void sendDataToReaders() {
        executor.execute(() -> {
            while (!isStopped) {
                Iterator<MessageImpl> it = readingQueue.iterator();
                if (!it.hasNext()) {
                    return;
                }

                MessageImpl next = it.next();
                if (!next.isReady()) {
                    return;
                }

                List<Message> messagesToRead = new ArrayList<>();
                while (next != null && next.isReady() && (maxBatchSize <= 0 || messagesToRead.size() < maxBatchSize)) {
                    messagesToRead.add(next);
                    it.remove();
                    next = it.hasNext() ? it.next() : null;
                }

                // Should be called maximum in 1 thread at a time
                DataReceivedEvent event = new DataReceivedEventImpl(partition, committer, messagesToRead);
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) is about "
                        + "to be called...", traceID, messagesToRead.size(),
                        messagesToRead.get(0).getOffset(),
                        messagesToRead.get(messagesToRead.size() - 1).getOffset());

                handleDataReceivedEvent(event);
            }
        });
    }

    private class RawMessage extends MessageImpl {
        private final byte[] data;

        RawMessage(BatchMeta meta, OffsetsRange range, YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
            super(partition, committer, meta, range, msg);
            this.data = msg.getData().toByteArray();
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public boolean isReady() {
            return true;
        }
    }
}
