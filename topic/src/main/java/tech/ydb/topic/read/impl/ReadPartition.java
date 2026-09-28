package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.SerialExecutor;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;
import tech.ydb.topic.read.impl.events.PartitionControl;

/**
 * @author Nikolay Perfilov
 */
public class ReadPartition implements PartitionControl {
    private static final Logger logger = LoggerFactory.getLogger(ReadPartition.class);

    private final String traceID;
    private final PartitionSession partition;
    private final ReadPartitionCommitter committer;
    private final ReadPartitionDecoder decoder;
    private final ReadPartitionQueue queue;
    private final BufferManager bufferManager;
    private final Consumer<DataReceivedEventImpl> eventConsumer;

    private final SerialExecutor dataProcessor;

    private volatile boolean isStopped = false;

    ReadPartition(String traceID, ReadSession session, PartitionSession partition, long lastCommittedOffset) {
        this.traceID = traceID;
        this.partition = partition;

        ReadConfig config = session.getConfig();
        MessageDecoder sessionDecoder = session.getDecoder();

        this.committer = new ReadPartitionCommitter(traceID, session, this, lastCommittedOffset);
        this.decoder = new ReadPartitionDecoder(traceID, sessionDecoder, partition, committer, this::sendDataToReaders);
        this.queue = new ReadPartitionQueue(traceID, decoder, config.getMaxBatchSize(), lastCommittedOffset);

        this.dataProcessor = new SerialExecutor(config.getProcessor());
        this.bufferManager = session.getBufferManager();
        this.eventConsumer = session.getEventConsumer();
    }

    @Override
    public boolean isActive() {
        return !isStopped;
    }

    @Override
    public void confirmProcessedRange(OffsetsRange range) {
        releaseRange(range);
        sendDataToReaders();
    }

    @Override
    public PartitionSession getPartition() {
        return partition;
    }

    public void confirmCommittedOffset(long committedOffset) {
        committer.confirmCommit(committedOffset);
    }

    public void stop() {
        isStopped = true;
        decoder.close();
        committer.failPendingCommits();
        logger.info("[{}] stopped", traceID);
    }

    public boolean addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
        if (isStopped) {
            return false;
        }
        queue.addBatches(batchList);
        sendDataToReaders();
        return !isStopped;
    }

    void releaseRange(OffsetsRange range) {
        bufferManager.releaseRange(partition.getId(), range);
        decoder.releaseRange(range);
    }

    void sendDataToReaders() {
        dataProcessor.execute(() -> {
            while (!isStopped) {
                List<Message> list = queue.getNextBatch();
                if (list == null) {
                    return;
                }
                eventConsumer.accept(new DataReceivedEventImpl(this, committer, list));
            }
        });
    }
}
