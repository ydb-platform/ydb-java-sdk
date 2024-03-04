package tech.ydb.query.impl;

import tech.ydb.proto.query.YdbQuery;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TxOnlineRo extends TxMode {
    TxOnlineRo(boolean commitTx, boolean allowInconsistentReads) {
        super(YdbQuery.TransactionSettings.newBuilder()
                .setOnlineReadOnly(YdbQuery.OnlineModeSettings.newBuilder()
                        .setAllowInconsistentReads(allowInconsistentReads)
                        .build())
                .build(), commitTx);
    }

    public boolean isAllowInconsistentReads() {
        return toTxSettingsPb().getOnlineReadOnly().getAllowInconsistentReads();
    }

    public TxOnlineRo setAllowInconsistentReads(boolean allow) {
        return new TxOnlineRo(isCommitTx(), allow);
    }

    @Override
    public Mode setCommitTx(boolean commitTx) {
        return new TxOnlineRo(commitTx, isAllowInconsistentReads());
    }
}
