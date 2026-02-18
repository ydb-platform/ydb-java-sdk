package tech.ydb.table.query;

import com.google.protobuf.ByteString;

import tech.ydb.proto.formats.YdbFormats;
import tech.ydb.proto.table.YdbTable;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BulkUpsertArrowData implements BulkUpsertData {
    private final ByteString schema;
    private final ByteString data;

    public BulkUpsertArrowData(ByteString schema, ByteString data) {
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
