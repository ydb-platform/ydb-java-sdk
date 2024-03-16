package tech.ydb.query;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum QueryTx {
    NONE,

    SERIALIZABLE_RW,
    SNAPSHOT_RO,
    STALE_RO,

    ONLINE_RO,
    ONLINE_INCONSISNTENT_RO
}
