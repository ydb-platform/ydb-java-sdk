package tech.ydb.table.query;


import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.values.ListValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface BulkUpsertData {
    void applyToRequest(YdbTable.BulkUpsertRequest.Builder builder);

    static BulkUpsertData fromRows(ListValue list) {
        return new BulkUpsertProtoData(list);
    }

    static BulkUpsertData fromProto(ValueProtos.TypedValue rows) {
        return new BulkUpsertProtoData(rows);
    }
}
