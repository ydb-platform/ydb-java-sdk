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
    private ResultSetReader resultSetReader = null;
    private VirtualTimestamp timestamp = null;

    public ReadTablePart(YdbTable.ReadTableResponse part) {
        this.part = part;
    }

    public YdbTable.ReadTableResponse getReadTableResponse() {
        return part;
    }

    public ResultSetReader getResultSetReader() {
        if (resultSetReader == null) {
            resultSetReader = ProtoValueReaders.forResultSet(part.getResult().getResultSet());
        }
        return resultSetReader;
    }

    public VirtualTimestamp getVirtualTimestamp() {
        if (timestamp == null) {
            CommonProtos.VirtualTimestamp vt = part.getSnapshot();
            timestamp = new VirtualTimestamp(vt.getPlanStep(), vt.getTxId());
        }
        return timestamp;
    }
}

