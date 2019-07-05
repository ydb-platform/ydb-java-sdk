package tech.ydb.table.transaction;


import tech.ydb.table.YdbTable.OnlineModeSettings;
import tech.ydb.table.YdbTable.SerializableModeSettings;
import tech.ydb.table.YdbTable.StaleModeSettings;
import tech.ydb.table.YdbTable.TransactionControl;
import tech.ydb.table.YdbTable.TransactionSettings;


/**
 * @author Sergey Polovko
 */
public abstract class TxControl<Self extends TxControl> {

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

    public static TxControl id(String id) {
        return new TxId(true, id);
    }

    public static TxControl id(Transaction tx) {
        return new TxId(true, tx.getId());
    }

    public static TxControl serializableRw() {
        return TxSerializableRw.WITH_COMMIT;
    }

    public static TxControl staleRo() {
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
    private static final class TxId extends TxControl<TxId> {
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
    private static final class TxSerializableRw extends TxControl<TxSerializableRw> {

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
    private static final class TxStaleRo extends TxControl<TxStaleRo> {

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
}
