package tech.ydb.coordination.recipes.election;

import java.util.Arrays;
import java.util.Objects;

public class ElectionParticipant {
    private final long sessionId;
    private final byte[] data;
    private final boolean isLeader;

    public ElectionParticipant(long id, byte[] data, boolean isLeader) {
        this.sessionId = id;
        this.data = data;
        this.isLeader = isLeader;
    }

    public long getSessionId() {
        return sessionId;
    }

    public byte[] getData() {
        return data;
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
        ElectionParticipant that = (ElectionParticipant) o;
        return sessionId == that.sessionId && isLeader == that.isLeader &&
                Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, Arrays.hashCode(data), isLeader);
    }

    @Override
    public String toString() {
        return "ElectionParticipant{" +
                "sessionId=" + sessionId +
                ", data=" + Arrays.toString(data) +
                ", isLeader=" + isLeader +
                '}';
    }
}
