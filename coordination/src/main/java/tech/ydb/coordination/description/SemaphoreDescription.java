package tech.ydb.coordination.description;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class SemaphoreDescription {
    private final String name;
    private final byte[] data;
    private final long count;
    private final long limit;
    private final boolean ephemeral;
    private final List<Session> ownersList;
    private final List<Session> waitersList;

    public SemaphoreDescription(tech.ydb.proto.coordination.SemaphoreDescription description) {
        this.name = description.getName();
        this.data = description.getData().toByteArray();
        this.count = description.getCount();
        this.limit = description.getLimit();
        this.ephemeral = description.getEphemeral();
        this.ownersList = description.getOwnersList().stream().map(Session::new).collect(Collectors.toList());
        this.waitersList = description.getWaitersList().stream().map(Session::new).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public long getCount() {
        return count;
    }

    public long getLimit() {
        return limit;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public List<Session> getOwnersList() {
        return ownersList;
    }

    public List<Session> getWaitersList() {
        return waitersList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SemaphoreDescription)) {
            return false;
        }
        SemaphoreDescription that = (SemaphoreDescription) o;
        return count == that.count &&
                limit == that.limit &&
                ephemeral == that.ephemeral &&
                Objects.equals(name, that.name) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(ownersList, that.ownersList) &&
                Objects.equals(waitersList, that.waitersList);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, count, limit, ephemeral, ownersList, waitersList);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "SemaphoreDescription{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", limit=" + limit +
                ", ephemeral=" + ephemeral +
                ", ownersList=" + ownersList +
                ", waitersList=" + waitersList +
                '}';
    }

    public static class Session {
        private final long id;
        private final long timeoutMillis;
        private final long count;
        private final byte[] data;
        private final long orderId;

        public Session(tech.ydb.proto.coordination.SemaphoreSession semaphoreSession) {
            this.id = semaphoreSession.getSessionId();
            this.timeoutMillis = semaphoreSession.getTimeoutMillis();
            this.count = semaphoreSession.getCount();
            this.data = Arrays.copyOf(semaphoreSession.getData().toByteArray(), semaphoreSession.getData().size());
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
            if (!(o instanceof Session)) {
                return false;
            }
            Session that = (Session) o;
            return id == that.id &&
                    timeoutMillis == that.timeoutMillis &&
                    count == that.count &&
                    orderId == that.orderId &&
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
            return "Session{" +
                    "id=" + id +
                    ", count=" + count +
                    ", data=" + new String(data, StandardCharsets.UTF_8) +
                    ", orderId=" + orderId +
                    ", timeoutMillis=" + timeoutMillis +
                    '}';
        }
    }
}
