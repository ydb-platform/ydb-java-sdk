package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.OffsetsRange;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class BufferManager {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);
    private static final OffsetsRange ALL = OffsetsRange.of(Integer.MIN_VALUE, Integer.MAX_VALUE);

    private final String traceID;
    private final long maxBufferSize;
    private final Consumer<Long> requestFunc;

    private final AtomicLong released = new AtomicLong(0);
    private final ConcurrentHashMap<Long, PartitionBuffer> partitions = new ConcurrentHashMap<>();

    public BufferManager(String traceID, long maxBufferSize, Consumer<Long> requestFunc) {
        this.traceID = traceID;
        this.maxBufferSize = maxBufferSize;
        this.requestFunc = requestFunc;
    }

    public void init(String sessionId) {
        logger.info("[{}] Session {} initialized. Requesting {} bytes...", traceID, sessionId, maxBufferSize);
        requestFunc.accept(maxBufferSize);
    }

    // Has no reentrant thread safety
    public void allocate(int bufferSize, List<YdbTopic.StreamReadMessage.ReadResponse.PartitionData> dataList) {
        logger.trace("[{}] Received ReadResponse of {} bytes", traceID, bufferSize);

        // calculate message count
        int messagesCount = 0;
        for (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data: dataList) {
            for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch: data.getBatchesList()) {
                messagesCount += batch.getMessageDataCount();
            }
        }

        if (messagesCount == 0) {
            logger.error("[{}] Received empty ReadResponse of {} bytes", traceID, bufferSize);
            release(bufferSize);
            return;
        }

        // get real size for every message
        int[] msgSize = new int[messagesCount];
        int msgIdx = 0;
        for (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data: dataList) {
            for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch: data.getBatchesList()) {
                for (YdbTopic.StreamReadMessage.ReadResponse.MessageData msg: batch.getMessageDataList()) {
                    msgSize[msgIdx] = msg.getData().size();
                    msgIdx++;
                }
            }
        }

        // recalculate real messages size to expected buffer size
        recalcBuffer(msgSize, bufferSize);

        // build batch and messages with calculated buffer size
        msgIdx = 0;
        for (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data: dataList) {
            PartitionBuffer part = partitions.computeIfAbsent(data.getPartitionSessionId(), PartitionBuffer::new);
            for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch: data.getBatchesList()) {
                if (batch.getMessageDataCount() <= 0) {
                    continue;
                }

                long startOffset = batch.getMessageData(0).getOffset();
                int[] batchSizes = new int[batch.getMessageDataCount()];
                for (int idx = 0; idx < batch.getMessageDataCount(); idx++) {
                    batchSizes[idx] = msgSize[msgIdx++];
                }

                part.add(new BatchBuffer(startOffset, batchSizes));
            }

            if (!partitions.containsKey(data.getPartitionSessionId())) {
                release(part.release(ALL));
            }
        }
    }

    // Thread safe
    public void releasePartition(Long id) {
        PartitionBuffer part = partitions.remove(id);
        if (part != null) {
            release(part.release(ALL));
        }
    }

    // Thread safe
    public void releaseRange(Long id, OffsetsRange range) {
        PartitionBuffer part = partitions.get(id);
        if (part != null) {
            release(part.release(range));
        }
    }

    private void release(long total) {
        long now = released.addAndGet(total);
        if (now >= maxBufferSize / 10) { // threshhold
            long request = released.getAndSet(0);
            if (request > 0) {
                requestFunc.accept(request);
            }
        }
    }

    private static void recalcBuffer(int[] buffer, int buffSize) {
        // corner cases
        if (buffSize == 0) {
            Arrays.fill(buffer, 0);
            return;
        }

        long total = 0;
        for (int v: buffer) {
            total += v;
        }

        long currBuff = 0;
        long currSum = 0;
        for (int idx = 0; idx < buffer.length; idx += 1) {
            currSum += buffer[idx];
            long newBuff = currSum * buffSize / total;
            buffer[idx] = (int) (newBuff - currBuff);
            currBuff = newBuff;
        }
    }

    private static class PartitionBuffer {
        private final ConcurrentLinkedQueue<BatchBuffer> batches = new ConcurrentLinkedQueue<>();

        PartitionBuffer(Long id) {
        }

        public void add(BatchBuffer range) {
            batches.add(range);
        }

        public long release(OffsetsRange range) {
            long released = 0;

            Iterator<BatchBuffer> it = batches.iterator();
            while (it.hasNext()) {
                BatchBuffer next = it.next();
                if (next.getStartOffset() > range.getEnd()) { // fast path
                    break;
                }

                released += next.release(range);
                if (!next.isActive()) {
                    it.remove();
                }
            }

            return released;
        }
    }

    private static class BatchBuffer {
        private final long startOffset;
        private final AtomicInteger[] messages;
        private final AtomicLong total;

        BatchBuffer(long startOffset, int[] messageSizes) {
            this.startOffset = startOffset;
            this.messages = new AtomicInteger[messageSizes.length];
            long totalSize = 0;
            for (int idx = 0; idx < messageSizes.length; idx++) {
                this.messages[idx] = new AtomicInteger(messageSizes[idx]);
                totalSize += messageSizes[idx];
            }
            this.total = new AtomicLong(totalSize);
        }

        public long getStartOffset() {
            return startOffset;
        }

        public boolean isActive() {
            return total.get() > 0;
        }

        public long release(OffsetsRange range) {
            int first = (int) (Math.max(startOffset, range.getStart()) - startOffset);
            int last = (int) Math.min(messages.length, range.getEnd() - startOffset);

            if (last <= first) {
                return 0;
            }

            long released = 0;
            for (int idx = first; idx < last; idx++) {
                released += messages[idx].getAndSet(0);
            }
            total.addAndGet(-released);

            return released;
        }
    }
}
