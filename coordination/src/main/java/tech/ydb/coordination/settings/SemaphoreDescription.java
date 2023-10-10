package tech.ydb.coordination.settings;

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
    private final List<SemaphoreSession> ownersList;
    private final List<SemaphoreSession> waitersList;

    public SemaphoreDescription(tech.ydb.proto.coordination.SemaphoreDescription semaphoreDescription) {
    this.name = semaphoreDescription.getName();
        this.data = semaphoreDescription.getData().toByteArray();
        this.count = semaphoreDescription.getCount();
        this.limit = semaphoreDescription.getLimit();
        this.ephemeral = semaphoreDescription.getEphemeral();
        this.ownersList =
                semaphoreDescription.getOwnersList().stream().map(SemaphoreSession::new).collect(Collectors.toList());
        this.waitersList =
                semaphoreDescription.getWaitersList().stream().map(SemaphoreSession::new).collect(Collectors.toList());
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

    public List<SemaphoreSession> getOwnersList() {
        return ownersList;
    }

    public List<SemaphoreSession> getWaitersList() {
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
        return count == that.count && limit == that.limit && ephemeral == that.ephemeral &&
                Objects.equals(name, that.name) && Arrays.equals(data, that.data) &&
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
                ", data=" + Arrays.toString(data) +
                ", count=" + count +
                ", limit=" + limit +
                ", ephemeral=" + ephemeral +
                ", ownersList=" + ownersList +
                ", waitersList=" + waitersList +
                '}';
    }
}
