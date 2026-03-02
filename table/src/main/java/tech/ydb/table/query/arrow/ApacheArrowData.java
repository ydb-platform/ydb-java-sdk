package tech.ydb.table.query.arrow;

import com.google.protobuf.ByteString;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.formats.YdbFormats;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.query.BulkUpsertData;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ApacheArrowData extends BulkUpsertData {
    private final ByteString schema;
    private final ByteString data;

    public ApacheArrowData(ByteString schema, ByteString data) {
        super((ValueProtos.TypedValue) null);
        this.schema = schema;
        this.data = data;
    }

    public ByteString getSchema() {
        return schema;
    }

    public ByteString getData() {
        return data;
    }

    @Override
    public void applyToRequest(YdbTable.BulkUpsertRequest.Builder builder) {
        builder.setArrowBatchSettings(
                YdbFormats.ArrowBatchSettings.newBuilder().setSchema(schema).build()
        ).setData(data);
    }
}
