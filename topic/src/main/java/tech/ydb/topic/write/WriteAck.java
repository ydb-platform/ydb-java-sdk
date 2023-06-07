package tech.ydb.topic.write;

import javax.annotation.Nullable;

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
        ALREADY_WRITTEN
    }

    public static class Details {
        private final long offset;

        public Details(long offset) {
            this.offset = offset;
        }

        public long getOffset() {
            return offset;
        }
    }

    public long getSeqNo() {
        return seqNo;
    }

    public State getState() {
        return state;
    }

    @Nullable
    public Details getDetails() {
        return details;
    }
}
