package tech.ydb.scheme.description;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum EntryType {
    UNSPECIFIED(0),

    DIRECTORY(1),
    TABLE(2),
    PERS_QUEUE_GROUP(3),
    DATABASE(4),
    RTMR_VOLUME(5),
    BLOCK_STORE_VOLUME(6),
    COORDINATION_NODE(7),
    COLUMN_STORE(12),
    COLUMN_TABLE(13),
    SEQUENCE(15),
    REPLICATION(16),
    TOPIC(17),
    EXTERNAL_TABLE(18),
    EXTERNAL_DATA_SOURCE(19),
    VIEW(20);

    private final int code;

    EntryType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EntryType fromCode(int code) {
        for (EntryType type: EntryType.values()) {
            if (code == type.code) {
                return type;
            }
        }
        return UNSPECIFIED;
    }
}
