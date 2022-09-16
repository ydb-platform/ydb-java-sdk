package tech.ydb.table.values;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;

import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.ByteString;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class PrimitiveValueTest {

    @Test
    public void bool() {
        PrimitiveValue v = PrimitiveValue.newBool(true);

        assertThat(v).isSameInstanceAs(PrimitiveValue.newBool(true));
        assertThat(v).isEqualTo(PrimitiveValue.newBool(true));
        assertThat(v).isNotSameInstanceAs(PrimitiveValue.newBool(false));
        assertThat(v).isNotEqualTo(PrimitiveValue.newBool(false));
        assertThat(v.toString()).isEqualTo("true");
        assertThat(v.getBool()).isTrue();

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isSameInstanceAs(ProtoValue.fromBool(true));
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromBool(true));
        assertThat(v).isSameInstanceAs(ProtoValue.fromPb(PrimitiveType.Bool, vPb));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Bool, vPb));
    }

    @Test
    public void int8() {
        PrimitiveValue v = PrimitiveValue.newInt8((byte) 1);

        assertThat(v).isEqualTo(PrimitiveValue.newInt8((byte) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newInt8((byte) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt8()).isEqualTo((byte) 1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromInt8((byte) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Int8, vPb));
    }

    @Test
    public void uint8_positive() {
        PrimitiveValue v = PrimitiveValue.newUint8((byte) 1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint8((byte) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint8((byte) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint8()).isEqualTo(1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint8((byte) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint8, vPb));
    }

    @Test
    public void uint8_negative() {
        PrimitiveValue v = PrimitiveValue.newUint8((byte) -1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint8((byte) -1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint8((byte) 0));
        assertThat(v.toString()).isEqualTo("255"); // 0xff
        assertThat(v.getUint8()).isEqualTo(255); // 0xff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint8((byte) -1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint8, vPb));
    }

    @Test
    public void int16() {
        PrimitiveValue v = PrimitiveValue.newInt16((short) 1);

        assertThat(v).isEqualTo(PrimitiveValue.newInt16((short) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newInt16((short) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt16()).isEqualTo((short) 1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromInt16((short) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Int16, vPb));
    }

    @Test
    public void uint16_positive() {
        PrimitiveValue v = PrimitiveValue.newUint16((short) 1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint16((short) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint16((short) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint16()).isEqualTo(1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint16((short) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint16, vPb));
    }

    @Test
    public void uint16_negative() {
        PrimitiveValue v = PrimitiveValue.newUint16((short) -1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint16((short) -1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint16((short) 0));
        assertThat(v.toString()).isEqualTo("65535"); // 0xffff
        assertThat(v.getUint16()).isEqualTo(65535); // 0xffff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint16((short) -1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint16, vPb));
    }

    @Test
    public void int32() {
        PrimitiveValue v = PrimitiveValue.newInt32(1);

        assertThat(v).isEqualTo(PrimitiveValue.newInt32(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newInt32(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt32()).isEqualTo(1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromInt32(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Int32, vPb));
    }

    @Test
    public void uint32_positive() {
        PrimitiveValue v = PrimitiveValue.newUint32(1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint32(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint32(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint32()).isEqualTo(1L);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint32(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint32, vPb));
    }

    @Test
    public void uint32_negative() {
        PrimitiveValue v = PrimitiveValue.newUint32(-1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint32(-1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint32(0));
        assertThat(v.toString()).isEqualTo("4294967295"); // 0xffffffff
        assertThat(v.getUint32()).isEqualTo(4294967295L); // 0xffffffff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint32(-1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint32, vPb));
    }

    @Test
    public void int64() {
        PrimitiveValue v = PrimitiveValue.newInt64(1);

        assertThat(v).isEqualTo(PrimitiveValue.newInt64(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newInt64(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt64()).isEqualTo(1L);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromInt64(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Int64, vPb));
    }

    @Test
    public void uint64_positive() {
        PrimitiveValue v = PrimitiveValue.newUint64(1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint64(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint64(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint64()).isEqualTo(1L);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint64(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint64, vPb));
    }

    @Test
    public void uint64_negative() {
        PrimitiveValue v = PrimitiveValue.newUint64(-1);

        assertThat(v).isEqualTo(PrimitiveValue.newUint64(-1));
        assertThat(v).isNotEqualTo(PrimitiveValue.newUint64(0));
        assertThat(v.toString()).isEqualTo("18446744073709551615"); // 0xffffffffffffffff
        assertThat(v.getUint64()).isEqualTo(Long.parseUnsignedLong("18446744073709551615")); // 0xffffffffffffffff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUint64(-1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uint64, vPb));
    }

    @Test
    public void floatTest() {
        PrimitiveValue v = PrimitiveValue.newFloat(3.14159f);

        assertThat(v).isEqualTo(PrimitiveValue.newFloat(3.14159f));
        assertThat(v).isNotEqualTo(PrimitiveValue.newFloat(0f));
        assertThat(v.toString()).isEqualTo("3.14159");
        assertThat(v.getFloat()).isEqualTo(3.14159f);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromFloat(3.14159f));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Float, vPb));
    }

    @Test
    public void float_nan() {
        PrimitiveValue v = PrimitiveValue.newFloat(Float.NaN);

        assertThat(v).isEqualTo(PrimitiveValue.newFloat(Float.NaN));
        assertThat(v).isNotEqualTo(PrimitiveValue.newFloat(0f));
        assertThat(v.toString()).isEqualTo("NaN");
        assertThat(v.getFloat()).isNaN();

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromFloat(Float.NaN));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Float, vPb));
    }

    @Test
    public void float_inf() {
        {
            PrimitiveValue v = PrimitiveValue.newFloat(Float.POSITIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.newFloat(Float.POSITIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.newFloat(0f));
            assertThat(v.toString()).isEqualTo("Infinity");
            assertThat(v.getFloat()).isPositiveInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromFloat(Float.POSITIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Float, vPb));
        }
        {
            PrimitiveValue v = PrimitiveValue.newFloat(Float.NEGATIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.newFloat(Float.NEGATIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.newFloat(0f));
            assertThat(v.toString()).isEqualTo("-Infinity");
            assertThat(v.getFloat()).isNegativeInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromFloat(Float.NEGATIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Float, vPb));
        }
    }

    @Test
    public void doubleTest() {
        PrimitiveValue v = PrimitiveValue.newDouble(3.14159);

        assertThat(v).isEqualTo(PrimitiveValue.newDouble(3.14159));
        assertThat(v).isNotEqualTo(PrimitiveValue.newDouble(0.0));
        assertThat(v.toString()).isEqualTo("3.14159");
        assertThat(v.getDouble()).isEqualTo(3.14159);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDouble(3.14159));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Double, vPb));
    }

    @Test
    public void double_nan() {
        PrimitiveValue v = PrimitiveValue.newDouble(Double.NaN);

        assertThat(v).isEqualTo(PrimitiveValue.newDouble(Double.NaN));
        assertThat(v).isNotEqualTo(PrimitiveValue.newDouble(0.0));
        assertThat(v.toString()).isEqualTo("NaN");
        assertThat(v.getDouble()).isNaN();

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDouble(Double.NaN));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Double, vPb));
    }

    @Test
    public void double_inf() {
        {
            PrimitiveValue v = PrimitiveValue.newDouble(Double.POSITIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.newDouble(Double.POSITIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.newDouble(0.0));
            assertThat(v.toString()).isEqualTo("Infinity");
            assertThat(v.getDouble()).isPositiveInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDouble(Double.POSITIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Double, vPb));
        }
        {
            PrimitiveValue v = PrimitiveValue.newDouble(Double.NEGATIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.newDouble(Double.NEGATIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.newDouble(0.0));
            assertThat(v.toString()).isEqualTo("-Infinity");
            assertThat(v.getDouble()).isNegativeInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDouble(Double.NEGATIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Double, vPb));
        }
    }

    @Test
    public void bytes() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.newBytes(data));
            assertThat(v).isEqualTo(PrimitiveValue.newBytes(ByteString.copyFrom(data)));
            assertThat(v).isNotEqualTo(PrimitiveValue.newBytes(ByteString.EMPTY));

            assertThat(v.toString()).isEqualTo("\"\\000\\007\\077\\177\\377\"");
            assertThat(v.getBytes()).isEqualTo(data);
            assertThat(v.getBytesUnsafe()).isEqualTo(data);
            assertThat(v.getBytesAsByteString()).isEqualTo(ByteString.copyFrom(data));

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.Bytes(data));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Bytes, vPb));
        };

        doTest.accept(PrimitiveValue.newBytes(data));
        doTest.accept(PrimitiveValue.newBytesOwn(data));
        doTest.accept(PrimitiveValue.newBytes(ByteString.copyFrom(data)));

        // hashes must be the same
        assertThat(PrimitiveValue.newBytes(data).hashCode())
            .isEqualTo(PrimitiveValue.newBytes(ByteString.copyFrom(data)).hashCode());
    }

    @Test
    public void yson() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.newYson(data));
            assertThat(v).isEqualTo(PrimitiveValue.newYson(ByteString.copyFrom(data)));
            assertThat(v).isNotEqualTo(PrimitiveValue.newYson(ByteString.EMPTY));

            assertThat(v.toString()).isEqualTo("\"\\000\\007\\077\\177\\377\"");
            assertThat(v.getYson()).isEqualTo(data);
            assertThat(v.getYsonUnsafe()).isEqualTo(data);
            assertThat(v.getYsonBytes()).isEqualTo(ByteString.copyFrom(data));

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromYson(data));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Yson, vPb));
        };

        doTest.accept(PrimitiveValue.newYson(data));
        doTest.accept(PrimitiveValue.newYsonOwn(data));
        doTest.accept(PrimitiveValue.newYson(ByteString.copyFrom(data)));

        // hashes must be the same
        assertThat(PrimitiveValue.newYson(data).hashCode())
            .isEqualTo(PrimitiveValue.newYson(ByteString.copyFrom(data)).hashCode());
    }

    @Test
    public void stringIsNotEqualToYson() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        PrimitiveValue string = PrimitiveValue.newBytes(data);
        PrimitiveValue yson = PrimitiveValue.newYson(data);

        assertThat(string).isNotEqualTo(yson);
        assertThat(yson).isNotEqualTo(string);
        assertThat(string.hashCode()).isNotEqualTo(yson.hashCode());
    }

    @Test
    public void text() {
        String[] fixtures = {
            "Hello", "Hola", "Bonjour",
            "Ciao", "こんにちは", "안녕하세요",
            "Cześć", "Olá", "Здравствуйте",
            "Chào bạn", "您好", "Hallo",
            "Hej", "Ahoj", "سلام", "สวัสด"
        };

        for (String date : fixtures) {
            PrimitiveValue v = PrimitiveValue.newText(date);

            assertThat(v).isEqualTo(PrimitiveValue.newText(date));
            assertThat(v).isNotEqualTo(PrimitiveValue.newText(""));

            assertThat(v.toString()).isEqualTo(String.format("\"%s\"", date));
            assertThat(v.getText()).isEqualTo(date);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromText(date));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Text, vPb));
        }
    }

    @Test
    public void json() {
        String data = "{\"name\": \"jamel\", \"age\": 99}";

        PrimitiveValue v = PrimitiveValue.newJson(data);

        assertThat(v).isEqualTo(PrimitiveValue.newJson(data));
        assertThat(v).isNotEqualTo(PrimitiveValue.newJson(""));

        assertThat(v.toString()).isEqualTo("\"{\\\"name\\\": \\\"jamel\\\", \\\"age\\\": 99}\"");
        assertThat(v.getJson()).isEqualTo(data);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromJson(data));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Json, vPb));
    }

    @Test
    public void uuid() {
        long low = 0x6677445500112233L, high = 0xffeeddccbbaa9988L;
        String uuidStr = "00112233-4455-6677-8899-aabbccddeeff";
        UUID uuid = UUID.fromString(uuidStr);

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.newUuid(uuid));
            assertThat(v).isEqualTo(PrimitiveValue.newUuid(uuidStr));
            assertThat(v).isEqualTo(PrimitiveValue.newUuid(high, low));
            assertThat(v).isNotEqualTo(PrimitiveValue.newUuid(UUID.randomUUID()));

            assertThat(v.toString()).isEqualTo("\"00112233-4455-6677-8899-aabbccddeeff\"");
            assertThat(v.getUuidString()).isEqualTo("00112233-4455-6677-8899-aabbccddeeff");
            assertThat(v.getUuidHigh()).isEqualTo(high);
            assertThat(v.getUuidLow()).isEqualTo(low);
            assertThat(v.getUuidJdk()).isEqualTo(uuid);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUuid(uuid));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromUuid(high, low));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Uuid, vPb));
        };

        doTest.accept(PrimitiveValue.newUuid(uuid));
        doTest.accept(PrimitiveValue.newUuid(uuidStr));
        doTest.accept(PrimitiveValue.newUuid(high, low));
    }

    @Test
    public void date() {
        long day = 17763L;
        LocalDate date = LocalDate.of(2018, Month.AUGUST, 20);
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.newDate(day));
            assertThat(v).isEqualTo(PrimitiveValue.newDate(date));
            assertThat(v).isEqualTo(PrimitiveValue.newDate(instant));
            assertThat(v).isNotEqualTo(PrimitiveValue.newDate(0));

            assertThat(v.toString()).isEqualTo("2018-08-20");
            assertThat(v.getDate()).isEqualTo(date);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDate(day));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDate(date));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDate(instant));

            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Date, vPb));
        };

        doTest.accept(PrimitiveValue.newDate(day));
        doTest.accept(PrimitiveValue.newDate(date));
        doTest.accept(PrimitiveValue.newDate(instant));
    }

    @Test
    public void datetime() {
        long seconds = 1534728225L;
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");
        LocalDateTime localDateTime = LocalDateTime.of(2018, 8, 20, 1, 23, 45);

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.newDatetime(seconds));
            assertThat(v).isEqualTo(PrimitiveValue.newDatetime(instant));
            assertThat(v).isEqualTo(PrimitiveValue.newDatetime(localDateTime));
            assertThat(v).isNotEqualTo(PrimitiveValue.newDatetime(0));

            assertThat(v.toString()).isEqualTo("2018-08-20T01:23:45");
            assertThat(v.getDatetime()).isEqualTo(LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC));

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDatetime(seconds));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDatetime(instant));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromDatetime(localDateTime));

            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Datetime, vPb));
        };

        doTest.accept(PrimitiveValue.newDatetime(seconds));
        doTest.accept(PrimitiveValue.newDatetime(instant));
        doTest.accept(PrimitiveValue.newDatetime(localDateTime));
    }

    @Test
    public void timestamp() {
        long seconds = 1534728225678901L;
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.newTimestamp(seconds));
            assertThat(v).isEqualTo(PrimitiveValue.newTimestamp(instant));
            assertThat(v).isNotEqualTo(PrimitiveValue.newTimestamp(0));

            assertThat(v.toString()).isEqualTo("2018-08-20T01:23:45.678901Z");
            assertThat(v.getTimestamp()).isEqualTo(instant);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromTimestamp(seconds));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromTimestamp(instant));

            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Timestamp, vPb));
        };

        doTest.accept(PrimitiveValue.newTimestamp(seconds));
        doTest.accept(PrimitiveValue.newTimestamp(instant));
    }

    @Test
    public void interval() {
        Duration oneSecond = Duration.ofSeconds(1);
        PrimitiveValue v = PrimitiveValue.newInterval(oneSecond);

        assertThat(v).isEqualTo(PrimitiveValue.newInterval(oneSecond));
        assertThat(v).isNotEqualTo(PrimitiveValue.newInterval(0));

        assertThat(v.toString()).isEqualTo("PT1S");
        assertThat(v.getInterval()).isEqualTo(oneSecond);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromInterval(oneSecond));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.Interval, vPb));
    }

    @Test
    public void tzDate() {
        PrimitiveValue v = PrimitiveValue.newTzDate(
            ZonedDateTime.of(LocalDate.of(2018, Month.AUGUST, 20),
            LocalTime.MIDNIGHT,
            ZoneId.of("Europe/Moscow")));

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromTzDate("2018-08-20,Europe/Moscow"));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.TzDate, vPb));
    }

    @Test
    public void tzDatetime() {
        PrimitiveValue v = PrimitiveValue.newTzDatetime(
            ZonedDateTime.of(LocalDate.of(2018, Month.SEPTEMBER, 21),
                LocalTime.of(12, 34, 56),
                ZoneId.of("Asia/Novosibirsk")));

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromTzDatetime("2018-09-21T12:34:56,Asia/Novosibirsk"));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.TzDatetime, vPb));
    }

    @Test
    public void tzTimestamp() {
        PrimitiveValue v = PrimitiveValue.newTzTimestamp(
            ZonedDateTime.of(LocalDate.of(2018, Month.OCTOBER, 22),
                LocalTime.of(12, 34, 56, (int) TimeUnit.MICROSECONDS.toNanos(778899)),
                ZoneId.of("America/Chicago")));

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.fromTzDatetime("2018-10-22T12:34:56.778899,America/Chicago"));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.TzTimestamp, vPb));
    }
}
