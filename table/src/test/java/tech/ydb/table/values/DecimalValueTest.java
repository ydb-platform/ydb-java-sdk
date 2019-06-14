package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import com.google.common.truth.extensions.proto.ProtoTruth;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class DecimalValueTest {

    @Test
    public void contract() {
        DecimalType type = DecimalType.of(13, 2);
        DecimalValue value = type.newValue(0x0001, 0x0002);

        assertThat(value.getType()).isEqualTo(DecimalType.of(13, 2));
        assertThat(value.getHigh()).isEqualTo(0x0001);
        assertThat(value.getLow()).isEqualTo(0x0002);

        assertThat(value).isEqualTo(type.newValue(0x0001, 0x0002));
        assertThat(value).isNotEqualTo(type.newValue(0x0001, 0x0003));
        assertThat(value).isNotEqualTo(type.newValue(0x0002, 0x0002));
        assertThat(value).isNotEqualTo(DecimalType.of(11, 2).newValue(0x0001, 0x0002));
    }

    @Test
    public void protobuf() {
        DecimalType type = DecimalType.of(13, 2);
        DecimalValue value = type.newValue(0x0001, 0x0002);

        ValueProtos.Value valuePb = value.toPb(type);
        ProtoTruth.assertThat(valuePb).isEqualTo(ProtoValue.decimal(0x0001, 0x0002));

        assertThat(value).isEqualTo(ProtoValue.fromPb(type, valuePb));
    }

    @Test
    public void inf() {
        DecimalType type = DecimalType.of();
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.newValue(inf);
            assertThat(value.isInf()).isTrue();
            assertThat(value.isNegative()).isFalse();
            assertThat(value).isSameAs(DecimalValue.INF);
            inf = inf.add(k);
        }
    }

    @Test
    public void negativeInf() {
        DecimalType type = DecimalType.of();
        BigInteger inf = BigInteger.TEN.negate().pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.newValue(inf);
            assertThat(value.isNegativeInf()).isTrue();
            assertThat(value.isNegative()).isTrue();
            assertThat(value).isSameAs(DecimalValue.NEG_INF);
            inf = inf.subtract(k);
        }
    }

    @Test
    public void ofString() {
        DecimalType t = DecimalType.of();

        assertThat(t.newValue("inf")).isSameAs(DecimalValue.INF);
        assertThat(t.newValue("Inf")).isSameAs(DecimalValue.INF);
        assertThat(t.newValue("INF")).isSameAs(DecimalValue.INF);
        assertThat(t.newValue("+inf")).isSameAs(DecimalValue.INF);
        assertThat(t.newValue("+Inf")).isSameAs(DecimalValue.INF);
        assertThat(t.newValue("+INF")).isSameAs(DecimalValue.INF);
        assertThat(t.newValue("-inf")).isSameAs(DecimalValue.NEG_INF);
        assertThat(t.newValue("-Inf")).isSameAs(DecimalValue.NEG_INF);
        assertThat(t.newValue("-INF")).isSameAs(DecimalValue.NEG_INF);
        assertThat(t.newValue("nan")).isSameAs(DecimalValue.NAN);
        assertThat(t.newValue("Nan")).isSameAs(DecimalValue.NAN);
        assertThat(t.newValue("NaN")).isSameAs(DecimalValue.NAN);
        assertThat(t.newValue("NAN")).isSameAs(DecimalValue.NAN);

        assertThat(t.newValue("0")).isSameAs(DecimalValue.ZERO);
        assertThat(t.newValue("00")).isSameAs(DecimalValue.ZERO);
        assertThat(t.newValue("0.0")).isSameAs(DecimalValue.ZERO);
        assertThat(t.newValue("00.00")).isSameAs(DecimalValue.ZERO);

        assertThat(t.newValue("1")).isEqualTo(t.newValue(1));
        assertThat(t.newValue("12")).isEqualTo(t.newValue(12));
        assertThat(t.newValue("123")).isEqualTo(t.newValue(123));
        assertThat(t.newValue("1234")).isEqualTo(t.newValue(1234));
        assertThat(t.newValue("12345")).isEqualTo(t.newValue(12345));
        assertThat(t.newValue("123456")).isEqualTo(t.newValue(123456));
        assertThat(t.newValue("1234567")).isEqualTo(t.newValue(1234567));
        assertThat(t.newValue("12345678")).isEqualTo(t.newValue(12345678));
        assertThat(t.newValue("123456789")).isEqualTo(t.newValue(123456789));
        assertThat(t.newValue("1234567890")).isEqualTo(t.newValue(1234567890));
        assertThat(t.newValue("12345678901")).isEqualTo(t.newValue(12345678901L));
        assertThat(t.newValue("1234567890123456789")).isEqualTo(t.newValue(1234567890123456789L));

        assertThat(t.newValue("-1")).isEqualTo(t.newValue(-1));
        assertThat(t.newValue("-12")).isEqualTo(t.newValue(-12));
        assertThat(t.newValue("-123")).isEqualTo(t.newValue(-123));
        assertThat(t.newValue("-1234")).isEqualTo(t.newValue(-1234));
        assertThat(t.newValue("-12345")).isEqualTo(t.newValue(-12345));
        assertThat(t.newValue("-123456")).isEqualTo(t.newValue(-123456));
        assertThat(t.newValue("-1234567")).isEqualTo(t.newValue(-1234567));
        assertThat(t.newValue("-12345678")).isEqualTo(t.newValue(-12345678));
        assertThat(t.newValue("-123456789")).isEqualTo(t.newValue(-123456789));
        assertThat(t.newValue("-1234567890")).isEqualTo(t.newValue(-1234567890));
        assertThat(t.newValue("-12345678901")).isEqualTo(t.newValue(-12345678901L));
        assertThat(t.newValue("-1234567890123456789")).isEqualTo(t.newValue(-1234567890123456789L));

        DecimalType t2 = DecimalType.of(DecimalType.MAX_PRECISION, 3);
        // fraction part is smaller than scale
        assertThat(t2.newValue("12345678.90")).isEqualTo(t2.newValue(12345678900L));
        // fraction part is bigger than scale
        assertThat(t2.newValue("123456.7890")).isEqualTo(t2.newValue(123456789L));
    }

    @Test
    public void ofUnsigned() {
        DecimalType t = DecimalType.of();

        assertThat(t.newValueUnsigned(0)).isSameAs(DecimalValue.ZERO);
        assertThat(t.newValueUnsigned(1).toString()).isEqualTo("1");
        assertThat(t.newValueUnsigned(Long.MAX_VALUE).toString()).isEqualTo(Long.toUnsignedString(Long.MAX_VALUE));
        assertThat(t.newValueUnsigned(Long.MIN_VALUE).toString()).isEqualTo(Long.toUnsignedString(Long.MIN_VALUE));
        assertThat(t.newValueUnsigned(-1).toString()).isEqualTo(Long.toUnsignedString(-1));

        BigInteger maxUint64 = BigInteger.valueOf(0xffffffffL).shiftLeft(32).or(BigInteger.valueOf(0xffffffffL));
        assertThat(t.newValueUnsigned(-1).toBigInteger()).isEqualTo(maxUint64);
    }

    @Test
    public void toBigInteger() {
        DecimalType t = DecimalType.of();

        // (1) zero
        assertThat(DecimalValue.ZERO.toBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.newValue(0, 0).toBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.newValue(BigInteger.ZERO).toBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.newValue(BigDecimal.ZERO).toBigInteger()).isEqualTo(BigInteger.ZERO);

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(t.newValue(value).toBigInteger()).isEqualTo(value);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(t.newValue(value).toBigInteger()).isEqualTo(value);
        }

        // (4) -inf, +inf, nan
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        assertThat(DecimalValue.INF.toBigInteger()).isEqualTo(inf);
        assertThat(DecimalValue.NEG_INF.toBigInteger()).isEqualTo(inf.negate());
        assertThat(DecimalValue.NAN.toBigInteger()).isEqualTo(inf.add(BigInteger.ONE));
    }

    @Test
    public void toBigDecimal() {
        // (1) special values
        MathContext mc = new MathContext(DecimalType.MAX_PRECISION, RoundingMode.HALF_EVEN);
        BigDecimal inf = BigDecimal.TEN.pow(DecimalType.MAX_PRECISION, mc);
        assertThat(DecimalValue.ZERO.toBigDecimal()).isEqualTo(BigDecimal.ZERO);
        assertThat(DecimalValue.INF.toBigDecimal()).isEqualTo(inf);
        assertThat(DecimalValue.NEG_INF.toBigDecimal()).isEqualTo(inf.negate());
        assertThat(DecimalValue.NAN.toBigDecimal()).isEqualTo(inf.add(BigDecimal.ONE, mc));

        // (2) positive numbers
        assertThat(newDecimal(1234567890L, 0).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 0));
        assertThat(newDecimal(1234567890L, 1).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 1));
        assertThat(newDecimal(1234567890L, 2).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 2));
        assertThat(newDecimal(1234567890L, 3).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 3));
        assertThat(newDecimal(1234567890L, 4).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 4));
        assertThat(newDecimal(1234567890L, 5).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 5));
        assertThat(newDecimal(1234567890L, 6).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 6));
        assertThat(newDecimal(1234567890L, 7).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 7));
        assertThat(newDecimal(1234567890L, 8).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 8));
        assertThat(newDecimal(1234567890L, 9).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 9));
        assertThat(newDecimal(1234567890L, 10).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 10));
        assertThat(newDecimal(1234567890L, 11).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 11));
        assertThat(newDecimal(1234567890L, 12).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1234567890L, 12));

        // (3) negative numbers
        assertThat(newDecimal(-1234567890L, 0).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 0));
        assertThat(newDecimal(-1234567890L, 1).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 1));
        assertThat(newDecimal(-1234567890L, 2).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 2));
        assertThat(newDecimal(-1234567890L, 3).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 3));
        assertThat(newDecimal(-1234567890L, 4).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 4));
        assertThat(newDecimal(-1234567890L, 5).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 5));
        assertThat(newDecimal(-1234567890L, 6).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 6));
        assertThat(newDecimal(-1234567890L, 7).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 7));
        assertThat(newDecimal(-1234567890L, 8).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 8));
        assertThat(newDecimal(-1234567890L, 9).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 9));
        assertThat(newDecimal(-1234567890L, 10).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 10));
        assertThat(newDecimal(-1234567890L, 11).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 11));
        assertThat(newDecimal(-1234567890L, 12).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1234567890L, 12));
    }

    @Test
    public void toUnscaledString() {
        DecimalType t = DecimalType.of();

        // (1) zero
        assertThat(DecimalValue.ZERO.toUnscaledString()).isEqualTo("0");
        assertThat(t.newValue(0, 0).toUnscaledString()).isEqualTo("0");
        assertThat(t.newValue(BigInteger.ZERO).toUnscaledString()).isEqualTo("0");
        assertThat(t.newValue(BigDecimal.ZERO).toUnscaledString()).isEqualTo("0");

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            assertThat(t.newValue(new BigInteger(s)).toUnscaledString()).isEqualTo(s);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            assertThat(t.newValue(new BigInteger(s)).toUnscaledString()).isEqualTo(s);
        }

        // (4) -inf, +inf, nan
        assertThat(DecimalValue.INF.toUnscaledString()).isEqualTo("100000000000000000000000000000000000"); // 10^35
        assertThat(DecimalValue.NEG_INF.toUnscaledString()).isEqualTo("-100000000000000000000000000000000000"); // -10^35
        assertThat(DecimalValue.NAN.toUnscaledString()).isEqualTo("100000000000000000000000000000000001"); // 10^35 + 1
    }

    @Test
    public void toStringTest() {
        // (1) special values
        assertThat(DecimalValue.ZERO.toString()).isEqualTo("0");
        assertThat(DecimalValue.INF.toString()).isEqualTo("inf");
        assertThat(DecimalValue.NEG_INF.toString()).isEqualTo("-inf");
        assertThat(DecimalValue.NAN.toString()).isEqualTo("nan");

        // (2) positive numbers
        assertThat(newDecimal(1234567890L, 0).toString()).isEqualTo("1234567890");
        assertThat(newDecimal(1234567890L, 1).toString()).isEqualTo("123456789.0");
        assertThat(newDecimal(1234567890L, 2).toString()).isEqualTo("12345678.90");
        assertThat(newDecimal(1234567890L, 3).toString()).isEqualTo("1234567.890");
        assertThat(newDecimal(1234567890L, 4).toString()).isEqualTo("123456.7890");
        assertThat(newDecimal(1234567890L, 5).toString()).isEqualTo("12345.67890");
        assertThat(newDecimal(1234567890L, 6).toString()).isEqualTo("1234.567890");
        assertThat(newDecimal(1234567890L, 7).toString()).isEqualTo("123.4567890");
        assertThat(newDecimal(1234567890L, 8).toString()).isEqualTo("12.34567890");
        assertThat(newDecimal(1234567890L, 9).toString()).isEqualTo("1.234567890");
        assertThat(newDecimal(1234567890L, 10).toString()).isEqualTo("0.1234567890");
        assertThat(newDecimal(1234567890L, 11).toString()).isEqualTo("0.01234567890");
        assertThat(newDecimal(1234567890L, 12).toString()).isEqualTo("0.001234567890");

        // (3) negative numbers
        assertThat(newDecimal(-1234567890L, 0).toString()).isEqualTo("-1234567890");
        assertThat(newDecimal(-1234567890L, 1).toString()).isEqualTo("-123456789.0");
        assertThat(newDecimal(-1234567890L, 2).toString()).isEqualTo("-12345678.90");
        assertThat(newDecimal(-1234567890L, 3).toString()).isEqualTo("-1234567.890");
        assertThat(newDecimal(-1234567890L, 4).toString()).isEqualTo("-123456.7890");
        assertThat(newDecimal(-1234567890L, 5).toString()).isEqualTo("-12345.67890");
        assertThat(newDecimal(-1234567890L, 6).toString()).isEqualTo("-1234.567890");
        assertThat(newDecimal(-1234567890L, 7).toString()).isEqualTo("-123.4567890");
        assertThat(newDecimal(-1234567890L, 8).toString()).isEqualTo("-12.34567890");
        assertThat(newDecimal(-1234567890L, 9).toString()).isEqualTo("-1.234567890");
        assertThat(newDecimal(-1234567890L, 10).toString()).isEqualTo("-0.1234567890");
        assertThat(newDecimal(-1234567890L, 11).toString()).isEqualTo("-0.01234567890");
        assertThat(newDecimal(-1234567890L, 12).toString()).isEqualTo("-0.001234567890");
    }

    private DecimalValue newDecimal(long value, int scale) {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION, scale);
        return type.newValue(value);
    }
}
