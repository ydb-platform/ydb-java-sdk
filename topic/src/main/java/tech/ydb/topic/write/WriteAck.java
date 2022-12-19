package tech.ydb.topic.write;

/**
 * @author Nikolay Perfilov
 */
public class WriteAck {
    private final long seqNo;
    private final State state;
    private final Details details;

    public WriteAck(long seqNo, State state, Details details) {
        this.seqNo = seqNo;
        this.state = state;
        this.details = details;
    }

    public enum State {
        WRITTEN,
        ALREADY_WRITTEN,
        DISCARDED
    }

    public static class Details {
        private final long offset;
        private final long partitionId;

        private Details(long offset, long partitionId) {
            this.offset = offset;
            this.partitionId = partitionId;
        }

        public long getOffset() {
            return offset;
        }

        public long getPartitionId() {
            return partitionId;
        }
    }

    public long getSeqNo() {
        return seqNo;
    }

    public State getState() {
        return state;
    }

    public Details getDetails() {
        return details;
    }
}
