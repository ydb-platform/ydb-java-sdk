package tech.ydb.table.query;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.google.protobuf.ByteString;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.BaseFixedWidthVector;
import org.apache.arrow.vector.BaseVariableWidthVector;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.FixedSizeBinaryVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.VarBinaryVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.WriteChannel;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.ipc.message.MessageSerializer;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;

import tech.ydb.table.utils.LittleEndian;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.proto.ProtoValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ApacheArrowWriter implements AutoCloseable {
    public interface Batch {
        Row writeNextRow();
        BulkUpsertArrowData buildBatch() throws IOException;
    }

    public interface Row {
        void writeNull(String column);

        void writeBool(String column, boolean value);

        void writeInt8(String column, byte value);
        void writeInt16(String column, short value);
        void writeInt32(String column, int value);
        void writeInt64(String column, long value);

        void writeUint8(String column, int value);
        void writeUint16(String column, int value);
        void writeUint32(String column, long value);
        void writeUint64(String column, long value);

        void writeFloat(String column, float value);
        void writeDouble(String column, double value);

        void writeText(String column, String text);
        void writeJson(String column, String json);
        void writeJsonDocument(String column, String jsonDocument);

        void writeBytes(String column, byte[] bytes);
        void writeYson(String column, byte[] yson);

        void writeUuid(String column, UUID uuid);

        void writeDate(String column, LocalDate date);
        void writeDatetime(String column, LocalDateTime datetime);
        void writeTimestamp(String column, Instant instant);
        void writeInterval(String column, Duration interval);

        void writeDate32(String column, LocalDate date32);
        void writeDatetime64(String column, LocalDateTime datetime64);
        void writeTimestamp64(String column, Instant instant64);
        void writeInterval64(String column, Duration interval64);

        void writeDecimal(String column, DecimalValue value);
    }

    private final VectorSchemaRoot vsr;
    private final Map<String, Column<?>> columns = new HashMap<>();

    private ApacheArrowWriter(BufferAllocator allocator, List<ColumnInfo> columnsList) {
        FieldVector[] vectors = new FieldVector[columnsList.size()];
        for (int idx = 0; idx < columnsList.size(); idx += 1) {
            ColumnInfo column = columnsList.get(idx);
            Column<?> vector = column.createVector(allocator);
            vectors[idx] = vector.vector;
            columns.put(column.name, vector);
        }

        this.vsr = VectorSchemaRoot.of(vectors);
    }

    public Batch createNewBatch(int estimatedRowsCount) {
        // reset all
        for (Column<?> column: columns.values()) {
            column.allocateNew(estimatedRowsCount);
        }
        return new BatchImpl();
    }

    @Override
    public void close() {
        vsr.close();

        for (FieldVector field: vsr.getFieldVectors()) {
            field.close();
        }
    }

    private class BatchImpl implements Batch {
        private int rowIndex = 0;

        @Override
        public Row writeNextRow() {
            return new RowImpl(rowIndex++);
        }

        @Override
        public BulkUpsertArrowData buildBatch() throws IOException {
            vsr.setRowCount(rowIndex);
            return new BulkUpsertArrowData(serializeSchema(), serializeBatch());
        }

        private ByteString serializeSchema() throws IOException {
            try (ByteString.Output out = ByteString.newOutput()) {
                try (WriteChannel channel = new WriteChannel(Channels.newChannel(out))) {
                    MessageSerializer.serialize(channel, vsr.getSchema());
                    return out.toByteString();
                }
            }
        }

        private ByteString serializeBatch() throws IOException {
            try (ByteString.Output out = ByteString.newOutput()) {
                try (WriteChannel channel = new WriteChannel(Channels.newChannel(out))) {
                    VectorUnloader loader = new VectorUnloader(vsr);
                    try (ArrowRecordBatch batch = loader.getRecordBatch()) {
                        MessageSerializer.serialize(channel, batch);
                        return out.toByteString();
                    }
                }
            }
        }
    }

    private class RowImpl implements Row {
        private final int rowIndex;

        RowImpl(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        private Column<?> find(String column) {
            Column<?> vector = columns.get(column);
            if (vector == null) {
                throw new IllegalArgumentException("Column '" + column + "' not found");
            }
            return vector;
        }

        @Override
        public void writeNull(String column) {
            find(column).writeNull(rowIndex);
        }

        @Override
        public void writeBool(String column, boolean value) {
            find(column).writeBool(rowIndex, value);
        }

        @Override
        public void writeInt8(String column, byte value) {
            find(column).writeInt8(rowIndex, value);
        }

        @Override
        public void writeInt16(String column, short value) {
            find(column).writeInt16(rowIndex, value);
        }

        @Override
        public void writeInt32(String column, int value) {
            find(column).writeInt32(rowIndex, value);
        }

        @Override
        public void writeInt64(String column, long value) {
            find(column).writeInt64(rowIndex, value);
        }

        @Override
        public void writeUint8(String column, int value) {
            find(column).writeUint8(rowIndex, value);
        }

        @Override
        public void writeUint16(String column, int value) {
            find(column).writeUint16(rowIndex, value);
        }

        @Override
        public void writeUint32(String column, long value) {
            find(column).writeUint32(rowIndex, value);
        }

        @Override
        public void writeUint64(String column, long value) {
            find(column).writeUint64(rowIndex, value);
        }

        @Override
        public void writeFloat(String column, float value) {
            find(column).writeFloat(rowIndex, value);
        }

        @Override
        public void writeDouble(String column, double value) {
            find(column).writeDouble(rowIndex, value);
        }

        @Override
        public void writeText(String column, String text) {
            find(column).writeText(rowIndex, text);
        }

        @Override
        public void writeJson(String column, String json) {
            find(column).writeJson(rowIndex, json);
        }

        @Override
        public void writeJsonDocument(String column, String jsonDocument) {
            find(column).writeJsonDocument(rowIndex, jsonDocument);
        }

        @Override
        public void writeBytes(String column, byte[] bytes) {
            find(column).writeBytes(rowIndex, bytes);
        }

        @Override
        public void writeYson(String column, byte[] yson) {
            find(column).writeYson(rowIndex, yson);
        }

        @Override
        public void writeUuid(String column, UUID uuid) {
            find(column).writeUuid(rowIndex, uuid);
        }

        @Override
        public void writeDate(String column, LocalDate date) {
            find(column).writeDate(rowIndex, date);
        }

        @Override
        public void writeDatetime(String column, LocalDateTime datetime) {
            find(column).writeDatetime(rowIndex, datetime);
        }

        @Override
        public void writeTimestamp(String column, Instant instant) {
            find(column).writeTimestamp(rowIndex, instant);
        }

        @Override
        public void writeInterval(String column, Duration interval) {
            find(column).writeInterval(rowIndex, interval);
        }

        @Override
        public void writeDate32(String column, LocalDate date32) {
            find(column).writeDate32(rowIndex, date32);
        }

        @Override
        public void writeDatetime64(String column, LocalDateTime datetime64) {
            find(column).writeDatetime64(rowIndex, datetime64);
        }

        @Override
        public void writeTimestamp64(String column, Instant instant64) {
            find(column).writeTimestamp64(rowIndex, instant64);
        }

        @Override
        public void writeInterval64(String column, Duration interval64) {
            find(column).writeInterval64(rowIndex, interval64);
        }

        @Override
        public void writeDecimal(String column, DecimalValue value) {
            find(column).writeDecimal(rowIndex, value);
        }
    }

    private abstract static class Column<T extends FieldVector> {
        protected final Field field;
        protected final Type type;
        protected final T vector;

        Column(Field field, Type type, T vector) {
            this.field = field;
            this.type = type;
            this.vector = vector;
        }

        protected IllegalStateException error(String method) {
            return new IllegalStateException("cannot call " + method + ", actual type: " + type);
        }

        public abstract void allocateNew(int estimated);

        void writeNull(int rowIndex) {
            if (field.isNullable()) {
                vector.setNull(rowIndex);
            } else {
                throw error("writeNull");
            }
        }

        void writeBool(int rowIndex, boolean value) {
            throw error("writeBool");
        }

        void writeInt8(int rowIndex, byte value) {
            throw error("writeInt8");
        }

        void writeInt16(int rowIndex, short value) {
            throw error("writeInt16");
        }

        void writeInt32(int rowIndex, int value) {
            throw error("writeInt32");
        }

        void writeInt64(int rowIndex, long value) {
            throw error("writeInt64");
        }

        void writeUint8(int rowIndex, int value) {
            throw error("writeUint8");
        }

        void writeUint16(int rowIndex, int value) {
            throw error("writeUint16");
        }

        void writeUint32(int rowIndex, long value) {
            throw error("writeUint32");
        }

        void writeUint64(int rowIndex, long value) {
            throw error("writeUint64");
        }

        void writeFloat(int rowIndex, float value) {
            throw error("writeFloat");
        }

        void writeDouble(int rowIndex, double value) {
            throw error("writeDouble");
        }

        void writeText(int rowIndex, String text) {
            throw error("writeText");
        }

        void writeJson(int rowIndex, String json) {
            throw error("writeJson");
        }

        void writeJsonDocument(int rowIndex, String jsonDocument) {
            throw error("writeJsonDocument");
        }

        void writeBytes(int rowIndex, byte[] bytes) {
            throw error("writeBytes");
        }

        void writeYson(int rowIndex, byte[] yson) {
            throw error("writeYson");
        }

        void writeUuid(int rowIndex, UUID yson) {
            throw error("writeUuid");
        }

        void writeDate(int rowIndex, LocalDate date) {
            throw error("writeDate");
        }

        void writeDatetime(int rowIndex, LocalDateTime datetime) {
            throw error("writeDatetime");
        }

        void writeTimestamp(int rowIndex, Instant instant) {
            throw error("writeTimestamp");
        }

        void writeInterval(int rowIndex, Duration interval) {
            throw error("writeInterval");
        }

        void writeDate32(int rowIndex, LocalDate date32) {
            throw error("writeDate32");
        }

        void writeDatetime64(int rowIndex, LocalDateTime datetime64) {
            throw error("writeDatetime64");
        }

        void writeTimestamp64(int rowIndex, Instant instant64) {
            throw error("writeTimestamp64");
        }

        void writeInterval64(int rowIndex, Duration interval64) {
            throw error("writeInterval64");
        }

        void writeDecimal(int rowIndex, DecimalValue value) {
            throw error("writeDecimal");
        }
    }

    private static class FixedWidthColumn<T extends BaseFixedWidthVector> extends Column<T> {

        FixedWidthColumn(Field field, Type type, T vector) {
            super(field, type, vector);
        }

        @Override
        public void allocateNew(int estimated) {
            vector.allocateNew(estimated);
        }
    }

    private static class VariableWidthColumn<T extends BaseVariableWidthVector> extends Column<T> {

        VariableWidthColumn(Field field, Type type, T vector) {
            super(field, type, vector);
        }

        @Override
        public void allocateNew(int estimated) {
            vector.allocateNew(estimated);
        }
    }

    private static class TinyIntColumn extends FixedWidthColumn<TinyIntVector> {

        TinyIntColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new TinyIntVector(field, allocator));
        }

        @Override
        void writeBool(int rowIndex, boolean value) {
            if (type != PrimitiveType.Bool) {
                throw error("writeBool");
            }
            vector.setSafe(rowIndex, value ? 1 : 0);
        }

        @Override
        void writeInt8(int rowIndex, byte value) {
            if (type != PrimitiveType.Int8) {
                throw error("writeInt8");
            }
            vector.setSafe(rowIndex, value);
        }

        @Override
        void writeUint8(int rowIndex, int value) {
            if (type != PrimitiveType.Uint8) {
                throw error("writeUint8");
            }
            vector.setSafe(rowIndex, value);
        }
    }

    private static class SmallIntColumn extends FixedWidthColumn<SmallIntVector> {

        SmallIntColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new SmallIntVector(field, allocator));
        }

        @Override
        void writeInt16(int rowIndex, short value) {
            if (type != PrimitiveType.Int16) {
                throw error("writeInt16");
            }
            vector.setSafe(rowIndex, value);
        }

        @Override
        void writeUint16(int rowIndex, int value) {
            if (type != PrimitiveType.Uint16) {
                throw error("writeUint16");
            }
            vector.setSafe(rowIndex, value);
        }

        @Override
        void writeDate(int rowIndex, LocalDate date) {
            if (type != PrimitiveType.Date) {
                throw error("writeDate");
            }
            vector.setSafe(rowIndex, (int) date.toEpochDay());
        }
    }

    private static class IntColumn extends FixedWidthColumn<IntVector> {

        IntColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new IntVector(field, allocator));
        }

        @Override
        void writeInt32(int rowIndex, int value) {
            if (type != PrimitiveType.Int32) {
                throw error("writeInt32");
            }
            vector.setSafe(rowIndex, value);
        }

        @Override
        void writeUint32(int rowIndex, long value) {
            if (type != PrimitiveType.Uint32) {
                throw error("writeUint32");
            }
            vector.setSafe(rowIndex, (int) value);
        }

        @Override
        void writeDate32(int rowIndex, LocalDate date) {
            if (type != PrimitiveType.Date32) {
                throw error("writeDate32");
            }
            vector.setSafe(rowIndex, (int) date.toEpochDay());
        }

        @Override
        void writeDatetime(int rowIndex, LocalDateTime datetime) {
            if (type != PrimitiveType.Datetime) {
                throw error("writeDatetime");
            }
            vector.setSafe(rowIndex, (int) datetime.toEpochSecond(ZoneOffset.UTC));
        }
    }

    private static class BigIntColumn extends FixedWidthColumn<BigIntVector> {

        BigIntColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new BigIntVector(field, allocator));
        }

        @Override
        void writeInt64(int rowIndex, long value) {
            if (type != PrimitiveType.Int64) {
                throw error("writeInt64");
            }
            vector.setSafe(rowIndex, value);
        }

        @Override
        void writeUint64(int rowIndex, long value) {
            if (type != PrimitiveType.Uint64) {
                throw error("writeUint64");
            }
            vector.setSafe(rowIndex, value);
        }

        @Override
        void writeDatetime64(int rowIndex, LocalDateTime datetime) {
            if (type != PrimitiveType.Datetime64) {
                throw error("writeDatetime64");
            }
            vector.setSafe(rowIndex, (int) datetime.toEpochSecond(ZoneOffset.UTC));
        }

        @Override
        void writeTimestamp(int rowIndex, Instant value) {
            if (type != PrimitiveType.Timestamp) {
                throw error("writeTimestamp");
            }
            long micros = value.getEpochSecond() * 1000000L + value.getNano() / 1000;
            vector.setSafe(rowIndex, micros);
        }

        @Override
        void writeTimestamp64(int rowIndex, Instant value) {
            if (type != PrimitiveType.Timestamp64) {
                throw error("writeTimestamp64");
            }
            long micros = value.getEpochSecond() * 1000000L + value.getNano() / 1000;
            vector.setSafe(rowIndex, micros);
        }

        @Override
        void writeInterval(int rowIndex, Duration duration) {
            if (type != PrimitiveType.Interval) {
                throw error("writeInterval");
            }
            long micros = duration.getSeconds() * 1000000L + duration.getNano() / 1000;
            vector.setSafe(rowIndex, micros);
        }

        @Override
        void writeInterval64(int rowIndex, Duration duration) {
            if (type != PrimitiveType.Interval64) {
                throw error("writeInterval64");
            }
            long micros = duration.getSeconds() * 1000000L + duration.getNano() / 1000;
            vector.setSafe(rowIndex, micros);
        }
    }

    private static class FloatColumn extends FixedWidthColumn<Float4Vector> {

        FloatColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new Float4Vector(field, allocator));
        }

        @Override
        void writeFloat(int rowIndex, float value) {
            vector.setSafe(rowIndex, value);
        }
    }

    private static class DoubleColumn extends FixedWidthColumn<Float8Vector> {

        DoubleColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new Float8Vector(field, allocator));
        }

        @Override
        void writeDouble(int rowIndex, double value) {
            vector.setSafe(rowIndex, value);
        }
    }

    private static class VarCharColumn extends VariableWidthColumn<VarCharVector> {

        VarCharColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new VarCharVector(field, allocator));
        }

        @Override
        void writeText(int rowIndex, String text) {
            if (type != PrimitiveType.Text) {
                throw error("writeText");
            }
            vector.setSafe(rowIndex, text.getBytes());
        }

        @Override
        void writeJson(int rowIndex, String json) {
            if (type != PrimitiveType.Json) {
                throw error("writeJson");
            }
            vector.setSafe(rowIndex, json.getBytes());
        }

        @Override
        void writeJsonDocument(int rowIndex, String jsonDocument) {
            if (type != PrimitiveType.JsonDocument) {
                throw error("writeJsonDocument");
            }
            vector.setSafe(rowIndex, jsonDocument.getBytes());
        }
    }

    private static class VarBinaryColumn extends VariableWidthColumn<VarBinaryVector> {

        VarBinaryColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new VarBinaryVector(field, allocator));
        }

        @Override
        void writeBytes(int rowIndex, byte[] bytes) {
            if (type != PrimitiveType.Bytes) {
                throw error("writeBytes");
            }
            vector.setSafe(rowIndex, bytes);
        }

        @Override
        void writeYson(int rowIndex, byte[] yson) {
            if (type != PrimitiveType.Yson) {
                throw error("writeYson");
            }
            vector.setSafe(rowIndex, yson);
        }
    }

    private static class FixedBinaryColumn extends FixedWidthColumn<FixedSizeBinaryVector> {

        FixedBinaryColumn(Type type, BufferAllocator allocator, Field field) {
            super(field, type, new FixedSizeBinaryVector(field, allocator));
        }

        /**
         * @see ProtoValue#newUuid(java.util.UUID)
         */
        @Override
        void writeUuid(int rowIndex, UUID uuid) {
            if (type != PrimitiveType.Uuid) {
                throw error("writeUuid");
            }

            ByteBuffer buf = ByteBuffer.allocate(16);

            long msb = uuid.getMostSignificantBits();
            long timeLow = (msb & 0xffffffff00000000L) >>> 32;
            long timeMid = (msb & 0x00000000ffff0000L) << 16;
            long timeHighAndVersion = (msb & 0x000000000000ffffL) << 48;
            buf.putLong(LittleEndian.bswap(timeLow | timeMid | timeHighAndVersion));
            buf.putLong(uuid.getLeastSignificantBits());

            vector.setSafe(rowIndex, buf.array());
        }

        @Override
        void writeDecimal(int rowIndex, DecimalValue value) {
            if (type.getKind() != Type.Kind.DECIMAL) {
                throw error("writeDecimal");
            }

            ByteBuffer buf = ByteBuffer.allocate(16);
            buf.putLong(LittleEndian.bswap(value.getLow()));
            buf.putLong(LittleEndian.bswap(value.getHigh()));

            vector.setSafe(rowIndex, buf.array());
        }
    }

    private static class ColumnInfo {
        private final String name;
        private final Type type;

        ColumnInfo(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        private Column<?> createVector(BufferAllocator allocator) {
            if (type.getKind() == Type.Kind.OPTIONAL) {
                return createColumnVector(allocator, type.unwrapOptional(), t -> Field.nullable(name, t));
            }

            return createColumnVector(allocator, type, t -> Field.notNullable(name, t));
        }
    }

    private static Column<?> createColumnVector(BufferAllocator allocator, Type type, Function<ArrowType, Field> gen) {
        if (type.getKind() == Type.Kind.DECIMAL) {
            return new FixedBinaryColumn(type, allocator, gen.apply(new ArrowType.FixedSizeBinary(16)));
        }

        if (type.getKind() == Type.Kind.PRIMITIVE) {
            switch ((PrimitiveType) type) {
                case Bool:
                    return new TinyIntColumn(type, allocator, gen.apply(new ArrowType.Int(8, false)));

                case Int8:
                    return new TinyIntColumn(type, allocator, gen.apply(new ArrowType.Int(8, true)));
                case Int16:
                    return new SmallIntColumn(type, allocator, gen.apply(new ArrowType.Int(16, true)));
                case Int32:
                    return new IntColumn(type, allocator, gen.apply(new ArrowType.Int(32, true)));
                case Int64:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Int(64, true)));

                case Uint8:
                    return new TinyIntColumn(type, allocator, gen.apply(new ArrowType.Int(8, false)));
                case Uint16:
                    return new SmallIntColumn(type, allocator, gen.apply(new ArrowType.Int(16, false)));
               case Uint32:
                    return new IntColumn(type, allocator, gen.apply(new ArrowType.Int(32, false)));
                case Uint64:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Int(64, false)));

                case Float:
                    return new FloatColumn(type, allocator, gen.apply(
                            new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)
                    ));
                case Double:
                    return new DoubleColumn(type, allocator, gen.apply(
                            new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)
                    ));

                case Text:
                case Json:
                case JsonDocument:
                    return new VarCharColumn(type, allocator, gen.apply(new ArrowType.Utf8()));

                case Bytes:
                case Yson:
                    return new VarBinaryColumn(type, allocator, gen.apply(new ArrowType.Binary()));

                case Uuid:
                    return new FixedBinaryColumn(type, allocator, gen.apply(new ArrowType.FixedSizeBinary(16)));

                case Date:
                    return new SmallIntColumn(type, allocator, gen.apply(new ArrowType.Int(16, false)));
                case Datetime:
                    return new IntColumn(type, allocator, gen.apply(new ArrowType.Int(32, false)));
                case Timestamp:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Int(64, true)));
                case Interval:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Duration(TimeUnit.MILLISECOND)));

                case Date32:
                    return new IntColumn(type, allocator, gen.apply(new ArrowType.Int(32, true)));
                case Datetime64:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Int(64, true)));
                case Timestamp64:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Int(64, true)));
                case Interval64:
                    return new BigIntColumn(type, allocator, gen.apply(new ArrowType.Int(64, true)));

                default:
                    break;
            }
        }

        throw new IllegalArgumentException("Type " + type + " is not supported in ArrowWriter");
    }

    public static Schema newSchema() {
        return new Schema();
    }

    public static class Schema {
        private final List<ColumnInfo> columns = new ArrayList<>();

        public Schema addColumn(String name, Type type) {
            this.columns.add(new ColumnInfo(name, type));
            return this;
        }

        public Schema addNullableColumn(String name, Type type) {
            return addColumn(name, type.makeOptional());
        }

        public ApacheArrowWriter createWriter(BufferAllocator allocator) {
            return new ApacheArrowWriter(allocator, columns);
        }
    }
}
