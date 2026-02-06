package tech.ydb.coordination.recipes.group;

import java.util.Arrays;
import java.util.Objects;

public class GroupMember {
    private final long sessionId;
    private final byte[] data;

    public GroupMember(long sessionId, byte[] data) {
        this.sessionId = sessionId;
        this.data = data;
    }

    public long getSessionId() {
        return sessionId;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupMember that = (GroupMember) o;
        return sessionId == that.sessionId && Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, Arrays.hashCode(data));
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "sessionId=" + sessionId +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
