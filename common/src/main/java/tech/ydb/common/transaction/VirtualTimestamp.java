package tech.ydb.common.transaction;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class VirtualTimestamp {
    private final long planStep;
    private final long txId;

    public VirtualTimestamp(long planStep, long txId) {
        this.planStep = planStep;
        this.txId = txId;
    }

    public long getPlanStep() {
        return planStep;
    }

    public long getTxId() {
        return txId;
    }
}
