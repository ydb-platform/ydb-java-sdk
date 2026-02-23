package tech.ydb.query.result.array;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import com.google.protobuf.ByteString;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ReadChannel;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.ipc.message.MessageSerializer;
import org.apache.arrow.vector.types.pojo.Schema;

import tech.ydb.proto.ValueProtos;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.Type;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class ArrayPartsHandler implements QueryStream.PartsHandler {
    private final RootAllocator allocator;

    public ArrayPartsHandler(RootAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public void onNextRawPart(long index, ValueProtos.ResultSet rs) {
        if (!rs.hasArrowFormatMeta()) {
            // use the standard behaviour
            onNextPart(new QueryResultPart(index, rs));
            return;
        }

        try {
            Schema schema = readApacheArrowSchema(rs.getArrowFormatMeta().getSchema());
            try (VectorSchemaRoot vsr = VectorSchemaRoot.create(schema, allocator)) {
                try (InputStream is = rs.getData().newInput()) {
                    try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
                        try (ArrowRecordBatch batch = MessageSerializer.deserializeRecordBatch(channel, allocator)) {
                            VectorLoader loader = new VectorLoader(vsr);
                            loader.load(batch);
                        }
                    }
                }

                ArrayValueReader<?>[] readers = new ArrayValueReader<?>[rs.getColumnsCount()];
                for (int idx = 0; idx < rs.getColumnsCount(); idx += 1) {
                    ValueProtos.Column column = rs.getColumns(idx);
                    String name = column.getName();
                    Type type = validateType(column.getType());
                    boolean optional = column.getType().hasOptionalType();
                    readers[idx] = ArrayValueReader.createReader(vsr.getVector(name), type, optional);
                }

                onNextPart(new ArrayQueryResultPart(index, new ArrayResultSetReader(vsr, readers, rs.getTruncated())));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read ApacheArrow vector", ex);
        }
    }

    private static Schema readApacheArrowSchema(ByteString bytes) throws IOException {
        try (InputStream is = bytes.newInput()) {
            try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
                return MessageSerializer.deserializeSchema(channel);
            }
        }
    }

    private static Type validateType(ValueProtos.Type type) {
        if (type.hasOptionalType()) {
            return validatePrimitiveType(type.getOptionalType().getItem());
        }
        return validatePrimitiveType(type);
    }

    private static Type validatePrimitiveType(ValueProtos.Type type) {
        if (type.hasTypeId()) {
            switch (type.getTypeId()) {
                case BOOL: return PrimitiveType.Bool;

                case INT8: return PrimitiveType.Int8;
                case INT16: return PrimitiveType.Int16;
                case INT32: return PrimitiveType.Int32;
                case INT64: return PrimitiveType.Int64;

                case UINT8: return PrimitiveType.Uint8;
                case UINT16: return PrimitiveType.Uint16;
                case UINT32: return PrimitiveType.Uint32;
                case UINT64: return PrimitiveType.Uint64;

                case FLOAT: return PrimitiveType.Float;
                case DOUBLE: return PrimitiveType.Double;

                case UTF8: return PrimitiveType.Text;
                case JSON: return PrimitiveType.Json;
                case JSON_DOCUMENT: return PrimitiveType.JsonDocument;

                case STRING: return PrimitiveType.Bytes;
                case YSON: return PrimitiveType.Yson;

                case UUID: return PrimitiveType.Uuid;

                case DATE: return PrimitiveType.Date;
                case DATETIME: return PrimitiveType.Datetime;
                case TIMESTAMP: return PrimitiveType.Timestamp;
                case INTERVAL: return PrimitiveType.Interval;

                case DATE32: return PrimitiveType.Date32;
                case DATETIME64: return PrimitiveType.Datetime64;
                case TIMESTAMP64: return PrimitiveType.Timestamp64;
                case INTERVAL64: return PrimitiveType.Interval64;
                default:
                    break;
            }
        }
        if (type.hasDecimalType()) {
            return DecimalType.of(type.getDecimalType().getPrecision(), type.getDecimalType().getScale());
        }

        throw new IllegalStateException("Unsupported type for ApacheArrow reader: " + type);
    }
}
