package tech.ydb.query.impl;

import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QueryTx;

/**
 *
 * @author Aleksandr Gorshenin
 */
class TxId implements QueryTx.Id {
    private final String id;
    private final boolean commitTx;

    TxId(String id, boolean commitTx) {
        this.id = id;
        this.commitTx = commitTx;
    }

    TxId(YdbQuery.TransactionMeta meta) {
        this(meta.getId(), false);
    }

    TxId(YdbQuery.BeginTransactionResponse resp) {
        this(resp.getTxMeta());
    }

    @Override
    public YdbQuery.TransactionControl toTxControlPb() {
        return YdbQuery.TransactionControl.newBuilder()
                .setTxId(id)
                .setCommitTx(commitTx)
                .build();
    }

    @Override
    public String txId() {
        return id;
    }

    @Override
    public boolean isCommitTx() {
        return commitTx;
    }

    @Override
    public Id withCommitTx() {
        return new TxId(id, true);
    }
}
