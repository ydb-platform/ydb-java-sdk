package tech.ydb.table.query;

import tech.ydb.proto.common.CommonProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 * @author Ilya Udalov
 */
public class ReadTablePart {
    public static final class VirtualTimestamp {
        private final long planStep;
        private final long txId;

        public VirtualTimestamp(long planStep, long txId) {
            this.planStep = planStep;
            this.txId = txId;
        }

        public long getPlanStep() {
            return planStep;
        }

        public long getTxId() {
            return txId;
        }
    }

    private final ResultSetReader resultSetReader;
    private final VirtualTimestamp timestamp;

    public ReadTablePart(YdbTable.ReadTableResult result, CommonProtos.VirtualTimestamp snapshot) {
        this.resultSetReader = ProtoValueReaders.forResultSet(result.getResultSet());
        this.timestamp = new VirtualTimestamp(snapshot.getPlanStep(), snapshot.getTxId());
    }

    public ResultSetReader getResultSetReader() {
        return resultSetReader;
    }

    public VirtualTimestamp getVirtualTimestamp() {
        return timestamp;
    }
}

