package tech.ydb.coordination.recipes.watch;

import java.util.Arrays;
import java.util.Objects;

public class Participant {
    private final long id;
    private final byte[] data;
    private final long count;
    private final boolean isLeader;

    public Participant(long id, byte[] data, long count, boolean isLeader) {
        this.id = id;
        this.data = data;
        this.count = count;
        this.isLeader = isLeader;
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public long getCount() {
        return count;
    }

    public boolean isLeader() {
        return isLeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Participant that = (Participant) o;
        return id == that.id && count == that.count && isLeader == that.isLeader && Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, Arrays.hashCode(data), count, isLeader);
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + id +
                ", data=" + Arrays.toString(data) +
                ", count=" + count +
                ", isLeader=" + isLeader +
                '}';
    }
}
