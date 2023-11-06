package tech.ydb.topic.read.impl.events;

import java.util.function.Consumer;

import tech.ydb.topic.read.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

/**
 * @author Nikolay Perfilov
 */
public class StartPartitionSessionEventImpl implements StartPartitionSessionEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;
    private final OffsetsRange partitionOffsets;
    private final Consumer<StartPartitionSessionSettings> confirmCallback;

    public StartPartitionSessionEventImpl(PartitionSession partitionSession, long committedOffset,
                                          OffsetsRange partitionOffsets,
                                          Consumer<StartPartitionSessionSettings> confirmCallback) {
        this.partitionSession = partitionSession;
        this.committedOffset = committedOffset;
        this.partitionOffsets = partitionOffsets;
        this.confirmCallback = confirmCallback;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }

    @Override
    public OffsetsRange getPartitionOffsets() {
        return partitionOffsets;
    }

    @Override
    public void confirm() {
        confirmCallback.accept(null);
    }

    @Override
    public void confirm(StartPartitionSessionSettings settings) {
        confirmCallback.accept(settings);
    }
}
