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

    private final YdbTable.ReadTableResponse part;
    private final ResultSetReader resultSetReader;
    private final VirtualTimestamp timestamp;

    public ReadTablePart(YdbTable.ReadTableResponse part) {
        this.part = part;
        this.resultSetReader = ProtoValueReaders.forResultSet(part.getResult().getResultSet());
        CommonProtos.VirtualTimestamp vt = part.getSnapshot();
        this.timestamp = new VirtualTimestamp(vt.getPlanStep(), vt.getTxId());
    }

    public YdbTable.ReadTableResponse getReadTableResponse() {
        return part;
    }

    public ResultSetReader getResultSetReader() {
        return resultSetReader;
    }

    public VirtualTimestamp getVirtualTimestamp() {
        return timestamp;
    }
}

