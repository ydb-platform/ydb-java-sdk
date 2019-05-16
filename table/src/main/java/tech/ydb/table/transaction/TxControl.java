package tech.ydb.table.transaction;


import tech.ydb.table.YdbTable;


/**
 * @author Sergey Polovko
 */
public abstract class TxControl<Self extends TxControl> {

    private boolean commitTx;

    public static TxControl id(String id) {
        return new TxId(id);
    }

    public static TxControl id(Transaction tx) {
        return new TxId(tx.getId());
    }

    public static TxSerializableRw serializableRw() {
        return new TxSerializableRw();
    }

    public static TxOnlineRo onlineRo() {
        return new TxOnlineRo();
    }

    public static TxStaleRo staleRo() {
        return new TxStaleRo();
    }

    public boolean isCommitTx() {
        return commitTx;
    }

    @SuppressWarnings("unchecked")
    public Self setCommitTx(boolean commitTx) {
        this.commitTx = commitTx;
        return (Self)this;
    }

    public abstract YdbTable.TransactionControl toPb();

    /**
     * TX ID
     */
    public static final class TxId extends TxControl<TxId> {
        private final String id;

        private TxId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public YdbTable.TransactionControl toPb() {
            return YdbTable.TransactionControl.newBuilder()
                .setCommitTx(isCommitTx())
                .setTxId(id)
                .build();
        }
    }

    /**
     * TX SERIALIZABLE READ/WRITE
     */
    public static final class TxSerializableRw extends TxControl<TxSerializableRw> {
        private TxSerializableRw() {
        }

        @Override
        public YdbTable.TransactionControl toPb() {
            YdbTable.TransactionControl.Builder builder = YdbTable.TransactionControl.newBuilder()
                .setCommitTx(isCommitTx());
            builder.getBeginTxBuilder()
                .setSerializableReadWrite(YdbTable.SerializableModeSettings.getDefaultInstance());
            return builder.build();
        }
    }

    /**
     * TX STALE READ-ONLY
     */
    public static final class TxStaleRo extends TxControl<TxStaleRo> {
        private TxStaleRo() {
        }

        @Override
        public YdbTable.TransactionControl toPb() {
            YdbTable.TransactionControl.Builder builder = YdbTable.TransactionControl.newBuilder()
                .setCommitTx(isCommitTx());
            builder.getBeginTxBuilder()
                .setStaleReadOnly(YdbTable.StaleModeSettings.getDefaultInstance());
            return builder.build();
        }
    }

    /**
     * TX ONLINE READ ONLY
     */
    public static final class TxOnlineRo extends TxControl<TxOnlineRo> {

        private boolean allowInconsistentReads;

        private TxOnlineRo() {
        }

        public boolean allowInconsistentReads() {
            return allowInconsistentReads;
        }

        public TxOnlineRo setAllowInconsistentReads(boolean allowInconsistentReads) {
            this.allowInconsistentReads = allowInconsistentReads;
            return this;
        }

        @Override
        public YdbTable.TransactionControl toPb() {
            YdbTable.TransactionControl.Builder builder = YdbTable.TransactionControl.newBuilder()
                .setCommitTx(isCommitTx());
            builder.getBeginTxBuilder()
                .getOnlineReadOnlyBuilder()
                .setAllowInconsistentReads(allowInconsistentReads);
            return builder.build();
        }
    }
}
