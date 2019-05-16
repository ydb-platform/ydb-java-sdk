package tech.ydb.table.transaction;

/**
 * @author Sergey Polovko
 */
public enum TransactionMode {
    SERIALIZABLE_READ_WRITE,
    ONLINE_READ_ONLY,
    STALE_READ_ONLY,
    ;
}
