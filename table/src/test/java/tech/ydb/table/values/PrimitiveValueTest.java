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

import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.ByteString;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class PrimitiveValueTest {

    @Test
    public void bool() {
        PrimitiveValue v = PrimitiveValue.bool(true);

        assertThat(v).isSameAs(PrimitiveValue.bool(true));
        assertThat(v).isEqualTo(PrimitiveValue.bool(true));
        assertThat(v).isNotSameAs(PrimitiveValue.bool(false));
        assertThat(v).isNotEqualTo(PrimitiveValue.bool(false));
        assertThat(v.toString()).isEqualTo("true");
        assertThat(v.getBool()).isTrue();

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isSameAs(ProtoValue.bool(true));
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.bool(true));
        assertThat(v).isSameAs(ProtoValue.fromPb(PrimitiveType.bool(), vPb));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.bool(), vPb));
    }

    @Test
    public void int8() {
        PrimitiveValue v = PrimitiveValue.int8((byte) 1);

        assertThat(v).isEqualTo(PrimitiveValue.int8((byte) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.int8((byte) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt8()).isEqualTo((byte) 1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.int8((byte) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.int8(), vPb));
    }

    @Test
    public void uint8_positive() {
        PrimitiveValue v = PrimitiveValue.uint8((byte) 1);

        assertThat(v).isEqualTo(PrimitiveValue.uint8((byte) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint8((byte) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint8()).isEqualTo(1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint8((byte) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint8(), vPb));
    }

    @Test
    public void uint8_negative() {
        PrimitiveValue v = PrimitiveValue.uint8((byte) -1);

        assertThat(v).isEqualTo(PrimitiveValue.uint8((byte) -1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint8((byte) 0));
        assertThat(v.toString()).isEqualTo("255"); // 0xff
        assertThat(v.getUint8()).isEqualTo(255); // 0xff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint8((byte) -1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint8(), vPb));
    }

    @Test
    public void int16() {
        PrimitiveValue v = PrimitiveValue.int16((short) 1);

        assertThat(v).isEqualTo(PrimitiveValue.int16((short) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.int16((short) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt16()).isEqualTo((short) 1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.int16((short) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.int16(), vPb));
    }

    @Test
    public void uint16_positive() {
        PrimitiveValue v = PrimitiveValue.uint16((short) 1);

        assertThat(v).isEqualTo(PrimitiveValue.uint16((short) 1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint16((short) 0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint16()).isEqualTo(1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint16((short) 1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint16(), vPb));
    }

    @Test
    public void uint16_negative() {
        PrimitiveValue v = PrimitiveValue.uint16((short) -1);

        assertThat(v).isEqualTo(PrimitiveValue.uint16((short) -1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint16((short) 0));
        assertThat(v.toString()).isEqualTo("65535"); // 0xffff
        assertThat(v.getUint16()).isEqualTo(65535); // 0xffff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint16((short) -1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint16(), vPb));
    }

    @Test
    public void int32() {
        PrimitiveValue v = PrimitiveValue.int32(1);

        assertThat(v).isEqualTo(PrimitiveValue.int32(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.int32(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt32()).isEqualTo(1);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.int32(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.int32(), vPb));
    }

    @Test
    public void uint32_positive() {
        PrimitiveValue v = PrimitiveValue.uint32(1);

        assertThat(v).isEqualTo(PrimitiveValue.uint32(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint32(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint32()).isEqualTo(1L);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint32(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint32(), vPb));
    }

    @Test
    public void uint32_negative() {
        PrimitiveValue v = PrimitiveValue.uint32(-1);

        assertThat(v).isEqualTo(PrimitiveValue.uint32(-1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint32(0));
        assertThat(v.toString()).isEqualTo("4294967295"); // 0xffffffff
        assertThat(v.getUint32()).isEqualTo(4294967295L); // 0xffffffff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint32(-1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint32(), vPb));
    }

    @Test
    public void int64() {
        PrimitiveValue v = PrimitiveValue.int64(1);

        assertThat(v).isEqualTo(PrimitiveValue.int64(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.int64(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getInt64()).isEqualTo(1L);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.int64(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.int64(), vPb));
    }

    @Test
    public void uint64_positive() {
        PrimitiveValue v = PrimitiveValue.uint64(1);

        assertThat(v).isEqualTo(PrimitiveValue.uint64(1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint64(0));
        assertThat(v.toString()).isEqualTo("1");
        assertThat(v.getUint64()).isEqualTo(1L);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint64(1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint64(), vPb));
    }

    @Test
    public void uint64_negative() {
        PrimitiveValue v = PrimitiveValue.uint64(-1);

        assertThat(v).isEqualTo(PrimitiveValue.uint64(-1));
        assertThat(v).isNotEqualTo(PrimitiveValue.uint64(0));
        assertThat(v.toString()).isEqualTo("18446744073709551615"); // 0xffffffffffffffff
        assertThat(v.getUint64()).isEqualTo(Long.parseUnsignedLong("18446744073709551615")); // 0xffffffffffffffff

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uint64(-1));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uint64(), vPb));
    }

    @Test
    public void float32() {
        PrimitiveValue v = PrimitiveValue.float32(3.14159f);

        assertThat(v).isEqualTo(PrimitiveValue.float32(3.14159f));
        assertThat(v).isNotEqualTo(PrimitiveValue.float32(0f));
        assertThat(v.toString()).isEqualTo("3.14159");
        assertThat(v.getFloat32()).isEqualTo(3.14159f);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float32(3.14159f));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float32(), vPb));
    }

    @Test
    public void float32_nan() {
        PrimitiveValue v = PrimitiveValue.float32(Float.NaN);

        assertThat(v).isEqualTo(PrimitiveValue.float32(Float.NaN));
        assertThat(v).isNotEqualTo(PrimitiveValue.float32(0f));
        assertThat(v.toString()).isEqualTo("NaN");
        assertThat(v.getFloat32()).isNaN();

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float32(Float.NaN));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float32(), vPb));
    }

    @Test
    public void float32_inf() {
        {
            PrimitiveValue v = PrimitiveValue.float32(Float.POSITIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.float32(Float.POSITIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.float32(0f));
            assertThat(v.toString()).isEqualTo("Infinity");
            assertThat(v.getFloat32()).isPositiveInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float32(Float.POSITIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float32(), vPb));
        }
        {
            PrimitiveValue v = PrimitiveValue.float32(Float.NEGATIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.float32(Float.NEGATIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.float32(0f));
            assertThat(v.toString()).isEqualTo("-Infinity");
            assertThat(v.getFloat32()).isNegativeInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float32(Float.NEGATIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float32(), vPb));
        }
    }

    @Test
    public void float64() {
        PrimitiveValue v = PrimitiveValue.float64(3.14159);

        assertThat(v).isEqualTo(PrimitiveValue.float64(3.14159));
        assertThat(v).isNotEqualTo(PrimitiveValue.float64(0.0));
        assertThat(v.toString()).isEqualTo("3.14159");
        assertThat(v.getFloat64()).isEqualTo(3.14159);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float64(3.14159));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float64(), vPb));
    }

    @Test
    public void float64_nan() {
        PrimitiveValue v = PrimitiveValue.float64(Double.NaN);

        assertThat(v).isEqualTo(PrimitiveValue.float64(Double.NaN));
        assertThat(v).isNotEqualTo(PrimitiveValue.float64(0.0));
        assertThat(v.toString()).isEqualTo("NaN");
        assertThat(v.getFloat64()).isNaN();

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float64(Double.NaN));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float64(), vPb));
    }

    @Test
    public void float64_inf() {
        {
            PrimitiveValue v = PrimitiveValue.float64(Double.POSITIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.float64(Double.POSITIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.float64(0.0));
            assertThat(v.toString()).isEqualTo("Infinity");
            assertThat(v.getFloat64()).isPositiveInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float64(Double.POSITIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float64(), vPb));
        }
        {
            PrimitiveValue v = PrimitiveValue.float64(Double.NEGATIVE_INFINITY);

            assertThat(v).isEqualTo(PrimitiveValue.float64(Double.NEGATIVE_INFINITY));
            assertThat(v).isNotEqualTo(PrimitiveValue.float64(0.0));
            assertThat(v.toString()).isEqualTo("-Infinity");
            assertThat(v.getFloat64()).isNegativeInfinity();

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.float64(Double.NEGATIVE_INFINITY));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.float64(), vPb));
        }
    }

    @Test
    public void string() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.string(data));
            assertThat(v).isEqualTo(PrimitiveValue.string(ByteString.copyFrom(data)));
            assertThat(v).isNotEqualTo(PrimitiveValue.string(ByteString.EMPTY));

            assertThat(v.toString()).isEqualTo("\"\\000\\007\\077\\177\\377\"");
            assertThat(v.getString()).isEqualTo(data);
            assertThat(v.getStringUnsafe()).isEqualTo(data);
            assertThat(v.getStringBytes()).isEqualTo(ByteString.copyFrom(data));

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.string(data));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.string(), vPb));
        };

        doTest.accept(PrimitiveValue.string(data));
        doTest.accept(PrimitiveValue.stringOwn(data));
        doTest.accept(PrimitiveValue.string(ByteString.copyFrom(data)));

        // hashes must be the same
        assertThat(PrimitiveValue.string(data).hashCode())
            .isEqualTo(PrimitiveValue.string(ByteString.copyFrom(data)).hashCode());
    }

    @Test
    public void yson() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.yson(data));
            assertThat(v).isEqualTo(PrimitiveValue.yson(ByteString.copyFrom(data)));
            assertThat(v).isNotEqualTo(PrimitiveValue.yson(ByteString.EMPTY));

            assertThat(v.toString()).isEqualTo("\"\\000\\007\\077\\177\\377\"");
            assertThat(v.getYson()).isEqualTo(data);
            assertThat(v.getYsonUnsafe()).isEqualTo(data);
            assertThat(v.getYsonBytes()).isEqualTo(ByteString.copyFrom(data));

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.yson(data));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.yson(), vPb));
        };

        doTest.accept(PrimitiveValue.yson(data));
        doTest.accept(PrimitiveValue.ysonOwn(data));
        doTest.accept(PrimitiveValue.yson(ByteString.copyFrom(data)));

        // hashes must be the same
        assertThat(PrimitiveValue.yson(data).hashCode())
            .isEqualTo(PrimitiveValue.yson(ByteString.copyFrom(data)).hashCode());
    }

    @Test
    public void stringIsNotEqualToYson() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        PrimitiveValue string = PrimitiveValue.string(data);
        PrimitiveValue yson = PrimitiveValue.yson(data);

        assertThat(string).isNotEqualTo(yson);
        assertThat(yson).isNotEqualTo(string);
        assertThat(string.hashCode()).isNotEqualTo(yson.hashCode());
    }

    @Test
    public void utf8() {
        String[] fixtures = {
            "Hello", "Hola", "Bonjour",
            "Ciao", "こんにちは", "안녕하세요",
            "Cześć", "Olá", "Здравствуйте",
            "Chào bạn", "您好", "Hallo",
            "Hej", "Ahoj", "سلام", "สวัสด"
        };

        for (String date : fixtures) {
            PrimitiveValue v = PrimitiveValue.utf8(date);

            assertThat(v).isEqualTo(PrimitiveValue.utf8(date));
            assertThat(v).isNotEqualTo(PrimitiveValue.utf8(""));

            assertThat(v.toString()).isEqualTo(String.format("\"%s\"", date));
            assertThat(v.getUtf8()).isEqualTo(date);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.utf8(date));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.utf8(), vPb));
        }
    }

    @Test
    public void json() {
        String data = "{\"name\": \"jamel\", \"age\": 99}";

        PrimitiveValue v = PrimitiveValue.json(data);

        assertThat(v).isEqualTo(PrimitiveValue.json(data));
        assertThat(v).isNotEqualTo(PrimitiveValue.json(""));

        assertThat(v.toString()).isEqualTo("\"{\\\"name\\\": \\\"jamel\\\", \\\"age\\\": 99}\"");
        assertThat(v.getJson()).isEqualTo(data);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.json(data));
        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.json(), vPb));
    }

    @Test
    public void uuid() {
        long low = 0x6677445500112233L, high = 0xffeeddccbbaa9988L;
        String uuidStr = "00112233-4455-6677-8899-aabbccddeeff";
        UUID uuid = UUID.fromString(uuidStr);

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.uuid(uuid));
            assertThat(v).isEqualTo(PrimitiveValue.uuid(uuidStr));
            assertThat(v).isEqualTo(PrimitiveValue.uuid(high, low));
            assertThat(v).isNotEqualTo(PrimitiveValue.uuid(UUID.randomUUID()));

            assertThat(v.toString()).isEqualTo("\"00112233-4455-6677-8899-aabbccddeeff\"");
            assertThat(v.getUuidString()).isEqualTo("00112233-4455-6677-8899-aabbccddeeff");
            assertThat(v.getUuidHigh()).isEqualTo(high);
            assertThat(v.getUuidLow()).isEqualTo(low);
            assertThat(v.getUuidJdk()).isEqualTo(uuid);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uuid(uuid));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.uuid(high, low));
            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.uuid(), vPb));
        };

        doTest.accept(PrimitiveValue.uuid(uuid));
        doTest.accept(PrimitiveValue.uuid(uuidStr));
        doTest.accept(PrimitiveValue.uuid(high, low));
    }

    @Test
    public void date() {
        long day = 17763L;
        LocalDate date = LocalDate.of(2018, Month.AUGUST, 20);
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.date(day));
            assertThat(v).isEqualTo(PrimitiveValue.date(date));
            assertThat(v).isEqualTo(PrimitiveValue.date(instant));
            assertThat(v).isNotEqualTo(PrimitiveValue.date(0));

            assertThat(v.toString()).isEqualTo("2018-08-20");
            assertThat(v.getDate()).isEqualTo(date);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.date(day));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.date(date));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.date(instant));

            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.date(), vPb));
        };

        doTest.accept(PrimitiveValue.date(day));
        doTest.accept(PrimitiveValue.date(date));
        doTest.accept(PrimitiveValue.date(instant));
    }

    @Test
    public void datetime() {
        long seconds = 1534728225L;
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");
        LocalDateTime localDateTime = LocalDateTime.of(2018, 8, 20, 1, 23, 45);

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.datetime(seconds));
            assertThat(v).isEqualTo(PrimitiveValue.datetime(instant));
            assertThat(v).isEqualTo(PrimitiveValue.datetime(localDateTime));
            assertThat(v).isNotEqualTo(PrimitiveValue.datetime(0));

            assertThat(v.toString()).isEqualTo("2018-08-20T01:23:45");
            assertThat(v.getDatetime()).isEqualTo(LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC));

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.datetime(seconds));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.datetime(instant));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.datetime(localDateTime));

            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.datetime(), vPb));
        };

        doTest.accept(PrimitiveValue.datetime(seconds));
        doTest.accept(PrimitiveValue.datetime(instant));
        doTest.accept(PrimitiveValue.datetime(localDateTime));
    }

    @Test
    public void timestamp() {
        long seconds = 1534728225678901L;
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");

        Consumer<PrimitiveValue> doTest = (v) -> {
            assertThat(v).isEqualTo(PrimitiveValue.timestamp(seconds));
            assertThat(v).isEqualTo(PrimitiveValue.timestamp(instant));
            assertThat(v).isNotEqualTo(PrimitiveValue.timestamp(0));

            assertThat(v.toString()).isEqualTo("2018-08-20T01:23:45.678901Z");
            assertThat(v.getTimestamp()).isEqualTo(instant);

            ValueProtos.Value vPb = v.toPb();
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.timestamp(seconds));
            ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.timestamp(instant));

            assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.timestamp(), vPb));
        };

        doTest.accept(PrimitiveValue.timestamp(seconds));
        doTest.accept(PrimitiveValue.timestamp(instant));
    }

    @Test
    public void interval() {
        Duration oneSecond = Duration.ofSeconds(1);
        PrimitiveValue v = PrimitiveValue.interval(oneSecond);

        assertThat(v).isEqualTo(PrimitiveValue.interval(oneSecond));
        assertThat(v).isNotEqualTo(PrimitiveValue.interval(0));

        assertThat(v.toString()).isEqualTo("PT1S");
        assertThat(v.getInterval()).isEqualTo(oneSecond);

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.interval(oneSecond));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.interval(), vPb));
    }

    @Test
    public void tzDate() {
        PrimitiveValue v = PrimitiveValue.tzDate(
            ZonedDateTime.of(LocalDate.of(2018, Month.AUGUST, 20),
            LocalTime.MIDNIGHT,
            ZoneId.of("Europe/Moscow")));

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.tzDate("2018-08-20,Europe/Moscow"));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.tzDate(), vPb));
    }

    @Test
    public void tzDatetime() {
        PrimitiveValue v = PrimitiveValue.tzDatetime(
            ZonedDateTime.of(LocalDate.of(2018, Month.SEPTEMBER, 21),
                LocalTime.of(12, 34, 56),
                ZoneId.of("Asia/Novosibirsk")));

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.tzDatetime("2018-09-21T12:34:56,Asia/Novosibirsk"));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.tzDatetime(), vPb));
    }

    @Test
    public void tzTimestamp() {
        PrimitiveValue v = PrimitiveValue.tzTimestamp(
            ZonedDateTime.of(LocalDate.of(2018, Month.OCTOBER, 22),
                LocalTime.of(12, 34, 56, (int) TimeUnit.MICROSECONDS.toNanos(778899)),
                ZoneId.of("America/Chicago")));

        ValueProtos.Value vPb = v.toPb();
        ProtoTruth.assertThat(vPb).isEqualTo(ProtoValue.tzDatetime("2018-10-22T12:34:56.778899,America/Chicago"));

        assertThat(v).isEqualTo(ProtoValue.fromPb(PrimitiveType.tzTimestamp(), vPb));
    }
}
