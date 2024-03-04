package tech.ydb.query.impl;

import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QueryTx;

/**
 *
 * @author Aleksandr Gorshenin
 */
class TxNone implements QueryTx {
    public static final TxNone INSTANCE = new TxNone();

    @Override
    public YdbQuery.TransactionControl toTxControlPb() {
        return null;
    }
}
