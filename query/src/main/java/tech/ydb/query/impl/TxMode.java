package tech.ydb.query.impl;

import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QueryTx;

/**
 *
 * @author Aleksandr Gorshenin
 */
abstract class TxMode implements QueryTx.Mode {
    private final YdbQuery.TransactionSettings txMode;
    private final boolean commitTx;

    TxMode(YdbQuery.TransactionSettings txMode, boolean commitTx) {
        this.txMode = txMode;
        this.commitTx = commitTx;
    }

    @Override
    public YdbQuery.TransactionControl toTxControlPb() {
        return YdbQuery.TransactionControl.newBuilder()
                .setBeginTx(txMode)
                .setCommitTx(commitTx)
                .build();
    }

    @Override
    public YdbQuery.TransactionSettings toTxSettingsPb() {
        return txMode;
    }

    @Override
    public boolean isCommitTx() {
        return commitTx;
    }

    static class TxSerializableRw extends TxMode {
        private static final YdbQuery.TransactionSettings MODE = YdbQuery.TransactionSettings.newBuilder()
                .setSerializableReadWrite(YdbQuery.SerializableModeSettings.getDefaultInstance())
                .build();

        static final TxSerializableRw COMMIT_ON = new TxSerializableRw(true);
        static final TxSerializableRw COMMIT_OFF = new TxSerializableRw(false);

        private TxSerializableRw(boolean commitTx) {
            super(MODE, commitTx);
        }

        @Override
        public QueryTx.Mode setCommitTx(boolean commitTx) {
            return commitTx ? COMMIT_ON : COMMIT_OFF;
        }
    }

    static class TxSnapshotRo extends TxMode {
        private static final YdbQuery.TransactionSettings MODE = YdbQuery.TransactionSettings.newBuilder()
                .setSnapshotReadOnly(YdbQuery.SnapshotModeSettings.getDefaultInstance())
                .build();

        static final TxSnapshotRo COMMIT_ON = new TxSnapshotRo(true);
        static final TxSnapshotRo COMMIT_OFF = new TxSnapshotRo(false);

        private TxSnapshotRo(boolean commitTx) {
            super(MODE, commitTx);
        }

        @Override
        public QueryTx.Mode setCommitTx(boolean commitTx) {
            return commitTx ? COMMIT_ON : COMMIT_OFF;
        }
    }

    static class TxStaleRo extends TxMode {
        private static final YdbQuery.TransactionSettings MODE = YdbQuery.TransactionSettings.newBuilder()
                .setStaleReadOnly(YdbQuery.StaleModeSettings.getDefaultInstance())
                .build();

        static final TxStaleRo COMMIT_ON = new TxStaleRo(true);
        static final TxStaleRo COMMIT_OFF = new TxStaleRo(false);

        private TxStaleRo(boolean commitTx) {
            super(MODE, commitTx);
        }

        @Override
        public TxMode setCommitTx(boolean commitTx) {
            return commitTx ? COMMIT_ON : COMMIT_OFF;
        }
    }
}
