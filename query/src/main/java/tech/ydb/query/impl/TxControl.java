package tech.ydb.query.impl;

import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QueryTx;

/**
 *
 * @author Aleksandr Gorshenin
 */
class TxControl {
    private static final YdbQuery.TransactionSettings TS_SERIALIZABLE = YdbQuery.TransactionSettings.newBuilder()
            .setSerializableReadWrite(YdbQuery.SerializableModeSettings.getDefaultInstance())
            .build();

    private static final YdbQuery.TransactionSettings TS_SNAPSHOT = YdbQuery.TransactionSettings.newBuilder()
            .setSnapshotReadOnly(YdbQuery.SnapshotModeSettings.getDefaultInstance())
            .build();

    private static final YdbQuery.TransactionSettings TS_STALE = YdbQuery.TransactionSettings.newBuilder()
            .setStaleReadOnly(YdbQuery.StaleModeSettings.getDefaultInstance())
            .build();

    private static final YdbQuery.TransactionSettings TS_ONLINE = YdbQuery.TransactionSettings.newBuilder()
            .setOnlineReadOnly(YdbQuery.OnlineModeSettings.newBuilder().setAllowInconsistentReads(false).build())
            .build();

    private static final YdbQuery.TransactionSettings TS_ONLINE_INCONSISTENT = YdbQuery.TransactionSettings
            .newBuilder()
            .setOnlineReadOnly(YdbQuery.OnlineModeSettings.newBuilder().setAllowInconsistentReads(true).build())
            .build();

    private TxControl() { }

    public static YdbQuery.TransactionControl txModeCtrl(QueryTx tx, boolean commitTx) {
        YdbQuery.TransactionSettings ts = txSettings(tx);
        if (ts == null) {
            return null;
        }
        return YdbQuery.TransactionControl.newBuilder()
                .setBeginTx(ts)
                .setCommitTx(commitTx)
                .build();
    }

    public static YdbQuery.TransactionControl txIdCtrl(String txId, boolean commitTx) {
        return YdbQuery.TransactionControl.newBuilder()
                .setTxId(txId)
                .setCommitTx(commitTx)
                .build();
    }

    public static YdbQuery.TransactionSettings txSettings(QueryTx tx) {
        switch (tx) {
            case SERIALIZABLE_RW:
                return TS_SERIALIZABLE;
            case SNAPSHOT_RO:
                return TS_SNAPSHOT;
            case STALE_RO:
                return TS_STALE;
            case ONLINE_RO:
                return TS_ONLINE;
            case ONLINE_INCONSISTENT_RO:
                return TS_ONLINE_INCONSISTENT;
            case NONE:
            default:
                return null;
        }
    }

}
