package tech.ydb.table.query;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.values.ListValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BulkUpsertProtoData implements BulkUpsertData {
    private final ValueProtos.TypedValue rows;

    public BulkUpsertProtoData(ListValue rows) {
        this.rows = ValueProtos.TypedValue.newBuilder()
                .setType(rows.getType().toPb())
                .setValue(rows.toPb())
                .build();
    }

    public BulkUpsertProtoData(ValueProtos.TypedValue rows) {
        this.rows = rows;
    }

    @Override
    public void applyToRequest(YdbTable.BulkUpsertRequest.Builder builder) {
        builder.setRows(rows);
    }
}
