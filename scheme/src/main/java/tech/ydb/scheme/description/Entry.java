package tech.ydb.scheme.description;

import java.util.Objects;

import tech.ydb.proto.scheme.SchemeOperationProtos;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class Entry {
    private final String name;
    private final String owner;
    private final EntryType type;
    private final long sizeBytes;

    public Entry(SchemeOperationProtos.Entry pb) {
        this.name = pb.getName();
        this.owner = pb.getOwner();
        this.type = EntryType.fromCode(pb.getTypeValue());
        this.sizeBytes = pb.getSizeBytes();
    }

    /**
     * @return Name of scheme entry (dir2 of /dir1/dir2)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return SID (Security ID) of user or group
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     * Size of entry in bytes. Currently filled for:
     * <ul>
     * <li> TABLE </li>
     * <li> DATABASE </li>
     * </ul>
     * Empty (zero) in other cases.
     *
     * @return Size of entry in bytes
     */
    public long getSizeBytes() {
        return this.sizeBytes;
    }

    public EntryType getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, owner, type, sizeBytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Entry.class) {
            return false;
        }
        Entry o = (Entry) obj;
        return Objects.equals(name, o.name)
                && Objects.equals(owner, o.owner)
                && type == o.type
                && sizeBytes == o.sizeBytes;
    }

    @Override
    public String toString() {
        return "Entry{name='" + name + "'"
                + (owner == null || owner.isEmpty() ? "" : ", owner='" + owner + "'")
                + ", type=" + type
                + (sizeBytes == 0 ? "" : ", size=" + Long.toUnsignedString(sizeBytes))
                + "}";
    }
}

