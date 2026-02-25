package tech.ydb.query.result.arrow;

import java.util.List;

import io.grpc.ExperimentalApi;
import org.apache.arrow.vector.VectorSchemaRoot;

import tech.ydb.proto.ValueProtos;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.Type;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("ApacheArrow support is experimental and API may change without notice")
public class ArrowQueryResultPart extends QueryResultPart  {
    private final ArrowResultSetReader resultSetReader;

    public ArrowQueryResultPart(long index, VectorSchemaRoot vsr, List<ValueProtos.Column> columns, boolean truncated) {
        super(index, null);

        ArrowValueReader<?>[] readers = new ArrowValueReader<?>[columns.size()];
        for (int idx = 0; idx < columns.size(); idx += 1) {
            ValueProtos.Column column = columns.get(idx);
            Type type = validateType(column.getType());
            boolean optional = column.getType().hasOptionalType();
            readers[idx] = ArrowValueReader.createReader(vsr.getVector(column.getName()), type, optional);
        }

        this.resultSetReader = new ArrowResultSetReader(vsr, readers, truncated);
    }

    @Override
    public ResultSetReader getResultSetReader() {
        return resultSetReader;
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
