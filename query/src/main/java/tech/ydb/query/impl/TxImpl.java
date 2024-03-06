package tech.ydb.query.impl;

import tech.ydb.query.QueryTx;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TxImpl {
    public static final QueryTx NONE = TxNone.INSTANCE;

    public static final QueryTx.Mode SERIALIZABLE_RW = TxMode.TxSerializableRw.COMMIT_ON;
    public static final QueryTx.Mode SNAPSHOT_RO = TxMode.TxSnapshotRo.COMMIT_ON;
    public static final QueryTx.Mode STALE_RO = TxMode.TxStaleRo.COMMIT_ON;

    private TxImpl() { }

    public static TxOnlineRo onlineRo() {
        return new TxOnlineRo(true, false);
    }

    public static QueryTx.Id id(String id) {
        return new TxId(id, false);
    }
}
