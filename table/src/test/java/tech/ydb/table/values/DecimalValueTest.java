package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;



/**
 * @author Sergey Polovko
 */
public class DecimalValueTest {

    @Test
    public void contract() {
        DecimalType type = DecimalType.of(13, 2);
        DecimalValue value = DecimalValue.fromBits(type, 0x0001, 0x0002);

        Assert.assertEquals(DecimalType.of(13, 2), value.getType());
        Assert.assertEquals(0x0001, value.getHigh());
        Assert.assertEquals(0x0002, value.getLow());

        // equals
        Assert.assertEquals(value, DecimalValue.fromBits(type, 0x0001, 0x0002));
        Assert.assertEquals(value, DecimalValue.fromBits(DecimalType.of(13, 2), 0x0001, 0x0002));

        Assert.assertNotEquals(value, DecimalValue.fromBits(type, 0x0001, 0x0003));
        Assert.assertNotEquals(value, DecimalValue.fromBits(type, 0x0002, 0x0002));
        Assert.assertNotEquals(value, DecimalValue.fromBits(DecimalType.of(12, 2), 0x0001, 0x0002));
        Assert.assertNotEquals(value, DecimalValue.fromBits(DecimalType.of(13, 1), 0x0001, 0x0002));

        // hashCode
        Assert.assertEquals(value.hashCode(), DecimalValue.fromBits(type, 0x0001, 0x0002).hashCode());
        Assert.assertEquals(value.hashCode(), DecimalValue.fromBits(DecimalType.of(13, 2), 0x0001, 0x0002).hashCode());

        Assert.assertNotEquals(value.hashCode(), DecimalValue.fromBits(type, 0x0001, 0x0003).hashCode());
        Assert.assertNotEquals(value.hashCode(), DecimalValue.fromBits(type, 0x0002, 0x0002).hashCode());
        Assert.assertNotEquals(value.hashCode(), DecimalValue.fromBits(DecimalType.of(12, 2), 0x0001, 0x0002).hashCode());
        Assert.assertNotEquals(value.hashCode(), DecimalValue.fromBits(DecimalType.of(13, 1), 0x0001, 0x0002).hashCode());
    }

    @Test
    public void protobuf() {
        DecimalType type = DecimalType.of(13, 2);
        DecimalValue value = DecimalValue.fromBits(type, 0x0001, 0x0002);

        ValueProtos.Value valuePb = value.toPb();
        Assert.assertEquals(ProtoValue.fromDecimal(0x0001, 0x0002), valuePb);

        Assert.assertTrue(ProtoValue.fromPb(type, valuePb).equals(value));
    }

    @Test
    public void decimalScale() {
        DecimalType type = DecimalType.of(30, 2);
        DecimalValue value1 = DecimalValue.fromBits(type, 0x0000, 0x0002);

        Assert.assertEquals("0.02", value1.toString());
        Assert.assertEquals("2", value1.toUnscaledString());

        // 2^32 = 18446744073709551616
        DecimalValue value2 = DecimalValue.fromBits(type, 0x0001, 0x0002);
        Assert.assertEquals("184467440737095516.18", value2.toString());
        Assert.assertEquals("18446744073709551618", value2.toUnscaledString());
    }

    @Test
    public void inf() {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION);
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.newValue(inf);
            Assert.assertTrue(value.isInf());
            Assert.assertFalse(value.isNegative());
            Assert.assertEquals(DecimalValue.INF, value);
            inf = inf.add(k);
        }
    }

    @Test
    public void infDefaulttype() {
        DecimalType type = DecimalType.getDefault();
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.newValue(inf);
            Assert.assertTrue(value.isInf());
            Assert.assertFalse(value.isNegative());
            Assert.assertNotEquals(DecimalValue.INF, value);
            inf = inf.add(k);
        }
    }

    @Test
    public void negativeInf() {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION);
        BigInteger inf = BigInteger.TEN.negate().pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.newValue(inf);
            Assert.assertTrue(value.isNegativeInf());
            Assert.assertTrue(value.isNegative());
            Assert.assertEquals(DecimalValue.NEG_INF, value);
            inf = inf.subtract(k);
        }
    }

    @Test
    public void negativeInfDefaultType() {
        DecimalType type = DecimalType.getDefault();
        BigInteger inf = BigInteger.TEN.negate().pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.newValue(inf);
            Assert.assertTrue(value.isNegativeInf());
            Assert.assertTrue(value.isNegative());
            Assert.assertNotEquals(DecimalValue.NEG_INF, value);
            inf = inf.subtract(k);
        }
    }

    @Test
    public void zero() {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION);

        Assert.assertTrue(type.newValue(0).isZero());
        Assert.assertEquals(BigDecimal.ZERO, type.newValue(0).toBigDecimal());
        Assert.assertEquals(BigInteger.ZERO, type.newValue(0).toBigInteger());
        Assert.assertEquals("0", type.newValue(0).toString());

        Assert.assertEquals(type.newValue(0, 0), type.newValue(0));
        Assert.assertEquals(type.newValue("0"), type.newValue(0));
        Assert.assertEquals(type.newValueUnsigned(0), type.newValue(0));
        Assert.assertEquals(type.newValue(BigInteger.ZERO), type.newValue(0));
        Assert.assertEquals(type.newValue(BigDecimal.ZERO), type.newValue(0));

        DecimalType ydb = DecimalType.getDefault();
        Assert.assertTrue(ydb.newValue(0).isZero());
        Assert.assertNotEquals(BigDecimal.ZERO, ydb.newValue(0).toBigDecimal());
        Assert.assertEquals(ydb.newValue(0).toBigDecimal(), BigDecimal.ZERO.setScale(ydb.getScale()));
        Assert.assertEquals(ydb.newValue(0).toBigInteger(), BigInteger.ZERO);
    }

    @Test
    public void ofString() {
        DecimalType t = DecimalType.getDefault();

        Assert.assertTrue(t.newValue("inf").isInf());
        Assert.assertTrue(t.newValue("Inf").isInf());
        Assert.assertTrue(t.newValue("INF").isInf());
        Assert.assertTrue(t.newValue("+inf").isInf());
        Assert.assertTrue(t.newValue("+Inf").isInf());
        Assert.assertTrue(t.newValue("+INF").isInf());
        Assert.assertTrue(t.newValue("-inf").isNegativeInf());
        Assert.assertTrue(t.newValue("-Inf").isNegativeInf());
        Assert.assertTrue(t.newValue("-INF").isNegativeInf());
        Assert.assertTrue(t.newValue("nan").isNan());
        Assert.assertTrue(t.newValue("Nan").isNan());
        Assert.assertTrue(t.newValue("NaN").isNan());
        Assert.assertTrue(t.newValue("NAN").isNan());

        Assert.assertTrue(t.newValue("0").isZero());
        Assert.assertTrue(t.newValue("00").isZero());
        Assert.assertTrue(t.newValue("0.0").isZero());
        Assert.assertTrue(t.newValue("00.00").isZero());

        Assert.assertEquals(t.newValue("1"), t.newValue(1));
        Assert.assertEquals(t.newValue("12"), t.newValue(12));
        Assert.assertEquals(t.newValue("123"), t.newValue(123));
        Assert.assertEquals(t.newValue("1234"), t.newValue(1234));
        Assert.assertEquals(t.newValue("12345"), t.newValue(12345));
        Assert.assertEquals(t.newValue("123456"), t.newValue(123456));
        Assert.assertEquals(t.newValue("1234567"), t.newValue(1234567));
        Assert.assertEquals(t.newValue("12345678"), t.newValue(12345678));
        Assert.assertEquals(t.newValue("123456789"), t.newValue(123456789));
        Assert.assertEquals(t.newValue("1234567890"), t.newValue(1234567890));
        Assert.assertEquals(t.newValue("12345678901"), t.newValue(12345678901L));
        Assert.assertEquals(t.newValue("1234567890123456789"), t.newValue(1234567890123456789L));

        Assert.assertEquals(t.newValue("-1"), t.newValue(-1));
        Assert.assertEquals(t.newValue("-12"), t.newValue(-12));
        Assert.assertEquals(t.newValue("-123"), t.newValue(-123));
        Assert.assertEquals(t.newValue("-1234"), t.newValue(-1234));
        Assert.assertEquals(t.newValue("-12345"), t.newValue(-12345));
        Assert.assertEquals(t.newValue("-123456"), t.newValue(-123456));
        Assert.assertEquals(t.newValue("-1234567"), t.newValue(-1234567));
        Assert.assertEquals(t.newValue("-12345678"), t.newValue(-12345678));
        Assert.assertEquals(t.newValue("-123456789"), t.newValue(-123456789));
        Assert.assertEquals(t.newValue("-1234567890"), t.newValue(-1234567890));
        Assert.assertEquals(t.newValue("-12345678901"), t.newValue(-12345678901L));
        Assert.assertEquals(t.newValue("-1234567890123456789"), t.newValue(-1234567890123456789L));

        DecimalType t2 = DecimalType.of(DecimalType.MAX_PRECISION, 3);
        // fraction part is smaller than scale
        Assert.assertEquals(t2.newValue("12345678.9"), DecimalValue.fromUnscaledLong(t2, 12345678900L));
        // fraction part is bigger than scale
        Assert.assertEquals(t2.newValue("123456.789"), DecimalValue.fromUnscaledLong(t2, 123456789L));
    }

    @Test
    public void ofUnsigned() {
        DecimalType t = DecimalType.getDefault();
        String zeros = "." + String.join("", Collections.nCopies(t.getScale(), "0"));

        Assert.assertTrue(t.newValueUnsigned(0).isZero());
        Assert.assertEquals(t.newValueUnsigned(1).toString(), "1" + zeros);
        Assert.assertEquals(t.newValueUnsigned(Long.MAX_VALUE).toString(), Long.toUnsignedString(Long.MAX_VALUE) + zeros);
        Assert.assertEquals(t.newValueUnsigned(Long.MIN_VALUE).toString(), Long.toUnsignedString(Long.MIN_VALUE) + zeros);
        Assert.assertEquals(t.newValueUnsigned(-1).toString(), Long.toUnsignedString(-1) + zeros);

        BigInteger maxUint64 = BigInteger.valueOf(0xffffffffL).shiftLeft(32).or(BigInteger.valueOf(0xffffffffL));

        BigInteger unscaledMaxUint64 = maxUint64.multiply(BigInteger.TEN.pow(t.getScale()));

        Assert.assertEquals(t.newValueUnsigned(-1).toBigInteger(), maxUint64);
        Assert.assertEquals(t.newValueUnsigned(-1).toUnscaledBigInteger(), unscaledMaxUint64);
        Assert.assertEquals(t.newValueUnsigned(-1).toBigDecimal(), new BigDecimal(unscaledMaxUint64, t.getScale()));
    }

    @Test
    public void ofLong() {
        DecimalType t = DecimalType.getDefault();
        String zeros = "." + String.join("", Collections.nCopies(t.getScale(), "0"));

        Assert.assertTrue(t.newValue(0).isZero());
        Assert.assertEquals(t.newValue(1).toString(), "1" + zeros);
        Assert.assertEquals(t.newValue(Long.MAX_VALUE).toString(), Long.toString(Long.MAX_VALUE) + zeros);
        Assert.assertEquals(t.newValue(Long.MIN_VALUE).toString(), Long.toString(Long.MIN_VALUE) + zeros);
        Assert.assertEquals(t.newValue(-1).toString(), Long.toString(-1) + zeros);

        BigInteger rawMinusOne = BigInteger.valueOf(-1)
                .multiply(BigInteger.TEN.pow(t.getScale()));

        Assert.assertEquals(t.newValue(-1).toBigInteger(), BigInteger.valueOf(-1));
        Assert.assertEquals(t.newValue(-1).toUnscaledBigInteger(), rawMinusOne);
        Assert.assertEquals(t.newValue(-1).toBigDecimal(), BigDecimal.valueOf(-1).setScale(t.getScale()));
        Assert.assertEquals(t.newValue(-1).toBigDecimal(), new BigDecimal(rawMinusOne, t.getScale()));
    }

    @Test
    public void unscaledType() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION);

        // zero
        Assert.assertTrue(t.newValue(0).isZero());

        // one
        Assert.assertEquals(t.newValue(1), t.newValueUnscaled(1));
        Assert.assertEquals(t.newValue(1), t.newValueUnsigned(1));

        Assert.assertEquals(0, t.newValue(1).getHigh());
        Assert.assertEquals(1, t.newValue(1).getLow());
        Assert.assertEquals(1, t.newValue(1).toLong());
        Assert.assertEquals(t.newValue(1).toBigDecimal(), BigDecimal.ONE);
        Assert.assertEquals(t.newValue(1).toBigInteger(), BigInteger.ONE);
        Assert.assertEquals(t.newValue(1).toUnscaledBigInteger(), BigInteger.ONE);
        Assert.assertEquals("1", t.newValue(1).toString());
        Assert.assertEquals("1", t.newValue(1).toUnscaledString());

        // -1 equals 0xFFFFFFFFFFFFFFFF or 18446744073709551615
        Assert.assertEquals(t.newValue(-1), t.newValueUnscaled(-1));
        Assert.assertNotEquals(t.newValue(-1), t.newValueUnsigned(-1));

        Assert.assertEquals(-1, t.newValue(-1).getHigh());
        Assert.assertEquals(-1, t.newValue(-1).getLow());
        Assert.assertEquals(-1, t.newValue(-1).toLong());
        Assert.assertEquals(BigDecimal.ONE.negate(), t.newValue(-1).toBigDecimal());
        Assert.assertEquals(BigInteger.ONE.negate(), t.newValue(-1).toBigInteger());
        Assert.assertEquals(BigInteger.ONE.negate(), t.newValue(-1).toUnscaledBigInteger());
        Assert.assertEquals("-1", t.newValue(-1).toString());
        Assert.assertEquals("-1", t.newValue(-1).toUnscaledString());

        Assert.assertEquals(0, t.newValueUnsigned(-1).getHigh());
        Assert.assertEquals(-1, t.newValueUnsigned(-1).getLow());
        Assert.assertEquals(new BigDecimal("18446744073709551615"), t.newValueUnsigned(-1).toBigDecimal());
        Assert.assertEquals(new BigInteger("18446744073709551615"), t.newValueUnsigned(-1).toBigInteger());
        Assert.assertEquals(new BigInteger("18446744073709551615"), t.newValueUnsigned(-1).toUnscaledBigInteger());
        Assert.assertEquals("18446744073709551615", t.newValueUnsigned(-1).toString());
        Assert.assertEquals("18446744073709551615", t.newValueUnsigned(-1).toUnscaledString());

        // Long.MAX_VALUE equals 0x7fffffffffffffff or 9223372036854775807
        Assert.assertEquals(t.newValue(Long.MAX_VALUE), t.newValueUnscaled(Long.MAX_VALUE));
        Assert.assertEquals(t.newValue(Long.MAX_VALUE), t.newValueUnsigned(Long.MAX_VALUE));

        Assert.assertEquals(0, t.newValue(Long.MAX_VALUE).getHigh());
        Assert.assertEquals(Long.MAX_VALUE, t.newValue(Long.MAX_VALUE).getLow());
        Assert.assertEquals(Long.MAX_VALUE, t.newValue(Long.MAX_VALUE).toLong());
        Assert.assertEquals(new BigDecimal("9223372036854775807"), t.newValue(Long.MAX_VALUE).toBigDecimal());
        Assert.assertEquals(new BigInteger("9223372036854775807"), t.newValue(Long.MAX_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("9223372036854775807"), t.newValue(Long.MAX_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("9223372036854775807", t.newValue(Long.MAX_VALUE).toString());
        Assert.assertEquals("9223372036854775807", t.newValue(Long.MAX_VALUE).toUnscaledString());

        // Long.MIN_VALUE equals 0x8000000000000000 or -9223372036854775808 for signed or 9223372036854775808 for unsigned
        Assert.assertEquals(t.newValue(Long.MIN_VALUE), t.newValueUnscaled(Long.MIN_VALUE));
        Assert.assertNotEquals(t.newValue(Long.MIN_VALUE), t.newValueUnsigned(Long.MIN_VALUE));

        Assert.assertEquals(-1, t.newValue(Long.MIN_VALUE).getHigh());
        Assert.assertEquals(Long.MIN_VALUE, t.newValue(Long.MIN_VALUE).getLow());
        Assert.assertEquals(Long.MIN_VALUE, t.newValue(Long.MIN_VALUE).toLong());
        Assert.assertEquals(new BigDecimal("-9223372036854775808"), t.newValue(Long.MIN_VALUE).toBigDecimal());
        Assert.assertEquals(new BigInteger("-9223372036854775808"), t.newValue(Long.MIN_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("-9223372036854775808"), t.newValue(Long.MIN_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("-9223372036854775808", t.newValue(Long.MIN_VALUE).toString());
        Assert.assertEquals("-9223372036854775808", t.newValue(Long.MIN_VALUE).toUnscaledString());

        Assert.assertEquals(0, t.newValueUnsigned(Long.MIN_VALUE).getHigh());
        Assert.assertEquals(Long.MIN_VALUE, t.newValueUnsigned(Long.MIN_VALUE).getLow());
        Assert.assertEquals(new BigDecimal("9223372036854775808"), t.newValueUnsigned(Long.MIN_VALUE).toBigDecimal());
        Assert.assertEquals(new BigInteger("9223372036854775808"), t.newValueUnsigned(Long.MIN_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("9223372036854775808"), t.newValueUnsigned(Long.MIN_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("9223372036854775808", t.newValueUnsigned(Long.MIN_VALUE).toString());
        Assert.assertEquals("9223372036854775808", t.newValueUnsigned(Long.MIN_VALUE).toUnscaledString());
    }

    @Test
    public void scaledType() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION, 15);

        // zero
        Assert.assertTrue(t.newValue(0).isZero());

        // one equals unscaled 10^15 = 0x00038D7EA4C68000
        Assert.assertNotEquals(t.newValue(1), t.newValueUnscaled(1));
        Assert.assertEquals(t.newValue(1), t.newValueUnsigned(1));

        Assert.assertEquals(0, t.newValue(1).getHigh());
        Assert.assertEquals(0x00038D7EA4C68000L, t.newValue(1).getLow());
        Assert.assertEquals(1, t.newValue(1).toLong());
        Assert.assertEquals(BigDecimal.ONE.setScale(15), t.newValue(1).toBigDecimal());
        Assert.assertEquals(BigInteger.ONE, t.newValue(1).toBigInteger());
        Assert.assertEquals(new BigInteger("1000000000000000"), t.newValue(1).toUnscaledBigInteger());
        Assert.assertEquals("1.000000000000000", t.newValue(1).toString());
        Assert.assertEquals("1000000000000000", t.newValue(1).toUnscaledString());

        Assert.assertEquals(0, t.newValueUnscaled(1).getHigh());
        Assert.assertEquals(1, t.newValueUnscaled(1).getLow());
        Assert.assertEquals(0, t.newValueUnscaled(1).toLong()); // round 1^-15 to zero
        Assert.assertEquals(BigDecimal.valueOf(1, t.getScale()), t.newValueUnscaled(1).toBigDecimal());
        Assert.assertEquals(BigInteger.ZERO, t.newValueUnscaled(1).toBigInteger()); // round 1^-15 to zero
        Assert.assertEquals(BigInteger.ONE, t.newValueUnscaled(1).toUnscaledBigInteger());
        Assert.assertEquals("0.000000000000001", t.newValueUnscaled(1).toString());
        Assert.assertEquals("1", t.newValueUnscaled(1).toUnscaledString());

        // -1 equals 0xFFFFFFFFFFFFFFFF or 18446744073709551615
        // scaled value equals 18446744073709551615 * 10^15 or 0x38D7EA4C67FFFFFFC72815B398000
        Assert.assertNotEquals(t.newValue(-1), t.newValueUnscaled(-1));
        Assert.assertNotEquals(t.newValue(-1), t.newValueUnsigned(-1));

        Assert.assertEquals(-1, t.newValue(-1).getHigh());
        Assert.assertEquals(-0x00038D7EA4C68000L, t.newValue(-1).getLow());
        Assert.assertEquals(-1, t.newValue(-1).toLong());
        Assert.assertEquals(BigDecimal.ONE.negate().setScale(15), t.newValue(-1).toBigDecimal());
        Assert.assertEquals(BigInteger.ONE.negate(), t.newValue(-1).toBigInteger());
        Assert.assertEquals(new BigInteger("-1000000000000000"), t.newValue(-1).toUnscaledBigInteger());
        Assert.assertEquals("-1.000000000000000", t.newValue(-1).toString());
        Assert.assertEquals("-1000000000000000", t.newValue(-1).toUnscaledString());

        Assert.assertEquals(0x00038D7EA4C67FFFL, t.newValueUnsigned(-1).getHigh());
        Assert.assertEquals(0xFFFC72815B398000L, t.newValueUnsigned(-1).getLow());
        Assert.assertEquals(new BigDecimal("18446744073709551615").setScale(15), t.newValueUnsigned(-1).toBigDecimal());
        Assert.assertEquals(new BigInteger("18446744073709551615"), t.newValueUnsigned(-1).toBigInteger());
        Assert.assertEquals(new BigInteger("18446744073709551615000000000000000"), t.newValueUnsigned(-1).toUnscaledBigInteger());
        Assert.assertEquals("18446744073709551615.000000000000000", t.newValueUnsigned(-1).toString());
        Assert.assertEquals("18446744073709551615000000000000000", t.newValueUnsigned(-1).toUnscaledString());

        Assert.assertEquals(-1, t.newValueUnscaled(-1).getHigh());
        Assert.assertEquals(-1, t.newValueUnscaled(-1).getLow());
        Assert.assertEquals(0, t.newValueUnscaled(-1).toLong()); // round -1^-15 to zero
        Assert.assertEquals(new BigDecimal("-0.000000000000001"), t.newValueUnscaled(-1).toBigDecimal());
        Assert.assertEquals(BigInteger.ZERO, t.newValueUnscaled(-1).toBigInteger()); // round -1^-15 to zero
        Assert.assertEquals(BigInteger.ONE.negate(), t.newValueUnscaled(-1).toUnscaledBigInteger());
        Assert.assertEquals("-0.000000000000001", t.newValueUnscaled(-1).toString());
        Assert.assertEquals("-1", t.newValueUnscaled(-1).toUnscaledString());

        // Long.MAX_VALUE equals 0x7fffffffffffffff or 9223372036854775807
        // scaled value equals 9223372036854775807 * 10^15 or 0x1C6BF52633FFFFFFC72815B398000
        Assert.assertNotEquals(t.newValue(Long.MAX_VALUE), t.newValueUnscaled(Long.MAX_VALUE));
        Assert.assertEquals(t.newValue(Long.MAX_VALUE), t.newValueUnsigned(Long.MAX_VALUE));

        Assert.assertEquals(0x0001C6BF52633FFFL, t.newValue(Long.MAX_VALUE).getHigh());
        Assert.assertEquals(0xFFFC72815B398000L, t.newValue(Long.MAX_VALUE).getLow());
        Assert.assertEquals(Long.MAX_VALUE, t.newValue(Long.MAX_VALUE).toLong());
        Assert.assertEquals(new BigDecimal("9223372036854775807").setScale(15), t.newValue(Long.MAX_VALUE).toBigDecimal());
        Assert.assertEquals(new BigInteger("9223372036854775807"), t.newValue(Long.MAX_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("9223372036854775807000000000000000"), t.newValue(Long.MAX_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("9223372036854775807.000000000000000", t.newValue(Long.MAX_VALUE).toString());
        Assert.assertEquals("9223372036854775807000000000000000", t.newValue(Long.MAX_VALUE).toUnscaledString());

        Assert.assertEquals(0, t.newValueUnscaled(Long.MAX_VALUE).getHigh());
        Assert.assertEquals(Long.MAX_VALUE, t.newValueUnscaled(Long.MAX_VALUE).getLow());
        Assert.assertEquals(9223, t.newValueUnscaled(Long.MAX_VALUE).toLong()); // round 9223.372036854775807
        Assert.assertEquals(BigDecimal.valueOf(Long.MAX_VALUE, t.getScale()), t.newValueUnscaled(Long.MAX_VALUE).toBigDecimal());
        Assert.assertEquals(BigInteger.valueOf(9223), t.newValueUnscaled(Long.MAX_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("9223372036854775807"), t.newValueUnscaled(Long.MAX_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("9223.372036854775807", t.newValueUnscaled(Long.MAX_VALUE).toString());
        Assert.assertEquals("9223372036854775807", t.newValueUnscaled(Long.MAX_VALUE).toUnscaledString());

        // Long.MIN_VALUE equals 0x8000000000000000
        // scaled value equals  9223372036854775808 * 10^15 = 0x0001C6BF526340000000000000000000 for unsigned
        // scaled value equals -9223372036854775808 * 10^15 = 0xFFFE3940AD9CC0000000000000000000 for signed
        Assert.assertNotEquals(t.newValue(Long.MIN_VALUE), t.newValueUnscaled(Long.MIN_VALUE));
        Assert.assertNotEquals(t.newValue(Long.MIN_VALUE), t.newValueUnsigned(Long.MIN_VALUE));

        Assert.assertEquals(0xFFFE3940AD9CC000L, t.newValue(Long.MIN_VALUE).getHigh());
        Assert.assertEquals(0, t.newValue(Long.MIN_VALUE).getLow());
        Assert.assertEquals(Long.MIN_VALUE, t.newValue(Long.MIN_VALUE).toLong());
        Assert.assertEquals(new BigDecimal("-9223372036854775808").setScale(15), t.newValue(Long.MIN_VALUE).toBigDecimal());
        Assert.assertEquals(new BigInteger("-9223372036854775808"), t.newValue(Long.MIN_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("-9223372036854775808000000000000000"), t.newValue(Long.MIN_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("-9223372036854775808.000000000000000", t.newValue(Long.MIN_VALUE).toString());
        Assert.assertEquals("-9223372036854775808000000000000000", t.newValue(Long.MIN_VALUE).toUnscaledString());

        Assert.assertEquals(0x0001C6BF52634000L, t.newValueUnsigned(Long.MIN_VALUE).getHigh());
        Assert.assertEquals(0, t.newValueUnsigned(Long.MIN_VALUE).getLow());
        Assert.assertEquals(new BigDecimal("9223372036854775808").setScale(15), t.newValueUnsigned(Long.MIN_VALUE).toBigDecimal());
        Assert.assertEquals(new BigInteger("9223372036854775808"), t.newValueUnsigned(Long.MIN_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("9223372036854775808000000000000000"), t.newValueUnsigned(Long.MIN_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("9223372036854775808.000000000000000", t.newValueUnsigned(Long.MIN_VALUE).toString());
        Assert.assertEquals("9223372036854775808000000000000000", t.newValueUnsigned(Long.MIN_VALUE).toUnscaledString());

        Assert.assertEquals(-1, t.newValueUnscaled(Long.MIN_VALUE).getHigh());
        Assert.assertEquals(Long.MIN_VALUE, t.newValueUnscaled(Long.MIN_VALUE).getLow());
        Assert.assertEquals(-9223, t.newValueUnscaled(Long.MIN_VALUE).toLong()); // round -9223.372036854775807
        Assert.assertEquals(new BigDecimal("-9223.372036854775808").setScale(15), t.newValueUnscaled(Long.MIN_VALUE).toBigDecimal());
        Assert.assertEquals(BigInteger.valueOf(-9223), t.newValueUnscaled(Long.MIN_VALUE).toBigInteger());
        Assert.assertEquals(new BigInteger("-9223372036854775808"), t.newValueUnscaled(Long.MIN_VALUE).toUnscaledBigInteger());
        Assert.assertEquals("-9223.372036854775808", t.newValueUnscaled(Long.MIN_VALUE).toString());
        Assert.assertEquals("-9223372036854775808", t.newValueUnscaled(Long.MIN_VALUE).toUnscaledString());
    }

    @Test
    public void maxScaledType() {
        // If precision == scale, values must be in (-1, 1)
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION, DecimalType.MAX_PRECISION);
        String maxValue = "0." + String.join("", Collections.nCopies(t.getScale(), "9"));
        String minValue = "-" + maxValue;

        // zero
        Assert.assertTrue(t.newValue(0).isZero());

        Assert.assertFalse(t.newValue(maxValue).isInf());
        Assert.assertEquals(t.newValue(maxValue).toString(), maxValue);

        Assert.assertFalse(t.newValue(minValue).isNegativeInf());
        Assert.assertEquals(t.newValue(minValue).toString(), minValue);

        Assert.assertEquals(0x0013426172C74D82L, t.newValue(1).getHigh());
        Assert.assertEquals(0x2B878FE800000000L, t.newValue(1).getLow());
        Assert.assertTrue(t.newValue(1).isInf());
        Assert.assertEquals("inf", t.newValue(1).toString());

        Assert.assertEquals(0x0013426172C74D82L, t.newValue(2).getHigh());
        Assert.assertEquals(0x2B878FE800000000L, t.newValue(2).getLow());
        Assert.assertTrue(t.newValue(2).isInf());
        Assert.assertEquals("inf", t.newValue(2).toString());

        Assert.assertEquals(0x0013426172C74D82L, t.newValue(Long.MAX_VALUE).getHigh());
        Assert.assertEquals(0x2B878FE800000000L, t.newValue(Long.MAX_VALUE).getLow());
        Assert.assertTrue(t.newValue(Long.MAX_VALUE).isInf());
        Assert.assertEquals("inf", t.newValue(Long.MAX_VALUE).toString());

        Assert.assertEquals(0xFFECBD9E8D38B27DL, t.newValue(-1).getHigh());
        Assert.assertEquals(0xD478701800000000L, t.newValue(-1).getLow());
        Assert.assertTrue(t.newValue(-1).isNegativeInf());
        Assert.assertEquals("-inf", t.newValue(-1).toString());

        Assert.assertEquals(0xFFECBD9E8D38B27DL, t.newValue(-2).getHigh());
        Assert.assertEquals(0xD478701800000000L, t.newValue(-2).getLow());
        Assert.assertTrue(t.newValue(-2).isNegativeInf());
        Assert.assertEquals("-inf", t.newValue(-2).toString());

        Assert.assertEquals(0xFFECBD9E8D38B27DL, t.newValue(Long.MIN_VALUE).getHigh());
        Assert.assertEquals(0xD478701800000000L, t.newValue(Long.MIN_VALUE).getLow());
        Assert.assertTrue(t.newValue(Long.MIN_VALUE).isNegativeInf());
        Assert.assertEquals("-inf", t.newValue(Long.MIN_VALUE).toString());

    }

    @Test
    public void scaledTypeRound() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION, 3);

        Assert.assertEquals(0, t.newValueUnscaled(0).toLong());
        Assert.assertEquals(BigInteger.ZERO, t.newValueUnscaled(0).toBigInteger());

        Assert.assertEquals(0, t.newValueUnscaled(499).toLong());
        Assert.assertEquals(BigInteger.ZERO, t.newValueUnscaled(499).toBigInteger());

        Assert.assertEquals(1, t.newValueUnscaled(500).toLong());
        Assert.assertEquals(BigInteger.ONE, t.newValueUnscaled(500).toBigInteger());

        Assert.assertEquals(1, t.newValueUnscaled(1000).toLong());
        Assert.assertEquals(BigInteger.ONE, t.newValueUnscaled(1000).toBigInteger());

        Assert.assertEquals(0, t.newValueUnscaled(-499).toLong());
        Assert.assertEquals(BigInteger.ZERO, t.newValueUnscaled(-499).toBigInteger());

        Assert.assertEquals(-1, t.newValueUnscaled(-500).toLong());
        Assert.assertEquals(BigInteger.ONE.negate(), t.newValueUnscaled(-500).toBigInteger());

        Assert.assertEquals(-1, t.newValueUnscaled(-1000).toLong());
        Assert.assertEquals(BigInteger.ONE.negate(), t.newValueUnscaled(-1000).toBigInteger());
    }

    @Test
    public void toUnscaledBigInteger() {
        DecimalType t = DecimalType.getDefault();

        // (1) zero
        Assert.assertEquals(t.newValue(0).toUnscaledBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValueUnsigned(0).toUnscaledBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValueUnscaled(BigInteger.ZERO).toUnscaledBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValue(BigInteger.ZERO).toUnscaledBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValue(BigDecimal.ZERO).toUnscaledBigInteger(), BigInteger.ZERO);

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            Assert.assertEquals(value, t.newValueUnscaled(value).toUnscaledBigInteger());
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            Assert.assertEquals(value, t.newValueUnscaled(value).toUnscaledBigInteger());
        }

        // (4) -inf, +inf, nan
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        Assert.assertEquals(DecimalValue.INF.toUnscaledBigInteger(), inf);
        Assert.assertEquals(DecimalValue.NEG_INF.toUnscaledBigInteger(), inf.negate());
        Assert.assertEquals(DecimalValue.NAN.toUnscaledBigInteger(), inf.add(BigInteger.ONE));
    }

    @Test
    public void toBigInteger() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION);

        // (1) zero
        Assert.assertEquals(t.newValue(0).toBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValueUnsigned(0).toBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValueUnscaled(BigInteger.ZERO).toBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValue(BigInteger.ZERO).toBigInteger(), BigInteger.ZERO);
        Assert.assertEquals(t.newValue(BigDecimal.ZERO).toBigInteger(), BigInteger.ZERO);

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            Assert.assertEquals(t.newValue(s).toBigInteger(), value);
            Assert.assertEquals(t.newValue(value).toBigInteger(), value);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            Assert.assertEquals(t.newValue(s).toBigInteger(), value);
            Assert.assertEquals(t.newValue(value).toBigInteger(), value);
        }

        // (4) -inf, +inf, nan
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        Assert.assertEquals(DecimalValue.INF.toBigInteger(), inf);
        Assert.assertEquals(DecimalValue.NEG_INF.toBigInteger(), inf.negate());
        Assert.assertEquals(DecimalValue.NAN.toBigInteger(), inf.add(BigInteger.ONE));
    }

    @Test
    public void toBigDecimal() {
        // (1) special values
        BigDecimal inf = BigDecimal.TEN.pow(DecimalType.MAX_PRECISION);
        Assert.assertEquals(DecimalValue.INF.toBigDecimal(), inf);
        Assert.assertEquals(DecimalValue.NEG_INF.toBigDecimal(), inf.negate());
        Assert.assertEquals(DecimalValue.NAN.toBigDecimal(), inf.add(BigDecimal.ONE));

        // (2) positive numbers
        Assert.assertEquals(newDecimal(1234567890L, 0).toBigDecimal(), BigDecimal.valueOf(1234567890L, 0));
        Assert.assertEquals(newDecimal(1234567890L, 1).toBigDecimal(), BigDecimal.valueOf(1234567890L, 1));
        Assert.assertEquals(newDecimal(1234567890L, 2).toBigDecimal(), BigDecimal.valueOf(1234567890L, 2));
        Assert.assertEquals(newDecimal(1234567890L, 3).toBigDecimal(), BigDecimal.valueOf(1234567890L, 3));
        Assert.assertEquals(newDecimal(1234567890L, 4).toBigDecimal(), BigDecimal.valueOf(1234567890L, 4));
        Assert.assertEquals(newDecimal(1234567890L, 5).toBigDecimal(), BigDecimal.valueOf(1234567890L, 5));
        Assert.assertEquals(newDecimal(1234567890L, 6).toBigDecimal(), BigDecimal.valueOf(1234567890L, 6));
        Assert.assertEquals(newDecimal(1234567890L, 7).toBigDecimal(), BigDecimal.valueOf(1234567890L, 7));
        Assert.assertEquals(newDecimal(1234567890L, 8).toBigDecimal(), BigDecimal.valueOf(1234567890L, 8));
        Assert.assertEquals(newDecimal(1234567890L, 9).toBigDecimal(), BigDecimal.valueOf(1234567890L, 9));
        Assert.assertEquals(newDecimal(1234567890L, 10).toBigDecimal(), BigDecimal.valueOf(1234567890L, 10));
        Assert.assertEquals(newDecimal(1234567890L, 11).toBigDecimal(), BigDecimal.valueOf(1234567890L, 11));
        Assert.assertEquals(newDecimal(1234567890L, 12).toBigDecimal(), BigDecimal.valueOf(1234567890L, 12));

        // (3) negative numbers
        Assert.assertEquals(newDecimal(-1234567890L, 0).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 0));
        Assert.assertEquals(newDecimal(-1234567890L, 1).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 1));
        Assert.assertEquals(newDecimal(-1234567890L, 2).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 2));
        Assert.assertEquals(newDecimal(-1234567890L, 3).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 3));
        Assert.assertEquals(newDecimal(-1234567890L, 4).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 4));
        Assert.assertEquals(newDecimal(-1234567890L, 5).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 5));
        Assert.assertEquals(newDecimal(-1234567890L, 6).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 6));
        Assert.assertEquals(newDecimal(-1234567890L, 7).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 7));
        Assert.assertEquals(newDecimal(-1234567890L, 8).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 8));
        Assert.assertEquals(newDecimal(-1234567890L, 9).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 9));
        Assert.assertEquals(newDecimal(-1234567890L, 10).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 10));
        Assert.assertEquals(newDecimal(-1234567890L, 11).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 11));
        Assert.assertEquals(newDecimal(-1234567890L, 12).toBigDecimal(), BigDecimal.valueOf(-1234567890L, 12));
    }

    @Test
    public void toUnscaledString() {
        DecimalType t = DecimalType.getDefault();

        // (1) zero
        Assert.assertEquals("0", t.newValue(0, 0).toUnscaledString());
        Assert.assertEquals("0", t.newValue(BigInteger.ZERO).toUnscaledString());
        Assert.assertEquals("0", t.newValue(BigDecimal.ZERO).toUnscaledString());

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            Assert.assertEquals(s, t.newValueUnscaled(new BigInteger(s)).toUnscaledString());
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            Assert.assertEquals(s, t.newValueUnscaled(new BigInteger(s)).toUnscaledString());
        }

        // (4) -inf, +inf, nan
        Assert.assertEquals("100000000000000000000000000000000000", DecimalValue.INF.toUnscaledString()); // 10^35
        Assert.assertEquals("-100000000000000000000000000000000000", DecimalValue.NEG_INF.toUnscaledString()); // -10^35
        Assert.assertEquals("100000000000000000000000000000000001", DecimalValue.NAN.toUnscaledString()); // 10^35 + 1
    }

    @Test
    public void toStringTest() {
        // (1) special values
        Assert.assertEquals("inf", DecimalValue.INF.toString());
        Assert.assertEquals("-inf", DecimalValue.NEG_INF.toString());
        Assert.assertEquals("nan", DecimalValue.NAN.toString());

        // (2) positive numbers
        Assert.assertEquals("1234567890", newDecimal(1234567890L, 0).toString());
        Assert.assertEquals("123456789.0", newDecimal(1234567890L, 1).toString());
        Assert.assertEquals("12345678.90", newDecimal(1234567890L, 2).toString());
        Assert.assertEquals("1234567.890", newDecimal(1234567890L, 3).toString());
        Assert.assertEquals("123456.7890", newDecimal(1234567890L, 4).toString());
        Assert.assertEquals("12345.67890", newDecimal(1234567890L, 5).toString());
        Assert.assertEquals("1234.567890", newDecimal(1234567890L, 6).toString());
        Assert.assertEquals("123.4567890", newDecimal(1234567890L, 7).toString());
        Assert.assertEquals("12.34567890", newDecimal(1234567890L, 8).toString());
        Assert.assertEquals("1.234567890", newDecimal(1234567890L, 9).toString());
        Assert.assertEquals("0.1234567890", newDecimal(1234567890L, 10).toString());
        Assert.assertEquals("0.01234567890", newDecimal(1234567890L, 11).toString());
        Assert.assertEquals("0.001234567890", newDecimal(1234567890L, 12).toString());

        // (3) negative numbers
        Assert.assertEquals("-1234567890", newDecimal(-1234567890L, 0).toString());
        Assert.assertEquals("-123456789.0", newDecimal(-1234567890L, 1).toString());
        Assert.assertEquals("-12345678.90", newDecimal(-1234567890L, 2).toString());
        Assert.assertEquals("-1234567.890", newDecimal(-1234567890L, 3).toString());
        Assert.assertEquals("-123456.7890", newDecimal(-1234567890L, 4).toString());
        Assert.assertEquals("-12345.67890", newDecimal(-1234567890L, 5).toString());
        Assert.assertEquals("-1234.567890", newDecimal(-1234567890L, 6).toString());
        Assert.assertEquals("-123.4567890", newDecimal(-1234567890L, 7).toString());
        Assert.assertEquals("-12.34567890", newDecimal(-1234567890L, 8).toString());
        Assert.assertEquals("-1.234567890", newDecimal(-1234567890L, 9).toString());
        Assert.assertEquals("-0.1234567890", newDecimal(-1234567890L, 10).toString());
        Assert.assertEquals("-0.01234567890", newDecimal(-1234567890L, 11).toString());
        Assert.assertEquals("-0.001234567890", newDecimal(-1234567890L, 12).toString());
    }

    private DecimalValue newDecimal(long value, int scale) {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION, scale);
        return type.newValueUnscaled(value);
    }
}
