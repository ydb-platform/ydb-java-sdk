package tech.ydb.table.query;

import com.google.protobuf.ByteString;

import tech.ydb.proto.formats.YdbFormats;
import tech.ydb.proto.table.YdbTable;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BulkUpsertCsvData implements BulkUpsertData {
    private final ByteString data;

    public BulkUpsertCsvData(ByteString data) {
        this.data = data;
    }

    @Override
    public void applyToRequest(YdbTable.BulkUpsertRequest.Builder builder) {
        builder.setCsvSettings(YdbFormats.CsvSettings.newBuilder().build()).setData(data);
    }
}
