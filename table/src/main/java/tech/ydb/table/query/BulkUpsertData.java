package tech.ydb.table.query;


import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.values.ListValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BulkUpsertData {
    private final ValueProtos.TypedValue rows;

    public BulkUpsertData(ListValue rows) {
        this.rows = ValueProtos.TypedValue.newBuilder()
                .setType(rows.getType().toPb())
                .setValue(rows.toPb())
                .build();
    }

    public BulkUpsertData(ValueProtos.TypedValue rows) {
        this.rows = rows;
    }

    public void applyToRequest(YdbTable.BulkUpsertRequest.Builder builder) {
        builder.setRows(rows);
    }
}
