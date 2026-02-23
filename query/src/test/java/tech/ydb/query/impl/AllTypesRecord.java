package tech.ydb.table.integration;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.common.hash.Hashing;
import org.junit.Assert;

import tech.ydb.table.description.TableDescription;
import tech.ydb.table.query.ApacheArrowWriter;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.Value;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AllTypesRecord {
    private static final DecimalType YDB_DECIMAL = DecimalType.getDefault();
    private static final DecimalType BANK_DECIMAL = DecimalType.of(31, 9);
    private static final DecimalType BIG_DECIMAL = DecimalType.of(35, 0);

    // Keys
    private final long id1;
    private final int id2;
    private final byte[] payload;
    private final int length;
    private final String hash ;

    // All types
    private final Byte v_int8;
    private final Short v_int16;
    private final Integer v_int32;
    private final Long v_int64;

    private final Integer v_uint8;
    private final Integer v_uint16;
    private final Long v_uint32;
    private final Long v_uint64;

    private final Boolean v_bool;
    private final Float v_float;
    private final Double v_double;

    private final String v_text;
    private final String v_json;
    private final String v_jsdoc;

    private final byte[] v_bytes;
    private final byte[] v_yson;

    private final UUID v_uuid;

    private final LocalDate v_date;
    private final LocalDateTime v_datetime;
    private final Instant v_timestamp;
    private final Duration v_interval;

    private final LocalDate v_date32;
    private final LocalDateTime v_datetime64;
    private final Instant v_timestamp64;
    private final Duration v_interval64;

    private final DecimalValue v_ydb_decimal;
    private final DecimalValue v_bank_decimal;
    private final DecimalValue v_big_decimal;

    private AllTypesRecord(long id1, int id2, Random rnd) {
        // Keys
        this.id1 = id1;
        this.id2 = id2;
        this.length = 10 + rnd.nextInt(256);
        this.payload = new byte[this.length];
        rnd.nextBytes(payload);

        this.hash = Hashing.sha256().hashBytes(payload).toString();


        // All types
        this.v_int8 = nullable(rnd, (byte) rnd.nextInt());
        this.v_int16 = nullable(rnd, (short) rnd.nextInt());
        this.v_int32 = nullable(rnd, rnd.nextInt());
        this.v_int64 = nullable(rnd, rnd.nextLong());

        this.v_uint8 = nullable(rnd, rnd.nextInt() & 0xFF);
        this.v_uint16 = nullable(rnd, rnd.nextInt() & 0xFFFF);
        this.v_uint32 = nullable(rnd, rnd.nextLong() & 0xFFFFFFFFL);
        this.v_uint64 = nullable(rnd, rnd.nextLong());

        this.v_bool = nullable(rnd, rnd.nextBoolean());
        this.v_float = nullable(rnd, rnd.nextFloat());
        this.v_double = nullable(rnd, rnd.nextDouble());

        this.v_text = nullable(rnd, "Text" + rnd.nextInt(1000));
        this.v_json = nullable(rnd, "{\"json\":" + (1000 + rnd.nextInt(1000)) + "}");
        this.v_jsdoc = nullable(rnd, "{\"document\":" + (2000 + rnd.nextInt(1000)) + "}");

        this.v_bytes = nullable(rnd, ("Bytes " + (3000 + rnd.nextInt(1000))).getBytes(StandardCharsets.UTF_8));
        this.v_yson = nullable(rnd, ("{yson=" + (4000 + rnd.nextInt(1000)) + "}").getBytes(StandardCharsets.UTF_8));

        this.v_uuid = nullable(rnd, UUID.nameUUIDFromBytes(("UUID" + rnd.nextInt()).getBytes(StandardCharsets.UTF_8)));

        this.v_date = nullable(rnd, LocalDate.ofEpochDay(rnd.nextInt(5000)));
        this.v_datetime = nullable(rnd, LocalDateTime.ofEpochSecond(0x60FFFFFF & rnd.nextLong(), 0, ZoneOffset.UTC));
        this.v_timestamp = nullable(rnd, Instant.ofEpochSecond(0x3FFFFFFFL & rnd.nextLong(),
                rnd.nextInt(1000000) * 1000));
        this.v_interval = nullable(rnd, Duration.ofNanos((0x7FFFFFFFFFL & rnd.nextLong() - 0x4000000000L) * 1000));

        this.v_date32 = nullable(rnd, LocalDate.ofEpochDay(rnd.nextInt(5000) - 2500));
        this.v_datetime64 = nullable(rnd, LocalDateTime.ofEpochSecond(0x60FFFFFF & rnd.nextLong() - 0x30FFFFFF, 0,
                ZoneOffset.UTC));
        this.v_timestamp64 = nullable(rnd, Instant.ofEpochSecond(0x7FFFFFFFFFL & rnd.nextLong() - 0x4000000000L,
                rnd.nextInt(1000000) * 1000));
        this.v_interval64 = nullable(rnd, Duration.ofNanos((0x7FFFFFFFFFL & rnd.nextLong() - 0x4000000000L) * 1000));

        this.v_ydb_decimal = nullable(rnd, randomDecimal(YDB_DECIMAL, rnd));
        this.v_bank_decimal = nullable(rnd, randomDecimal(BANK_DECIMAL, rnd));
        this.v_big_decimal = nullable(rnd, randomDecimal(BIG_DECIMAL, rnd));
    }

    private <T> void assertValue(Set<String> columns, String key, int idx, Function<ValueReader, T> reader,
            ResultSetReader rs, T value) {
        if (!columns.contains(key)) {
            return;
        }

        String msg = "Row " + idx + " " + key;
        Assert.assertEquals(msg + " is null", value != null, rs.getColumn(key).isOptionalItemPresent());

        if (value != null) {
            if (value instanceof byte[]) {
                Assert.assertArrayEquals(msg, (byte[]) value, (byte[]) reader.apply(rs.getColumn(key)));
            } else {
                Assert.assertEquals(msg, value, reader.apply(rs.getColumn(key)));
            }

            int columnIdx = rs.getColumnIndex(key);
            Value<?> v = rs.getColumn(columnIdx).getValue();
            Assert.assertEquals(rs.getColumnType(columnIdx), v.getType());
        }
    }

    public void assertRow(Set<String> columns, int idx, ResultSetReader rs) {
        // keys are required
        Assert.assertEquals("Row " + idx + " id1", id1, rs.getColumn("id1").getUint64());
        Assert.assertEquals("Row " + idx + " id2", id2, rs.getColumn("id2").getInt64());
        Assert.assertArrayEquals("Row " + idx + " payload", payload, rs.getColumn("payload").getBytes());
        Assert.assertEquals("Row " + idx + " length", length, rs.getColumn("length").getUint32());
        Assert.assertEquals("Row " + idx + " hash", hash, rs.getColumn("hash").getText());

        // other columns may be skipped or be empty
        assertValue(columns, "Int8", idx, ValueReader::getInt8, rs, v_int8);
        assertValue(columns, "Int16", idx, ValueReader::getInt16, rs, v_int16);
        assertValue(columns, "Int32", idx, ValueReader::getInt32, rs, v_int32);
        assertValue(columns, "Int64", idx, ValueReader::getInt64, rs, v_int64);

        assertValue(columns, "Uint8", idx, ValueReader::getUint8, rs, v_uint8);
        assertValue(columns, "Uint16", idx, ValueReader::getUint16, rs, v_uint16);
        assertValue(columns, "Uint32", idx, ValueReader::getUint32, rs, v_uint32);
        assertValue(columns, "Uint64", idx, ValueReader::getUint64, rs, v_uint64);

        assertValue(columns, "Bool", idx, ValueReader::getBool, rs, v_bool);
        assertValue(columns, "Float", idx, ValueReader::getFloat, rs, v_float);
        assertValue(columns, "Double", idx, ValueReader::getDouble, rs, v_double);

        assertValue(columns, "Text", idx, ValueReader::getText, rs, v_text);
        assertValue(columns, "Json", idx, ValueReader::getJson, rs, v_json);
        assertValue(columns, "JsonDocument", idx, ValueReader::getJsonDocument, rs, v_jsdoc);

        assertValue(columns, "Uuid", idx, ValueReader::getUuid, rs, v_uuid);

        assertValue(columns, "Bytes", idx, ValueReader::getBytes, rs, v_bytes);
        assertValue(columns, "Yson", idx, ValueReader::getYson, rs, v_yson);

        assertValue(columns, "Date", idx, ValueReader::getDate, rs, v_date);
        assertValue(columns, "Datetime", idx, ValueReader::getDatetime, rs, v_datetime);
        assertValue(columns, "Timestamp", idx, ValueReader::getTimestamp, rs, v_timestamp);
        assertValue(columns, "Interval", idx, ValueReader::getInterval, rs, v_interval);

        assertValue(columns, "Date32", idx, ValueReader::getDate32, rs, v_date32);
        assertValue(columns, "Datetime64", idx, ValueReader::getDatetime64, rs, v_datetime64);
        assertValue(columns, "Timestamp64", idx, ValueReader::getTimestamp64, rs, v_timestamp64);
        assertValue(columns, "Interval64", idx, ValueReader::getInterval64, rs, v_interval64);

        assertValue(columns, "YdbDecimal", idx, ValueReader::getDecimal, rs, v_ydb_decimal);
        assertValue(columns, "BankDecimal", idx, ValueReader::getDecimal, rs, v_bank_decimal);
        assertValue(columns, "BigDecimal", idx, ValueReader::getDecimal, rs, v_big_decimal);
    }

    private <T> void writeNullable(Set<String> columns, String key, ApacheArrowWriter.Row row,
            BiConsumer<String, T> writer, T value) {
        if (!columns.contains(key)) {
            return;
        }

        if (value == null) {
            row.writeNull(key);
        } else {
            writer.accept(key, value);
        }
    }

    public void writeToApacheArrow(Set<String> columnNames, ApacheArrowWriter.Row row) {
        // keys are required
        row.writeUint64("id1", id1);
        row.writeInt64("id2", id2);
        row.writeBytes("payload", payload);
        row.writeUint32("length", length);
        row.writeText("hash", hash);

        // other columns may be skipped or be empty
        writeNullable(columnNames, "Int8", row, row::writeInt8, v_int8);
        writeNullable(columnNames, "Int16", row, row::writeInt16, v_int16);
        writeNullable(columnNames, "Int32", row, row::writeInt32, v_int32);
        writeNullable(columnNames, "Int64", row, row::writeInt64, v_int64);

        writeNullable(columnNames, "Uint8", row, row::writeUint8, v_uint8);
        writeNullable(columnNames, "Uint16", row, row::writeUint16, v_uint16);
        writeNullable(columnNames, "Uint32", row, row::writeUint32, v_uint32);
        writeNullable(columnNames, "Uint64", row, row::writeUint64, v_uint64);

        writeNullable(columnNames, "Bool", row, row::writeBool, v_bool);

        writeNullable(columnNames, "Float", row, row::writeFloat, v_float);
        writeNullable(columnNames, "Double", row, row::writeDouble, v_double);

        writeNullable(columnNames, "Text", row, row::writeText, v_text);
        writeNullable(columnNames, "Json", row, row::writeJson, v_json);
        writeNullable(columnNames, "JsonDocument", row, row::writeJsonDocument, v_jsdoc);

        writeNullable(columnNames, "Bytes", row, row::writeBytes, v_bytes);
        writeNullable(columnNames, "Yson", row, row::writeYson, v_yson);

        writeNullable(columnNames, "Uuid", row, row::writeUuid, v_uuid);

        writeNullable(columnNames, "Date", row, row::writeDate, v_date);
        writeNullable(columnNames, "Datetime", row, row::writeDatetime, v_datetime);
        writeNullable(columnNames, "Timestamp", row, row::writeTimestamp, v_timestamp);
        writeNullable(columnNames, "Interval", row, row::writeInterval, v_interval);

        writeNullable(columnNames, "Date32", row, row::writeDate32, v_date32);
        writeNullable(columnNames, "Datetime64", row, row::writeDatetime64, v_datetime64);
        writeNullable(columnNames, "Timestamp64", row, row::writeTimestamp64, v_timestamp64);
        writeNullable(columnNames, "Interval64", row, row::writeInterval64, v_interval64);

        writeNullable(columnNames, "YdbDecimal", row, row::writeDecimal, v_ydb_decimal);
        writeNullable(columnNames, "BankDecimal", row, row::writeDecimal, v_bank_decimal);
        writeNullable(columnNames, "BigDecimal", row, row::writeDecimal, v_big_decimal);
    }

    public static DecimalValue randomDecimal(DecimalType type, Random rnd) {
        int kind = rnd.nextInt(1000);
        switch (kind) {
            case 0: return type.getNegInf();
            case 499: return type.newValue(0);
            case 999: return type.getInf();
            default:
                break;
        }
        BigInteger unscaled = new BigInteger(1 + rnd.nextInt(type.getPrecision() * 3 - 1), rnd);
        if (kind < 500) {
            unscaled = unscaled.negate();
        }
        return type.newValueUnscaled(unscaled);
    }

    private static <T> T nullable(Random rnd, T value) {
        return rnd.nextInt(10) == 0 ? null : value;
    }

    public static List<AllTypesRecord> randomBatch(int id1, int id2_start, int count) {
        Random rnd = new Random(id1 * count + id2_start);
        List<AllTypesRecord> batch = new ArrayList<>(count);
        for (int idx = 0; idx < count; idx += 1) {
            batch.add(new AllTypesRecord(id1, id2_start + idx, rnd));
        }
        return batch;
    }

    public static TableDescription createTableDescription(boolean isColumnShard) {
        TableDescription.Builder builder = TableDescription.newBuilder()
                .addNonnullColumn("id1", PrimitiveType.Uint64)
                .addNonnullColumn("id2", PrimitiveType.Int64)
                .addNonnullColumn("payload", PrimitiveType.Bytes)
                .addNonnullColumn("length", PrimitiveType.Uint32)
                .addNonnullColumn("hash", PrimitiveType.Text)

                .addNullableColumn("Int8", PrimitiveType.Int8)
                .addNullableColumn("Int16", PrimitiveType.Int16)
                .addNullableColumn("Int32", PrimitiveType.Int32)
                .addNullableColumn("Int64", PrimitiveType.Int64)
                .addNullableColumn("Uint8", PrimitiveType.Uint8)
                .addNullableColumn("Uint16", PrimitiveType.Uint16)
                .addNullableColumn("Uint32", PrimitiveType.Uint32)
                .addNullableColumn("Uint64", PrimitiveType.Uint64)
                .addNullableColumn("Bool", PrimitiveType.Bool)
                .addNullableColumn("Float", PrimitiveType.Float)
                .addNullableColumn("Double", PrimitiveType.Double)
                .addNullableColumn("Text", PrimitiveType.Text)
                .addNullableColumn("Json", PrimitiveType.Json)
                .addNullableColumn("JsonDocument", PrimitiveType.JsonDocument)
                .addNullableColumn("Bytes", PrimitiveType.Bytes)
                .addNullableColumn("Yson", PrimitiveType.Yson);

        // https://github.com/ydb-platform/ydb/issues/13047
        if (!isColumnShard) {
                builder = builder.addNullableColumn("Uuid", PrimitiveType.Uuid);
        }

        builder = builder
                .addNullableColumn("Date", PrimitiveType.Date)
                .addNullableColumn("Datetime", PrimitiveType.Datetime)
                .addNullableColumn("Timestamp", PrimitiveType.Timestamp);

        // https://github.com/ydb-platform/ydb/issues/13050
        if (!isColumnShard) {
                builder = builder.addNullableColumn("Interval", PrimitiveType.Interval);
        }

        builder = builder
                .addNullableColumn("Date32", PrimitiveType.Date32)
                .addNullableColumn("Datetime64", PrimitiveType.Datetime64)
                .addNullableColumn("Timestamp64", PrimitiveType.Timestamp64)
                .addNullableColumn("Interval64", PrimitiveType.Interval64)
                .addNullableColumn("YdbDecimal", YDB_DECIMAL)
                .addNullableColumn("BankDecimal", BANK_DECIMAL)
                .addNullableColumn("BigDecimal", BIG_DECIMAL);

        if (isColumnShard) {
            builder = builder.setPrimaryKey("hash").setStoreType(TableDescription.StoreType.COLUMN);
        } else {
            builder = builder.setPrimaryKeys("id1", "id2").setStoreType(TableDescription.StoreType.ROW);
        }

        return builder.build();
    }
}
