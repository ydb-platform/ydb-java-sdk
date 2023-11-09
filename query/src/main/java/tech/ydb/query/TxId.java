package tech.ydb.query;

import tech.ydb.proto.query.YdbQuery;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TxId implements QuerySession.Tx {
    private final String id;
    private final boolean commitTx;

    private TxId(String id, boolean commitTx) {
        this.id = id;
        this.commitTx = commitTx;
    }

    @Override
    public YdbQuery.TransactionControl toPb() {
        return YdbQuery.TransactionControl.newBuilder()
                .setTxId(id)
                .setCommitTx(commitTx)
                .build();
    }

    public String txID() {
        return id;
    }

    public boolean isCommitTx() {
        return commitTx;
    }

    public static TxId id(String id, boolean commitTx) {
        return new TxId(id, commitTx);
    }

    public static TxId id(String id) {
        return new TxId(id, false);
    }
}
