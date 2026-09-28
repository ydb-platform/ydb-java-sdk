package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;

/**
 * @author Nikolay Perfilov
 */
class ReadPartitionQueue {
    private static final Logger logger = LoggerFactory.getLogger(ReadPartition.class);

    private final String traceID;
    private final ReadPartitionDecoder decoder;
    private final int maxBatchSize;
    private final Queue<MessageImpl> readingQueue = new ConcurrentLinkedQueue<>();
    private volatile long lastReadOffset;

    ReadPartitionQueue(String traceID, ReadPartitionDecoder decoder, int maxBatchSize, long lastCommittedOffset) {
        this.traceID = traceID;
        this.decoder = decoder;
        this.maxBatchSize = maxBatchSize;
        this.lastReadOffset = lastCommittedOffset;
    }

    void addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
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
                messages.add(decoder.decode(meta, commitRange, msg));
                lastReadOffset = commitRange.getEnd();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("[{}] Received a batch of {} messages (offsets {} - {})", traceID, messages.size(),
                        messages.get(0).getOffset(), messages.get(messages.size() - 1).getOffset());
            }

            readingQueue.addAll(messages);
        }
    }

    List<Message> getNextBatch() {
        Iterator<MessageImpl> it = readingQueue.iterator();
        if (!it.hasNext()) {
            return null;
        }

        MessageImpl next = it.next();
        if (!next.isReady()) {
            return null;
        }

        List<Message> messagesToRead = new ArrayList<>();
        while (next != null && next.isReady() && (maxBatchSize <= 0 || messagesToRead.size() < maxBatchSize)) {
            messagesToRead.add(next);
            it.remove();
            next = it.hasNext() ? it.next() : null;
        }

        return messagesToRead;
    }
}
