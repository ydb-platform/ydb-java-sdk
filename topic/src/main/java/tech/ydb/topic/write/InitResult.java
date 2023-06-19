package tech.ydb.topic.write;

/**
 * @author Nikolay Perfilov
 */
public class InitResult {
    private final long seqNo;

    public InitResult(long seqNo) {
        this.seqNo = seqNo;
    }

    public long getSeqNo() {
        return seqNo;
    }
}
