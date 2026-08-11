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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof VirtualTimestamp)) {
            return false;
        }

        VirtualTimestamp that = (VirtualTimestamp) o;
        return planStep == that.planStep && txId == that.txId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(planStep) * 31 + Long.hashCode(txId);
    }

    @Override
    public String toString() {
        return "VirtualTimestamp{"
                + "planStep=" + Long.toUnsignedString(planStep)
                + ", txId=" + Long.toUnsignedString(txId)
                + "}";
    }
}
