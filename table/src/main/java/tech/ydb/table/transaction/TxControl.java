package tech.ydb.table.transaction;


import tech.ydb.common.transaction.TxMode;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.proto.table.YdbTable.OnlineModeSettings;
import tech.ydb.proto.table.YdbTable.SerializableModeSettings;
import tech.ydb.proto.table.YdbTable.StaleModeSettings;
import tech.ydb.proto.table.YdbTable.TransactionControl;
import tech.ydb.proto.table.YdbTable.TransactionSettings;
import tech.ydb.table.settings.BeginTxSettings;


/**
 * @author Sergey Polovko
 * @param <Self> self type
 */
public abstract class TxControl<Self extends TxControl<?>> {

    private final TransactionControl pb;

    protected TxControl(boolean commitTx, String id) {
        this.pb = TransactionControl.newBuilder()
            .setCommitTx(commitTx)
            .setTxId(id)
            .build();
    }

    protected TxControl(boolean commitTx, TransactionSettings settings) {
        this.pb = TransactionControl.newBuilder()
            .setCommitTx(commitTx)
            .setBeginTx(settings)
            .build();
    }

    /**
     * @deprecated
     * Use {@link TableTransaction} created by {@link tech.ydb.table.Session#createNewTransaction(TxMode)}
     * or {@link tech.ydb.table.Session#beginTransaction(TxMode, BeginTxSettings)} to execute queries in transaction
     */
    @Deprecated
    public static TxId id(String id) {
        return new TxId(true, id);
    }

    /**
     * @deprecated
     * Use {@link TableTransaction} created by {@link tech.ydb.table.Session#createNewTransaction(TxMode)}
     * or {@link tech.ydb.table.Session#beginTransaction(TxMode, BeginTxSettings)} to execute queries in transaction
     */
    @Deprecated
    public static TxId id(Transaction tx) {
        return new TxId(true, tx.getId());
    }

    public static TxSerializableRw serializableRw() {
        return TxSerializableRw.WITH_COMMIT;
    }

    public static TxSnapshotRo snapshotRo() {
        return TxSnapshotRo.WITH_COMMIT;
    }

    public static TxStaleRo staleRo() {
        return TxStaleRo.WITH_COMMIT;
    }

    public static TxOnlineRo onlineRo() {
        return TxOnlineRo.WITH_COMMIT;
    }

    public boolean isCommitTx() {
        return pb.getCommitTx();
    }

    public abstract Self setCommitTx(boolean commitTx);
    public TransactionControl toPb() {
        return pb;
    }

    /**
     * TX ID
     */
    public static final class TxId extends TxControl<TxId> {
        private final String id;

        TxId(boolean commitTx, String id) {
            super(commitTx, id);
            this.id = id;
        }

        @Override
        public TxId setCommitTx(boolean commitTx) {
            return commitTx == isCommitTx() ? this : new TxId(commitTx, id);
        }
    }

    /**
     * TX SERIALIZABLE READ/WRITE
     */
    public static final class TxSerializableRw extends TxControl<TxSerializableRw> {

        private static final TxSerializableRw WITH_COMMIT = new TxSerializableRw(true);
        private static final TxSerializableRw WITHOUT_COMMIT = new TxSerializableRw(false);

        TxSerializableRw(boolean commitTx) {
            super(commitTx, TransactionSettings.newBuilder()
                .setSerializableReadWrite(SerializableModeSettings.getDefaultInstance())
                .build());
        }

        @Override
        public TxSerializableRw setCommitTx(boolean commitTx) {
            return commitTx ? WITH_COMMIT : WITHOUT_COMMIT;
        }
    }

    /**
     * TX STALE READ-ONLY
     */
    public static final class TxStaleRo extends TxControl<TxStaleRo> {

        private static final TxStaleRo WITH_COMMIT = new TxStaleRo(true);
        private static final TxStaleRo WITHOUT_COMMIT = new TxStaleRo(false);

        TxStaleRo(boolean commitTx) {
            super(commitTx, TransactionSettings.newBuilder()
                .setStaleReadOnly(StaleModeSettings.getDefaultInstance())
                .build());
        }

        @Override
        public TxStaleRo setCommitTx(boolean commitTx) {
            return commitTx ? WITH_COMMIT : WITHOUT_COMMIT;
        }
    }

    /**
     * TX ONLINE READ ONLY
     */
    public static final class TxOnlineRo extends TxControl<TxOnlineRo> {

        private static final TxOnlineRo WITH_COMMIT = new TxOnlineRo(true, false);
        private static final TxOnlineRo WITHOUT_COMMIT = new TxOnlineRo(false, false);

        private final boolean allowInconsistentReads;

        TxOnlineRo(boolean commitTx, boolean allowInconsistentReads) {
            super(commitTx, TransactionSettings.newBuilder()
                .setOnlineReadOnly(OnlineModeSettings.newBuilder()
                    .setAllowInconsistentReads(allowInconsistentReads))
                .build());
            this.allowInconsistentReads = allowInconsistentReads;
        }

        public boolean isAllowInconsistentReads() {
            return allowInconsistentReads;
        }

        public TxOnlineRo setAllowInconsistentReads(boolean allowInconsistentReads) {
            if (allowInconsistentReads == isAllowInconsistentReads()) {
                return this;
            }
            return new TxOnlineRo(isCommitTx(), allowInconsistentReads);
        }

        @Override
        public TxOnlineRo setCommitTx(boolean commitTx) {
            if (commitTx == isCommitTx()) {
                return this;
            }

            if (allowInconsistentReads) {
                return new TxOnlineRo(commitTx, true);
            }

            return commitTx ? WITH_COMMIT : WITHOUT_COMMIT;
        }
    }

    /**
     * TX SNAPSHOT READ ONLY
     */
    public static final class TxSnapshotRo extends TxControl<TxSnapshotRo> {

        private static final TxSnapshotRo WITH_COMMIT = new TxSnapshotRo(true);
        private static final TxSnapshotRo WITHOUT_COMMIT = new TxSnapshotRo(false);

        TxSnapshotRo(boolean commitTx) {
            super(commitTx, TransactionSettings.newBuilder()
                .setSnapshotReadOnly(YdbTable.SnapshotModeSettings
                        .newBuilder().build()
                ).build()
            );
        }

        @Override
        public TxSnapshotRo setCommitTx(boolean commitTx) {
            if (commitTx == isCommitTx()) {
                return this;
            }

            return commitTx ? WITH_COMMIT : WITHOUT_COMMIT;
        }
    }
}
