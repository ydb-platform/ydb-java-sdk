package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import com.google.common.truth.extensions.proto.ProtoTruth;
import tech.ydb.ValueProtos;
import tech.ydb.table.types.DecimalType;
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
        DecimalValue value = DecimalValue.of(type, 0x0001, 0x0002);

        assertThat(value.getType()).isEqualTo(DecimalType.of(13, 2));
        assertThat(value.getHigh()).isEqualTo(0x0001);
        assertThat(value.getLow()).isEqualTo(0x0002);

        assertThat(value).isEqualTo(DecimalValue.of(type, 0x0001, 0x0002));
        assertThat(value).isNotEqualTo(DecimalValue.of(type, 0x0001, 0x0003));
        assertThat(value).isNotEqualTo(DecimalValue.of(type, 0x0002, 0x0002));
        assertThat(value).isNotEqualTo(DecimalValue.of(DecimalType.of(11, 2), 0x0001, 0x0002));
    }

    @Test
    public void protobuf() {
        DecimalType type = DecimalType.of(13, 2);
        DecimalValue value = DecimalValue.of(type, 0x0001, 0x0002);

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
            DecimalValue value = DecimalValue.of(type, inf);
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
            DecimalValue value = DecimalValue.of(type, inf);
            assertThat(value.isNegativeInf()).isTrue();
            assertThat(value.isNegative()).isTrue();
            assertThat(value).isSameAs(DecimalValue.NEG_INF);
            inf = inf.subtract(k);
        }
    }

    @Test
    public void ofString() {
        DecimalType t = DecimalType.of();

        assertThat(DecimalValue.of(t, "inf")).isSameAs(DecimalValue.INF);
        assertThat(DecimalValue.of(t, "Inf")).isSameAs(DecimalValue.INF);
        assertThat(DecimalValue.of(t, "INF")).isSameAs(DecimalValue.INF);
        assertThat(DecimalValue.of(t, "+inf")).isSameAs(DecimalValue.INF);
        assertThat(DecimalValue.of(t, "+Inf")).isSameAs(DecimalValue.INF);
        assertThat(DecimalValue.of(t, "+INF")).isSameAs(DecimalValue.INF);
        assertThat(DecimalValue.of(t, "-inf")).isSameAs(DecimalValue.NEG_INF);
        assertThat(DecimalValue.of(t, "-Inf")).isSameAs(DecimalValue.NEG_INF);
        assertThat(DecimalValue.of(t, "-INF")).isSameAs(DecimalValue.NEG_INF);
        assertThat(DecimalValue.of(t, "nan")).isSameAs(DecimalValue.NAN);
        assertThat(DecimalValue.of(t, "Nan")).isSameAs(DecimalValue.NAN);
        assertThat(DecimalValue.of(t, "NaN")).isSameAs(DecimalValue.NAN);
        assertThat(DecimalValue.of(t, "NAN")).isSameAs(DecimalValue.NAN);

        assertThat(DecimalValue.of(t, "0")).isSameAs(DecimalValue.ZERO);
        assertThat(DecimalValue.of(t, "00")).isSameAs(DecimalValue.ZERO);
        assertThat(DecimalValue.of(t, "0.0")).isSameAs(DecimalValue.ZERO);
        assertThat(DecimalValue.of(t, "00.00")).isSameAs(DecimalValue.ZERO);

        assertThat(DecimalValue.of(t, "1")).isEqualTo(DecimalValue.of(t, 1));
        assertThat(DecimalValue.of(t, "12")).isEqualTo(DecimalValue.of(t, 12));
        assertThat(DecimalValue.of(t, "123")).isEqualTo(DecimalValue.of(t, 123));
        assertThat(DecimalValue.of(t, "1234")).isEqualTo(DecimalValue.of(t, 1234));
        assertThat(DecimalValue.of(t, "12345")).isEqualTo(DecimalValue.of(t, 12345));
        assertThat(DecimalValue.of(t, "123456")).isEqualTo(DecimalValue.of(t, 123456));
        assertThat(DecimalValue.of(t, "1234567")).isEqualTo(DecimalValue.of(t, 1234567));
        assertThat(DecimalValue.of(t, "12345678")).isEqualTo(DecimalValue.of(t, 12345678));
        assertThat(DecimalValue.of(t, "123456789")).isEqualTo(DecimalValue.of(t, 123456789));
        assertThat(DecimalValue.of(t, "1234567890")).isEqualTo(DecimalValue.of(t, 1234567890));
        assertThat(DecimalValue.of(t, "12345678901")).isEqualTo(DecimalValue.of(t, 12345678901L));
        assertThat(DecimalValue.of(t, "1234567890123456789")).isEqualTo(DecimalValue.of(t, 1234567890123456789L));

        assertThat(DecimalValue.of(t, "-1")).isEqualTo(DecimalValue.of(t, -1));
        assertThat(DecimalValue.of(t, "-12")).isEqualTo(DecimalValue.of(t, -12));
        assertThat(DecimalValue.of(t, "-123")).isEqualTo(DecimalValue.of(t, -123));
        assertThat(DecimalValue.of(t, "-1234")).isEqualTo(DecimalValue.of(t, -1234));
        assertThat(DecimalValue.of(t, "-12345")).isEqualTo(DecimalValue.of(t, -12345));
        assertThat(DecimalValue.of(t, "-123456")).isEqualTo(DecimalValue.of(t, -123456));
        assertThat(DecimalValue.of(t, "-1234567")).isEqualTo(DecimalValue.of(t, -1234567));
        assertThat(DecimalValue.of(t, "-12345678")).isEqualTo(DecimalValue.of(t, -12345678));
        assertThat(DecimalValue.of(t, "-123456789")).isEqualTo(DecimalValue.of(t, -123456789));
        assertThat(DecimalValue.of(t, "-1234567890")).isEqualTo(DecimalValue.of(t, -1234567890));
        assertThat(DecimalValue.of(t, "-12345678901")).isEqualTo(DecimalValue.of(t, -12345678901L));
        assertThat(DecimalValue.of(t, "-1234567890123456789")).isEqualTo(DecimalValue.of(t, -1234567890123456789L));

        DecimalType t2 = DecimalType.of(DecimalType.MAX_PRECISION, 3);
        // fraction part is smaller than scale
        assertThat(DecimalValue.of(t2, "12345678.90")).isEqualTo(DecimalValue.of(t2, 12345678900L));
        // fraction part is bigger than scale
        assertThat(DecimalValue.of(t2, "123456.7890")).isEqualTo(DecimalValue.of(t2, 123456789L));
    }

    @Test
    public void ofUnsigned() {
        DecimalType t = DecimalType.of();

        assertThat(DecimalValue.ofUnsigned(t, 0)).isSameAs(DecimalValue.ZERO);
        assertThat(DecimalValue.ofUnsigned(t, 1).toString()).isEqualTo("1");
        assertThat(DecimalValue.ofUnsigned(t, Long.MAX_VALUE).toString()).isEqualTo(Long.toUnsignedString(Long.MAX_VALUE));
        assertThat(DecimalValue.ofUnsigned(t, Long.MIN_VALUE).toString()).isEqualTo(Long.toUnsignedString(Long.MIN_VALUE));
        assertThat(DecimalValue.ofUnsigned(t, -1).toString()).isEqualTo(Long.toUnsignedString(-1));

        BigInteger maxUint64 = BigInteger.valueOf(0xffffffffL).shiftLeft(32).or(BigInteger.valueOf(0xffffffffL));
        assertThat(DecimalValue.ofUnsigned(t, -1).toBigInteger()).isEqualTo(maxUint64);
    }

    @Test
    public void toBigInteger() {
        DecimalType t = DecimalType.of();

        // (1) zero
        assertThat(DecimalValue.ZERO.toBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(DecimalValue.of(t, 0, 0).toBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(DecimalValue.of(t, BigInteger.ZERO).toBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(DecimalValue.of(t, BigDecimal.ZERO).toBigInteger()).isEqualTo(BigInteger.ZERO);

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(DecimalValue.of(t, value).toBigInteger()).isEqualTo(value);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(DecimalValue.of(t, value).toBigInteger()).isEqualTo(value);
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
        assertThat(DecimalValue.of(t, 0, 0).toUnscaledString()).isEqualTo("0");
        assertThat(DecimalValue.of(t, BigInteger.ZERO).toUnscaledString()).isEqualTo("0");
        assertThat(DecimalValue.of(t, BigDecimal.ZERO).toUnscaledString()).isEqualTo("0");

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            assertThat(DecimalValue.of(t, new BigInteger(s)).toUnscaledString()).isEqualTo(s);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            assertThat(DecimalValue.of(t, new BigInteger(s)).toUnscaledString()).isEqualTo(s);
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
        return DecimalValue.of(DecimalType.of(DecimalType.MAX_PRECISION, scale), value);
    }
}
