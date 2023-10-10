package tech.ydb.coordination.settings;

import java.util.Arrays;
import java.util.Objects;

public class SemaphoreSession {
    private final long id;
    private final long timeoutMillis;
    private final long count;
    private final byte[] data;
    private final long orderId;

    public SemaphoreSession(tech.ydb.proto.coordination.SemaphoreSession semaphoreSession) {
        this.id = semaphoreSession.getSessionId();
        this.timeoutMillis = semaphoreSession.getTimeoutMillis();
        this.count = semaphoreSession.getCount();
        this.data = semaphoreSession.getData().toByteArray();
        this.orderId = semaphoreSession.getOrderId();
    }

    public long getId() {
        return id;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public long getCount() {
        return count;
    }

    public byte[] getData() {
        return data;
    }

    public long getOrderId() {
        return orderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SemaphoreSession)) {
            return false;
        }
        SemaphoreSession that = (SemaphoreSession) o;
        return id == that.id && timeoutMillis == that.timeoutMillis && count == that.count && orderId == that.orderId &&
                Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, timeoutMillis, count, orderId);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "SemaphoreSession{" +
                "id=" + id +
                ", timeoutMillis=" + timeoutMillis +
                ", count=" + count +
                ", data=" + Arrays.toString(data) +
                ", orderId=" + orderId +
                '}';
    }
}
