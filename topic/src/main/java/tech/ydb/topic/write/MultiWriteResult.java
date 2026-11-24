package tech.ydb.topic.write;

import java.util.Objects;

/**
 * Result for the original logical message, even when a lower ACK covers several sequence numbers.
 * Nullable location fields are populated only when the physical location is known.
 * WRITTEN_IN_TX confirms staging, not commit or visibility.
 */
public final class MultiWriteResult {
    public enum ConfirmationSource {
        ACK,
        PRODUCER_HISTORY
    }

    private final long seqNo;
    private final WriteAck.State state;
    private final ConfirmationSource source;
    private final Long partitionId;
    private final Long offset;

    public MultiWriteResult(long seqNo, WriteAck.State state, ConfirmationSource source,
            Long partitionId, Long offset) {
        if (seqNo <= 0) {
            throw new IllegalArgumentException("Sequence number must be positive");
        }
        this.state = Objects.requireNonNull(state, "state");
        this.source = Objects.requireNonNull(source, "source");
        if (source == ConfirmationSource.PRODUCER_HISTORY && state != WriteAck.State.WRITTEN) {
            throw new IllegalArgumentException("Producer history only confirms ordinary writes");
        }
        if (partitionId != null && partitionId < 0) {
            throw new IllegalArgumentException("Partition ID must be nonnegative");
        }
        if (offset != null && (offset < 0 || partitionId == null || source != ConfirmationSource.ACK
                || state != WriteAck.State.WRITTEN)) {
            throw new IllegalArgumentException("An offset requires a written ACK and its known partition");
        }
        this.seqNo = seqNo;
        this.partitionId = partitionId;
        this.offset = offset;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public WriteAck.State getState() {
        return state;
    }

    public ConfirmationSource getSource() {
        return source;
    }

    /** @return known physical partition ID, or null when history cannot identify it */
    public Long getPartitionId() {
        return partitionId;
    }

    /** @return known offset of this exact message, or null */
    public Long getOffset() {
        return offset;
    }
}
