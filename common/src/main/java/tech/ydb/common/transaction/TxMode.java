package tech.ydb.common.transaction;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum TxMode {
    NONE,

    SERIALIZABLE_RW,
    SNAPSHOT_RO,
    SNAPSHOT_RW,
    STALE_RO,

    ONLINE_RO,
    ONLINE_INCONSISTENT_RO
}
