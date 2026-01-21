package tech.ydb.table.impl;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.proto.table.YdbTable;

/**
 * @author Aleksandr Gorshenin
 * @author Nikolay Perfilov
 */
public class TxControlToPb {
    private static final YdbTable.TransactionSettings TS_SERIALIZABLE = YdbTable.TransactionSettings.newBuilder()
            .setSerializableReadWrite(YdbTable.SerializableModeSettings.getDefaultInstance())
            .build();

    private static final YdbTable.TransactionSettings TS_SNAPSHOT = YdbTable.TransactionSettings.newBuilder()
            .setSnapshotReadOnly(YdbTable.SnapshotModeSettings.getDefaultInstance())
            .build();

    private static final YdbTable.TransactionSettings TS_STALE = YdbTable.TransactionSettings.newBuilder()
            .setStaleReadOnly(YdbTable.StaleModeSettings.getDefaultInstance())
            .build();

    private static final YdbTable.TransactionSettings TS_ONLINE = YdbTable.TransactionSettings.newBuilder()
            .setOnlineReadOnly(YdbTable.OnlineModeSettings.newBuilder().setAllowInconsistentReads(false).build())
            .build();

    private static final YdbTable.TransactionSettings TS_ONLINE_INCONSISTENT = YdbTable.TransactionSettings
            .newBuilder()
            .setOnlineReadOnly(YdbTable.OnlineModeSettings.newBuilder().setAllowInconsistentReads(true).build())
            .build();

    private TxControlToPb() { }

    public static YdbTable.TransactionControl txModeCtrl(TxMode tx, boolean commitTx) {
        YdbTable.TransactionSettings ts = txSettings(tx);
        if (ts == null) {
            return null;
        }
        return YdbTable.TransactionControl.newBuilder()
                .setBeginTx(ts)
                .setCommitTx(commitTx)
                .build();
    }

    public static YdbTable.TransactionControl txIdCtrl(String txId, boolean commitTx) {
        return YdbTable.TransactionControl.newBuilder()
                .setTxId(txId)
                .setCommitTx(commitTx)
                .build();
    }

    public static YdbTable.TransactionSettings txSettings(TxMode tx) {
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
            case SNAPSHOT_RW:
            default:
                throw new IllegalArgumentException("Tx mode " + tx + " is not supported in TableService");
        }
    }
}
