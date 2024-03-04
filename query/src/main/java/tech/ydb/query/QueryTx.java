package tech.ydb.query;

import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.impl.TxImpl;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueryTx {
    interface Mode extends QueryTx {
        YdbQuery.TransactionSettings toTxSettingsPb();

        boolean isCommitTx();

        Mode setCommitTx(boolean commitTx);

        default Mode withCommitTx() {
            return setCommitTx(true);
        }

        default Mode withoutCommitTx() {
            return setCommitTx(false);
        }
    }

    interface Id extends QueryTx {
        String txId();

        boolean isCommitTx();

        Id withCommitTx();
    }

    YdbQuery.TransactionControl toTxControlPb();

    static QueryTx noTx() {
        return TxImpl.NONE;
    }

    static Mode serializableRw() {
        return TxImpl.SERIALIZABLE_RW;
    }

    static Mode snapshotRo() {
        return TxImpl.SNAPSHOT_RO;
    }

    static Mode staleRo() {
        return TxImpl.STALE_RO;
    }
}
