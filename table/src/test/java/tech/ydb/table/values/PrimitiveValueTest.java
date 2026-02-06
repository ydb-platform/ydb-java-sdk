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

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;



/**
 * @author Sergey Polovko
 */
public class PrimitiveValueTest {

    @Test
    public void bool() {
        PrimitiveValue v = PrimitiveValue.newBool(true);

        Assert.assertSame(PrimitiveValue.newBool(true), v);
        Assert.assertEquals(PrimitiveValue.newBool(true), v);
        Assert.assertNotSame(PrimitiveValue.newBool(false), v);
        Assert.assertNotEquals(PrimitiveValue.newBool(false), v);
        Assert.assertEquals("true", v.toString());
        Assert.assertTrue(v.getBool());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertSame(ProtoValue.fromBool(true), vPb);
        Assert.assertEquals(ProtoValue.fromBool(true), vPb);
        Assert.assertSame(v, ProtoValue.fromPb(PrimitiveType.Bool, vPb));
    }

    @Test
    public void int8() {
        PrimitiveValue v = PrimitiveValue.newInt8((byte) 1);

        Assert.assertEquals(PrimitiveValue.newInt8((byte) 1), v);
        Assert.assertNotEquals(PrimitiveValue.newInt8((byte) 0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals((byte) 1, v.getInt8());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromInt8((byte) 1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Int8, vPb).equals(v));
    }

    @Test
    public void uint8_positive() {
        PrimitiveValue v = PrimitiveValue.newUint8((byte) 1);

        Assert.assertEquals(PrimitiveValue.newUint8((byte) 1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint8((byte) 0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(1, v.getUint8());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint8((byte) 1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint8, vPb).equals(v));
    }

    @Test
    public void uint8_negative() {
        PrimitiveValue v = PrimitiveValue.newUint8((byte) -1);

        Assert.assertEquals(PrimitiveValue.newUint8((byte) -1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint8((byte) 0), v);
        Assert.assertEquals("255", v.toString()); // 0xff
        Assert.assertEquals(255, v.getUint8()); // 0xff

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint8((byte) -1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint8, vPb).equals(v));
    }

    @Test
    public void int16() {
        PrimitiveValue v = PrimitiveValue.newInt16((short) 1);

        Assert.assertEquals(PrimitiveValue.newInt16((short) 1), v);
        Assert.assertNotEquals(PrimitiveValue.newInt16((short) 0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals((short) 1, v.getInt16());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromInt16((short) 1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Int16, vPb).equals(v));
    }

    @Test
    public void uint16_positive() {
        PrimitiveValue v = PrimitiveValue.newUint16((short) 1);

        Assert.assertEquals(PrimitiveValue.newUint16((short) 1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint16((short) 0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(1, v.getUint16());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint16((short) 1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint16, vPb).equals(v));
    }

    @Test
    public void uint16_negative() {
        PrimitiveValue v = PrimitiveValue.newUint16((short) -1);

        Assert.assertEquals(PrimitiveValue.newUint16((short) -1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint16((short) 0), v);
        Assert.assertEquals("65535", v.toString()); // 0xffff
        Assert.assertEquals(65535, v.getUint16()); // 0xffff

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint16((short) -1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint16, vPb).equals(v));
    }

    @Test
    public void int32() {
        PrimitiveValue v = PrimitiveValue.newInt32(1);

        Assert.assertEquals(PrimitiveValue.newInt32(1), v);
        Assert.assertNotEquals(PrimitiveValue.newInt32(0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(1, v.getInt32());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromInt32(1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Int32, vPb).equals(v));
    }

    @Test
    public void uint32_positive() {
        PrimitiveValue v = PrimitiveValue.newUint32(1);

        Assert.assertEquals(PrimitiveValue.newUint32(1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint32(0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(1L, v.getUint32());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint32(1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint32, vPb).equals(v));
    }

    @Test
    public void uint32_negative() {
        PrimitiveValue v = PrimitiveValue.newUint32(-1);

        Assert.assertEquals(PrimitiveValue.newUint32(-1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint32(0), v);
        Assert.assertEquals("4294967295", v.toString()); // 0xffffffff
        Assert.assertEquals(4294967295L, v.getUint32()); // 0xffffffff

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint32(-1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint32, vPb).equals(v));
    }

    @Test
    public void int64() {
        PrimitiveValue v = PrimitiveValue.newInt64(1);

        Assert.assertEquals(PrimitiveValue.newInt64(1), v);
        Assert.assertNotEquals(PrimitiveValue.newInt64(0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(1L, v.getInt64());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromInt64(1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Int64, vPb).equals(v));
    }

    @Test
    public void uint64_positive() {
        PrimitiveValue v = PrimitiveValue.newUint64(1);

        Assert.assertEquals(PrimitiveValue.newUint64(1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint64(0), v);
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(1L, v.getUint64());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint64(1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint64, vPb).equals(v));
    }

    @Test
    public void uint64_negative() {
        PrimitiveValue v = PrimitiveValue.newUint64(-1);

        Assert.assertEquals(PrimitiveValue.newUint64(-1), v);
        Assert.assertNotEquals(PrimitiveValue.newUint64(0), v);
        Assert.assertEquals("18446744073709551615", v.toString()); // 0xffffffffffffffff
        Assert.assertEquals(v.getUint64(), Long.parseUnsignedLong("18446744073709551615")); // 0xffffffffffffffff

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromUint64(-1));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uint64, vPb).equals(v));
    }

    @Test
    public void floatTest() {
        PrimitiveValue v = PrimitiveValue.newFloat(3.14159f);

        Assert.assertEquals(PrimitiveValue.newFloat(3.14159f), v);
        Assert.assertNotEquals(PrimitiveValue.newFloat(0f), v);
        Assert.assertEquals("3.14159", v.toString());
        Assert.assertEquals(3.14159f, v.getFloat(), 1e-6f);

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromFloat(3.14159f));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Float, vPb).equals(v));
    }

    @Test
    public void float_nan() {
        PrimitiveValue v = PrimitiveValue.newFloat(Float.NaN);

        Assert.assertEquals(PrimitiveValue.newFloat(Float.NaN), v);
        Assert.assertNotEquals(PrimitiveValue.newFloat(0f), v);
        Assert.assertEquals("NaN", v.toString());
        Assert.assertTrue(Float.isNaN(v.getFloat()));

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromFloat(Float.NaN));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Float, vPb).equals(v));
    }

    @Test
    public void float_inf() {
        {
            PrimitiveValue v = PrimitiveValue.newFloat(Float.POSITIVE_INFINITY);

            Assert.assertEquals(PrimitiveValue.newFloat(Float.POSITIVE_INFINITY), v);
            Assert.assertNotEquals(PrimitiveValue.newFloat(0f), v);
            Assert.assertEquals("Infinity", v.toString());
            Assert.assertTrue(Float.POSITIVE_INFINITY == v.getFloat());

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromFloat(Float.POSITIVE_INFINITY));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Float, vPb).equals(v));
        }
        {
            PrimitiveValue v = PrimitiveValue.newFloat(Float.NEGATIVE_INFINITY);

            Assert.assertEquals(PrimitiveValue.newFloat(Float.NEGATIVE_INFINITY), v);
            Assert.assertNotEquals(PrimitiveValue.newFloat(0f), v);
            Assert.assertEquals("-Infinity", v.toString());
            Assert.assertTrue(Float.NEGATIVE_INFINITY == v.getFloat());

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromFloat(Float.NEGATIVE_INFINITY));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Float, vPb).equals(v));
        }
    }

    @Test
    public void doubleTest() {
        PrimitiveValue v = PrimitiveValue.newDouble(3.14159);

        Assert.assertEquals(PrimitiveValue.newDouble(3.14159), v);
        Assert.assertNotEquals(PrimitiveValue.newDouble(0.0), v);
        Assert.assertEquals("3.14159", v.toString());
        Assert.assertEquals(3.14159, v.getDouble(), 1e-6);

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromDouble(3.14159));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Double, vPb).equals(v));
    }

    @Test
    public void double_nan() {
        PrimitiveValue v = PrimitiveValue.newDouble(Double.NaN);

        Assert.assertEquals(PrimitiveValue.newDouble(Double.NaN), v);
        Assert.assertNotEquals(PrimitiveValue.newDouble(0.0), v);
        Assert.assertEquals("NaN", v.toString());
        Assert.assertTrue(Double.isNaN(v.getDouble()));

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromDouble(Double.NaN));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Double, vPb).equals(v));
    }

    @Test
    public void double_inf() {
        {
            PrimitiveValue v = PrimitiveValue.newDouble(Double.POSITIVE_INFINITY);

            Assert.assertEquals(PrimitiveValue.newDouble(Double.POSITIVE_INFINITY), v);
            Assert.assertNotEquals(PrimitiveValue.newDouble(0.0), v);
            Assert.assertEquals("Infinity", v.toString());
            Assert.assertTrue(Double.POSITIVE_INFINITY == v.getDouble());

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromDouble(Double.POSITIVE_INFINITY));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Double, vPb).equals(v));
        }
        {
            PrimitiveValue v = PrimitiveValue.newDouble(Double.NEGATIVE_INFINITY);

            Assert.assertEquals(PrimitiveValue.newDouble(Double.NEGATIVE_INFINITY), v);
            Assert.assertNotEquals(PrimitiveValue.newDouble(0.0), v);
            Assert.assertEquals("-Infinity", v.toString());
            Assert.assertTrue(Double.NEGATIVE_INFINITY == v.getDouble());

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromDouble(Double.NEGATIVE_INFINITY));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Double, vPb).equals(v));
        }
    }

    @Test
    public void bytes() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };
        byte[] other = { 0x0, 0x7, 0x34, 0x7f, (byte) 0xff };

        Consumer<PrimitiveValue> doTest = (v) -> {
            Assert.assertEquals(v, v);

            Assert.assertNotEquals(v, null);
            Assert.assertNotEquals(v, PrimitiveValue.newYson(data));
            Assert.assertNotEquals(v, PrimitiveValue.newInt32(1));

            Assert.assertEquals(PrimitiveValue.newBytes(data), v);
            Assert.assertEquals(PrimitiveValue.newBytes(ByteString.copyFrom(data)), v);
            Assert.assertNotEquals(PrimitiveValue.newBytes(ByteString.EMPTY), v);
            Assert.assertNotEquals(PrimitiveValue.newBytes(other), v);

            Assert.assertEquals("Bytes[len=5 content=00073f7fff]", v.toString());
            Assert.assertArrayEquals(data, v.getBytes());
            Assert.assertArrayEquals(data, v.getBytesUnsafe());

            Assert.assertNotSame(v.getBytes(), v.getBytes());
            Assert.assertSame(v.getBytesUnsafe(), v.getBytesUnsafe());

            Assert.assertEquals(ByteString.copyFrom(data), v.getBytesAsByteString());

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromBytes(data));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Bytes, vPb).equals(v));
        };

        doTest.accept(PrimitiveValue.newBytes(data));
        doTest.accept(PrimitiveValue.newBytesOwn(data));
        doTest.accept(PrimitiveValue.newBytes(ByteString.copyFrom(data)));

        // hashes must be the same
        Assert.assertEquals(PrimitiveValue.newBytes(data).hashCode(),
                PrimitiveValue.newBytes(ByteString.copyFrom(data)).hashCode());

        Assert.assertEquals("Bytes[len=0]", PrimitiveValue.newBytes(ByteString.EMPTY).toString());

        // toString must cut too long value
        PrimitiveValue bytes51 = PrimitiveValue.newBytes(ByteString.fromHex(""
                + "000102030405060708090A0B0C0D0E0F"
                + "101112131415161718191A1B1C1D1E1F"
                + "202122232425262728292A2B2C2D2E2F"
                + "303132"
        ));
        Assert.assertEquals("Bytes[len=51 content="
                + "000102030405060708090a0b0c0d0e0f"
                + "101112131415161718191a1b1c1d1e1f"
                + "202122232425262728292a2b2c2d2e..."
                + "]", bytes51.toString());
    }

    @Test
    public void yson() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };
        byte[] other = { 0x0, 0x7, 0x34, 0x7f, (byte) 0xff };

        Consumer<PrimitiveValue> doTest = (v) -> {
            Assert.assertEquals(v, v);

            Assert.assertNotEquals(v, null);
            Assert.assertNotEquals(v, PrimitiveValue.newBytes(data));
            Assert.assertNotEquals(v, PrimitiveValue.newInt32(1));

            Assert.assertEquals(PrimitiveValue.newYson(data), v);
            Assert.assertEquals(PrimitiveValue.newYson(ByteString.copyFrom(data)), v);
            Assert.assertNotEquals(PrimitiveValue.newYson(ByteString.EMPTY), v);
            Assert.assertNotEquals(PrimitiveValue.newYson(other), v);

            Assert.assertEquals("Yson[len=5 content=00073f7fff]", v.toString());

            Assert.assertArrayEquals(data, v.getYson());
            Assert.assertArrayEquals(data, v.getYsonUnsafe());

            Assert.assertNotSame(v.getYson(), v.getYson());
            Assert.assertSame(v.getYsonUnsafe(), v.getYsonUnsafe());

            Assert.assertEquals(ByteString.copyFrom(data), v.getYsonBytes());

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromYson(data));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Yson, vPb).equals(v));
        };

        doTest.accept(PrimitiveValue.newYson(data));
        doTest.accept(PrimitiveValue.newYsonOwn(data));
        doTest.accept(PrimitiveValue.newYson(ByteString.copyFrom(data)));

        // hashes must be the same
        Assert.assertEquals(PrimitiveValue.newYson(data).hashCode(),
                PrimitiveValue.newYson(ByteString.copyFrom(data)).hashCode());

        Assert.assertEquals("Yson[len=0]", PrimitiveValue.newYson(ByteString.EMPTY).toString());

        // toString must cut too long value
        PrimitiveValue yson51 = PrimitiveValue.newYson(ByteString.fromHex(""
                + "000102030405060708090A0B0C0D0E0F"
                + "101112131415161718191A1B1C1D1E1F"
                + "202122232425262728292A2B2C2D2E2F"
                + "303132"
        ));
        Assert.assertEquals("Yson[len=51 content="
                + "000102030405060708090a0b0c0d0e0f"
                + "101112131415161718191a1b1c1d1e1f"
                + "202122232425262728292a2b2c2d2e..."
                + "]", yson51.toString());
    }

    @Test
    public void stringIsNotEqualToYson() {
        byte[] data = { 0x0, 0x7, 0x3f, 0x7f, (byte) 0xff };

        PrimitiveValue string = PrimitiveValue.newBytes(data);
        PrimitiveValue yson = PrimitiveValue.newYson(data);

        Assert.assertNotEquals(string, yson);
        Assert.assertNotEquals(yson, string);
        Assert.assertNotEquals(string.hashCode(), yson.hashCode());
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

            Assert.assertEquals(PrimitiveValue.newText(date), v);
            Assert.assertNotEquals(PrimitiveValue.newText(""), v);

            Assert.assertEquals(v.toString(), String.format("\"%s\"", date));
            Assert.assertEquals(v.getText(), date);

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromText(date));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Text, vPb).equals(v));
        }
    }

    @Test
    public void json() {
        String data = "{\"name\": \"jamel\", \"age\": 99}";

        PrimitiveValue v = PrimitiveValue.newJson(data);

        Assert.assertEquals(PrimitiveValue.newJson(data), v);
        Assert.assertNotEquals(PrimitiveValue.newJson(""), v);

        Assert.assertEquals("\"{\\\"name\\\": \\\"jamel\\\", \\\"age\\\": 99}\"", v.toString());
        Assert.assertEquals(data, v.getJson());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromJson(data));
        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Json, vPb).equals(v));
    }

    @Test
    public void uuid() {
        long low = 0x6677445500112233L, high = 0xffeeddccbbaa9988L;
        String uuidStr = "00112233-4455-6677-8899-aabbccddeeff";
        UUID uuid = UUID.fromString(uuidStr);

        Consumer<PrimitiveValue> doTest = (v) -> {
            Assert.assertEquals(PrimitiveValue.newUuid(uuid), v);
            Assert.assertEquals(PrimitiveValue.newUuid(uuidStr), v);
            Assert.assertNotEquals(PrimitiveValue.newUuid(UUID.randomUUID()), v);

            Assert.assertEquals("\"00112233-4455-6677-8899-aabbccddeeff\"", v.toString());
            Assert.assertEquals("00112233-4455-6677-8899-aabbccddeeff", v.getUuidString());
            Assert.assertEquals(v.getUuidJdk(), uuid);

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromUuid(uuid));
            Assert.assertEquals(vPb, ProtoValue.fromUuid(high, low));
            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Uuid, vPb).equals(v));
        };

        doTest.accept(PrimitiveValue.newUuid(uuid));
        doTest.accept(PrimitiveValue.newUuid(uuidStr));
    }

    @Test
    public void date() {
        long day = 17763L;
        LocalDate date = LocalDate.of(2018, Month.AUGUST, 20);
        Instant instant = Instant.parse("2018-08-20T01:23:45.678901Z");

        Consumer<PrimitiveValue> doTest = (v) -> {
            Assert.assertEquals(PrimitiveValue.newDate(day), v);
            Assert.assertEquals(PrimitiveValue.newDate(date), v);
            Assert.assertEquals(PrimitiveValue.newDate(instant), v);
            Assert.assertNotEquals(PrimitiveValue.newDate(0), v);

            Assert.assertEquals("2018-08-20", v.toString());
            Assert.assertEquals(v.getDate(), date);

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromDate(day));
            Assert.assertEquals(vPb, ProtoValue.fromDate(date));
            Assert.assertEquals(vPb, ProtoValue.fromDate(instant));

            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Date, vPb).equals(v));
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
            Assert.assertEquals(PrimitiveValue.newDatetime(seconds), v);
            Assert.assertEquals(PrimitiveValue.newDatetime(instant), v);
            Assert.assertEquals(PrimitiveValue.newDatetime(localDateTime), v);
            Assert.assertNotEquals(PrimitiveValue.newDatetime(0), v);

            Assert.assertEquals("2018-08-20T01:23:45", v.toString());
            Assert.assertEquals(v.getDatetime(), LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC));

            ValueProtos.Value vPb = v.toPb();
            Assert.assertEquals(vPb, ProtoValue.fromDatetime(seconds));
            Assert.assertEquals(vPb, ProtoValue.fromDatetime(instant));
            Assert.assertEquals(vPb, ProtoValue.fromDatetime(localDateTime));

            Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Datetime, vPb).equals(v));
        };

        doTest.accept(PrimitiveValue.newDatetime(seconds));
        doTest.accept(PrimitiveValue.newDatetime(instant));
        doTest.accept(PrimitiveValue.newDatetime(localDateTime));
    }

    @Test
    public void timestamp() {
        PrimitiveValue min = PrimitiveValue.newTimestamp(Instant.EPOCH);
        Assert.assertEquals(min, PrimitiveValue.newTimestamp(0));
        ValueProtos.Value minValue = min.toPb();

        Assert.assertEquals(0, minValue.getUint32Value());
        Assert.assertEquals(0, minValue.getUint64Value());
        Assert.assertEquals(0, minValue.getInt32Value());
        Assert.assertEquals(0, minValue.getInt64Value());
        Assert.assertEquals(0, minValue.getLow128());
        Assert.assertEquals(0, minValue.getHigh128());

        PrimitiveValue max = PrimitiveValue.newTimestamp(Instant.parse("2105-12-31T23:59:59.999999Z"));
        Assert.assertEquals(max, PrimitiveValue.newTimestamp(4291747199999999l));
        ValueProtos.Value maxValue = max.toPb();

        Assert.assertEquals(0, maxValue.getUint32Value());
        Assert.assertEquals(4291747199999999l, maxValue.getUint64Value());
        Assert.assertEquals(0, maxValue.getInt32Value());
        Assert.assertEquals(0, maxValue.getInt64Value());
        Assert.assertEquals(0, maxValue.getLow128());
        Assert.assertEquals(0, maxValue.getHigh128());

        Assert.assertEquals(
                "microsSinceEpoch value is before minimum timestamp(1970-01-01 00:00:00.000000): -1",
                Assert.assertThrows(IllegalArgumentException.class,
                        () -> PrimitiveValue.newTimestamp(-1)
                ).getMessage()
        );
        Assert.assertEquals(
                "Instant value is before minimum timestamp(1970-01-01 00:00:00.000000): 1969-12-31T23:59:59.999999999Z",
                Assert.assertThrows(IllegalArgumentException.class,
                        () -> PrimitiveValue.newTimestamp(Instant.EPOCH.minusNanos(1))
                ).getMessage()
        );
        Assert.assertEquals(
                "microsSinceEpoch value is after maximum timestamp(2105-12-31 23:59:59.999999): 4291747200000000",
                Assert.assertThrows(IllegalArgumentException.class,
                        () -> PrimitiveValue.newTimestamp(4291747200000000l)
                ).getMessage()
        );
        Assert.assertEquals(
                "Instant value is after maximum timestamp(2105-12-31 23:59:59.999999): 2106-01-01T00:00:00Z",
                Assert.assertThrows(IllegalArgumentException.class,
                        () -> PrimitiveValue.newTimestamp(Instant.parse("2106-01-01T00:00:00.000000Z"))
                ).getMessage()
        );
    }

    @Test
    public void interval() {
        Duration oneSecond = Duration.ofSeconds(1);
        PrimitiveValue v = PrimitiveValue.newInterval(oneSecond);

        Assert.assertEquals(PrimitiveValue.newInterval(oneSecond), v);
        Assert.assertNotEquals(PrimitiveValue.newInterval(0), v);

        Assert.assertEquals("PT1S", v.toString());
        Assert.assertEquals(oneSecond, v.getInterval());

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromInterval(oneSecond));

        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.Interval, vPb).equals(v));
    }

    @Test
    public void tzDate() {
        PrimitiveValue v = PrimitiveValue.newTzDate(
            ZonedDateTime.of(LocalDate.of(2018, Month.AUGUST, 20),
            LocalTime.MIDNIGHT,
            ZoneId.of("Europe/Moscow")));

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromTzDate("2018-08-20,Europe/Moscow"));

        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.TzDate, vPb).equals(v));
    }

    @Test
    public void tzDatetime() {
        PrimitiveValue v = PrimitiveValue.newTzDatetime(
            ZonedDateTime.of(LocalDate.of(2018, Month.SEPTEMBER, 21),
                LocalTime.of(12, 34, 56),
                ZoneId.of("Asia/Novosibirsk")));

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromTzDatetime("2018-09-21T12:34:56,Asia/Novosibirsk"));

        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.TzDatetime, vPb).equals(v));
    }

    @Test
    public void tzTimestamp() {
        PrimitiveValue v = PrimitiveValue.newTzTimestamp(
            ZonedDateTime.of(LocalDate.of(2018, Month.OCTOBER, 22),
                LocalTime.of(12, 34, 56, (int) TimeUnit.MICROSECONDS.toNanos(778899)),
                ZoneId.of("America/Chicago")));

        ValueProtos.Value vPb = v.toPb();
        Assert.assertEquals(vPb, ProtoValue.fromTzDatetime("2018-10-22T12:34:56.778899,America/Chicago"));

        Assert.assertTrue(ProtoValue.fromPb(PrimitiveType.TzTimestamp, vPb).equals(v));
    }
}
