package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

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
        DecimalValue value = DecimalValue.fromBits(type, 0x0001, 0x0002);

        assertThat(value.getType()).isEqualTo(DecimalType.of(13, 2));
        assertThat(value.getHigh()).isEqualTo(0x0001);
        assertThat(value.getLow()).isEqualTo(0x0002);

        // equals
        assertThat(value).isEqualTo(DecimalValue.fromBits(type, 0x0001, 0x0002));
        assertThat(value).isEqualTo(DecimalValue.fromBits(DecimalType.of(13, 2), 0x0001, 0x0002));

        assertThat(value).isNotEqualTo(DecimalValue.fromBits(type, 0x0001, 0x0003));
        assertThat(value).isNotEqualTo(DecimalValue.fromBits(type, 0x0002, 0x0002));
        assertThat(value).isNotEqualTo(DecimalValue.fromBits(DecimalType.of(12, 2), 0x0001, 0x0002));
        assertThat(value).isNotEqualTo(DecimalValue.fromBits(DecimalType.of(13, 1), 0x0001, 0x0002));

        // hashCode
        assertThat(value.hashCode()).isEqualTo(DecimalValue.fromBits(type, 0x0001, 0x0002).hashCode());
        assertThat(value.hashCode()).isEqualTo(DecimalValue.fromBits(DecimalType.of(13, 2), 0x0001, 0x0002).hashCode());

        assertThat(value.hashCode()).isNotEqualTo(DecimalValue.fromBits(type, 0x0001, 0x0003).hashCode());
        assertThat(value.hashCode()).isNotEqualTo(DecimalValue.fromBits(type, 0x0002, 0x0002).hashCode());
        assertThat(value.hashCode()).isNotEqualTo(DecimalValue.fromBits(DecimalType.of(12, 2), 0x0001, 0x0002).hashCode());
        assertThat(value.hashCode()).isNotEqualTo(DecimalValue.fromBits(DecimalType.of(13, 1), 0x0001, 0x0002).hashCode());
    }

    @Test
    public void protobuf() {
        DecimalType type = DecimalType.of(13, 2);
        DecimalValue value = DecimalValue.fromBits(type, 0x0001, 0x0002);

        ValueProtos.Value valuePb = value.toPb();
        ProtoTruth.assertThat(valuePb).isEqualTo(ProtoValue.fromDecimal(0x0001, 0x0002));

        assertThat(value).isEqualTo(ProtoValue.fromPb(type, valuePb));
    }

    @Test
    public void decimalScale() {
        DecimalType type = DecimalType.of(30, 2);
        DecimalValue value1 = DecimalValue.fromBits(type, 0x0000, 0x0002);

        assertThat(value1.toString()).isEqualTo("0.02");
        assertThat(value1.toUnscaledString()).isEqualTo("2");

        // 2^32 = 18446744073709551616
        DecimalValue value2 = DecimalValue.fromBits(type, 0x0001, 0x0002);
        assertThat(value2.toString()).isEqualTo("184467440737095516.18");
        assertThat(value2.toUnscaledString()).isEqualTo("18446744073709551618");
    }

    @Test
    public void inf() {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION);
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.fromBigInteger(inf);
            assertThat(value.isInf()).isTrue();
            assertThat(value.isNegative()).isFalse();
            assertThat(value).isSameAs(DecimalValue.INF);
            inf = inf.add(k);
        }
    }

    @Test
    public void negativeInf() {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION);
        BigInteger inf = BigInteger.TEN.negate().pow(DecimalType.MAX_PRECISION);
        BigInteger k = BigInteger.valueOf(0x10000000_00000000L);

        for (int i = 0; i < 100; i++) {
            DecimalValue value = type.fromBigInteger(inf);
            assertThat(value.isNegativeInf()).isTrue();
            assertThat(value.isNegative()).isTrue();
            assertThat(value).isSameAs(DecimalValue.NEG_INF);
            inf = inf.subtract(k);
        }
    }

    @Test
    public void zero() {
        DecimalType type = DecimalType.of(DecimalType.MAX_PRECISION);

        assertThat(type.fromLong(0).isZero()).isTrue();
        assertThat(type.fromLong(0).toBigDecimal()).isEqualTo(BigDecimal.ZERO);
        assertThat(type.fromLong(0).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);
        assertThat(type.fromLong(0).toString()).isEqualTo("0");

        assertThat(type.fromBits(0, 0)).isEqualTo(type.fromLong(0));
        assertThat(type.fromString("0")).isEqualTo(type.fromLong(0));
        assertThat(type.fromUnsignedLong(0)).isEqualTo(type.fromLong(0));
        assertThat(type.fromBigInteger(BigInteger.ZERO)).isEqualTo(type.fromLong(0));
        assertThat(type.fromBigDecimal(BigDecimal.ZERO)).isEqualTo(type.fromLong(0));

        DecimalType ydb = DecimalType.getDefault();
        assertThat(ydb.fromLong(0).isZero()).isTrue();
        assertThat(ydb.fromLong(0).toBigDecimal()).isNotEqualTo(BigDecimal.ZERO);
        assertThat(ydb.fromLong(0).toBigDecimal()).isEqualTo(BigDecimal.ZERO.setScale(ydb.getScale()));
        assertThat(ydb.fromLong(0).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    public void ofString() {
        DecimalType t = DecimalType.getDefault();

        assertThat(t.fromString("inf")).isSameAs(DecimalValue.INF);
        assertThat(t.fromString("Inf")).isSameAs(DecimalValue.INF);
        assertThat(t.fromString("INF")).isSameAs(DecimalValue.INF);
        assertThat(t.fromString("+inf")).isSameAs(DecimalValue.INF);
        assertThat(t.fromString("+Inf")).isSameAs(DecimalValue.INF);
        assertThat(t.fromString("+INF")).isSameAs(DecimalValue.INF);
        assertThat(t.fromString("-inf")).isSameAs(DecimalValue.NEG_INF);
        assertThat(t.fromString("-Inf")).isSameAs(DecimalValue.NEG_INF);
        assertThat(t.fromString("-INF")).isSameAs(DecimalValue.NEG_INF);
        assertThat(t.fromString("nan")).isSameAs(DecimalValue.NAN);
        assertThat(t.fromString("Nan")).isSameAs(DecimalValue.NAN);
        assertThat(t.fromString("NaN")).isSameAs(DecimalValue.NAN);
        assertThat(t.fromString("NAN")).isSameAs(DecimalValue.NAN);

        assertThat(t.fromString("0").isZero()).isTrue();
        assertThat(t.fromString("00").isZero()).isTrue();
        assertThat(t.fromString("0.0").isZero()).isTrue();
        assertThat(t.fromString("00.00").isZero()).isTrue();

        assertThat(t.fromString("1")).isEqualTo(t.fromLong(1));
        assertThat(t.fromString("12")).isEqualTo(t.fromLong(12));
        assertThat(t.fromString("123")).isEqualTo(t.fromLong(123));
        assertThat(t.fromString("1234")).isEqualTo(t.fromLong(1234));
        assertThat(t.fromString("12345")).isEqualTo(t.fromLong(12345));
        assertThat(t.fromString("123456")).isEqualTo(t.fromLong(123456));
        assertThat(t.fromString("1234567")).isEqualTo(t.fromLong(1234567));
        assertThat(t.fromString("12345678")).isEqualTo(t.fromLong(12345678));
        assertThat(t.fromString("123456789")).isEqualTo(t.fromLong(123456789));
        assertThat(t.fromString("1234567890")).isEqualTo(t.fromLong(1234567890));
        assertThat(t.fromString("12345678901")).isEqualTo(t.fromLong(12345678901L));
        assertThat(t.fromString("1234567890123456789")).isEqualTo(t.fromLong(1234567890123456789L));

        assertThat(t.fromString("-1")).isEqualTo(t.fromLong(-1));
        assertThat(t.fromString("-12")).isEqualTo(t.fromLong(-12));
        assertThat(t.fromString("-123")).isEqualTo(t.fromLong(-123));
        assertThat(t.fromString("-1234")).isEqualTo(t.fromLong(-1234));
        assertThat(t.fromString("-12345")).isEqualTo(t.fromLong(-12345));
        assertThat(t.fromString("-123456")).isEqualTo(t.fromLong(-123456));
        assertThat(t.fromString("-1234567")).isEqualTo(t.fromLong(-1234567));
        assertThat(t.fromString("-12345678")).isEqualTo(t.fromLong(-12345678));
        assertThat(t.fromString("-123456789")).isEqualTo(t.fromLong(-123456789));
        assertThat(t.fromString("-1234567890")).isEqualTo(t.fromLong(-1234567890));
        assertThat(t.fromString("-12345678901")).isEqualTo(t.fromLong(-12345678901L));
        assertThat(t.fromString("-1234567890123456789")).isEqualTo(t.fromLong(-1234567890123456789L));

        DecimalType t2 = DecimalType.of(DecimalType.MAX_PRECISION, 3);
        // fraction part is smaller than scale
        assertThat(t2.fromString("12345678.9")).isEqualTo(DecimalValue.fromUnscaledLong(t2, 12345678900L));
        // fraction part is bigger than scale
        assertThat(t2.fromString("123456.789")).isEqualTo(DecimalValue.fromUnscaledLong(t2, 123456789L));
    }

    @Test
    public void ofUnsigned() {
        DecimalType t = DecimalType.getDefault();
        String zeros = "." + String.join("", Collections.nCopies(t.getScale(), "0"));

        assertThat(t.fromUnsignedLong(0).isZero()).isTrue();
        assertThat(t.fromUnsignedLong(1).toString()).isEqualTo("1" + zeros);
        assertThat(t.fromUnsignedLong(Long.MAX_VALUE).toString()).isEqualTo(Long.toUnsignedString(Long.MAX_VALUE) + zeros);
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toString()).isEqualTo(Long.toUnsignedString(Long.MIN_VALUE) + zeros);
        assertThat(t.fromUnsignedLong(-1).toString()).isEqualTo(Long.toUnsignedString(-1) + zeros);

        BigInteger maxUint64 = BigInteger.valueOf(0xffffffffL).shiftLeft(32).or(BigInteger.valueOf(0xffffffffL));

        BigInteger unscaledMaxUint64 = maxUint64.multiply(BigInteger.TEN.pow(t.getScale()));

        assertThat(t.fromUnsignedLong(-1).toBigIntegerValue()).isEqualTo(maxUint64);
        assertThat(t.fromUnsignedLong(-1).toUnscaledBigInteger()).isEqualTo(unscaledMaxUint64);
        assertThat(t.fromUnsignedLong(-1).toBigDecimal()).isEqualTo(new BigDecimal(unscaledMaxUint64, t.getScale()));
    }

    @Test
    public void ofLong() {
        DecimalType t = DecimalType.getDefault();
        String zeros = "." + String.join("", Collections.nCopies(t.getScale(), "0"));

        assertThat(t.fromLong(0).isZero()).isTrue();
        assertThat(t.fromLong(1).toString()).isEqualTo("1" + zeros);
        assertThat(t.fromLong(Long.MAX_VALUE).toString()).isEqualTo(Long.toString(Long.MAX_VALUE) + zeros);
        assertThat(t.fromLong(Long.MIN_VALUE).toString()).isEqualTo(Long.toString(Long.MIN_VALUE) + zeros);
        assertThat(t.fromLong(-1).toString()).isEqualTo(Long.toString(-1) + zeros);

        BigInteger rawMinusOne = BigInteger.valueOf(-1)
                .multiply(BigInteger.TEN.pow(t.getScale()));

        assertThat(t.fromLong(-1).toBigIntegerValue()).isEqualTo(BigInteger.valueOf(-1));
        assertThat(t.fromLong(-1).toUnscaledBigInteger()).isEqualTo(rawMinusOne);
        assertThat(t.fromLong(-1).toBigDecimal()).isEqualTo(BigDecimal.valueOf(-1).setScale(t.getScale()));
        assertThat(t.fromLong(-1).toBigDecimal()).isEqualTo(new BigDecimal(rawMinusOne, t.getScale()));
    }

    @Test
    public void unscaledType() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION);

        // zero
        assertThat(t.fromLong(0).isZero()).isTrue();

        // one
        assertThat(t.fromLong(1)).isEqualTo(t.fromUnscaledLong(1));
        assertThat(t.fromLong(1)).isEqualTo(t.fromUnsignedLong(1));

        assertThat(t.fromLong(1).getHigh()).isEqualTo(0);
        assertThat(t.fromLong(1).getLow()).isEqualTo(1);
        assertThat(t.fromLong(1).toLong()).isEqualTo(1);
        assertThat(t.fromLong(1).toBigDecimal()).isEqualTo(BigDecimal.ONE);
        assertThat(t.fromLong(1).toBigIntegerValue()).isEqualTo(BigInteger.ONE);
        assertThat(t.fromLong(1).toUnscaledBigInteger()).isEqualTo(BigInteger.ONE);
        assertThat(t.fromLong(1).toString()).isEqualTo("1");
        assertThat(t.fromLong(1).toUnscaledString()).isEqualTo("1");

        // -1 equals 0xFFFFFFFFFFFFFFFF or 18446744073709551615
        assertThat(t.fromLong(-1)).isEqualTo(t.fromUnscaledLong(-1));
        assertThat(t.fromLong(-1)).isNotEqualTo(t.fromUnsignedLong(-1));

        assertThat(t.fromLong(-1).getHigh()).isEqualTo(-1);
        assertThat(t.fromLong(-1).getLow()).isEqualTo(-1);
        assertThat(t.fromLong(-1).toLong()).isEqualTo(-1);
        assertThat(t.fromLong(-1).toBigDecimal()).isEqualTo(BigDecimal.ONE.negate());
        assertThat(t.fromLong(-1).toBigIntegerValue()).isEqualTo(BigInteger.ONE.negate());
        assertThat(t.fromLong(-1).toUnscaledBigInteger()).isEqualTo(BigInteger.ONE.negate());
        assertThat(t.fromLong(-1).toString()).isEqualTo("-1");
        assertThat(t.fromLong(-1).toUnscaledString()).isEqualTo("-1");

        assertThat(t.fromUnsignedLong(-1).getHigh()).isEqualTo(0);
        assertThat(t.fromUnsignedLong(-1).getLow()).isEqualTo(-1);
        assertThat(t.fromUnsignedLong(-1).toBigDecimal()).isEqualTo(new BigDecimal("18446744073709551615"));
        assertThat(t.fromUnsignedLong(-1).toBigIntegerValue()).isEqualTo(new BigInteger("18446744073709551615"));
        assertThat(t.fromUnsignedLong(-1).toUnscaledBigInteger()).isEqualTo(new BigInteger("18446744073709551615"));
        assertThat(t.fromUnsignedLong(-1).toString()).isEqualTo("18446744073709551615");
        assertThat(t.fromUnsignedLong(-1).toUnscaledString()).isEqualTo("18446744073709551615");

        // Long.MAX_VALUE equals 0x7fffffffffffffff or 9223372036854775807
        assertThat(t.fromLong(Long.MAX_VALUE)).isEqualTo(t.fromUnscaledLong(Long.MAX_VALUE));
        assertThat(t.fromLong(Long.MAX_VALUE)).isEqualTo(t.fromUnsignedLong(Long.MAX_VALUE));

        assertThat(t.fromLong(Long.MAX_VALUE).getHigh()).isEqualTo(0);
        assertThat(t.fromLong(Long.MAX_VALUE).getLow()).isEqualTo(Long.MAX_VALUE);
        assertThat(t.fromLong(Long.MAX_VALUE).toLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(t.fromLong(Long.MAX_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("9223372036854775807"));
        assertThat(t.fromLong(Long.MAX_VALUE).toBigIntegerValue()).isEqualTo(new BigInteger("9223372036854775807"));
        assertThat(t.fromLong(Long.MAX_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("9223372036854775807"));
        assertThat(t.fromLong(Long.MAX_VALUE).toString()).isEqualTo("9223372036854775807");
        assertThat(t.fromLong(Long.MAX_VALUE).toUnscaledString()).isEqualTo("9223372036854775807");

        // Long.MIN_VALUE equals 0x8000000000000000 or -9223372036854775808 for signed or 9223372036854775808 for unsigned
        assertThat(t.fromLong(Long.MIN_VALUE)).isEqualTo(t.fromUnscaledLong(Long.MIN_VALUE));
        assertThat(t.fromLong(Long.MIN_VALUE)).isNotEqualTo(t.fromUnsignedLong(Long.MIN_VALUE));

        assertThat(t.fromLong(Long.MIN_VALUE).getHigh()).isEqualTo(-1);
        assertThat(t.fromLong(Long.MIN_VALUE).getLow()).isEqualTo(Long.MIN_VALUE);
        assertThat(t.fromLong(Long.MIN_VALUE).toLong()).isEqualTo(Long.MIN_VALUE);
        assertThat(t.fromLong(Long.MIN_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("-9223372036854775808"));
        assertThat(t.fromLong(Long.MIN_VALUE).toBigIntegerValue()).isEqualTo(new BigInteger("-9223372036854775808"));
        assertThat(t.fromLong(Long.MIN_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("-9223372036854775808"));
        assertThat(t.fromLong(Long.MIN_VALUE).toString()).isEqualTo("-9223372036854775808");
        assertThat(t.fromLong(Long.MIN_VALUE).toUnscaledString()).isEqualTo("-9223372036854775808");

        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).getHigh()).isEqualTo(0);
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).getLow()).isEqualTo(Long.MIN_VALUE);
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("9223372036854775808"));
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toBigIntegerValue()).isEqualTo(new BigInteger("9223372036854775808"));
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("9223372036854775808"));
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toString()).isEqualTo("9223372036854775808");
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toUnscaledString()).isEqualTo("9223372036854775808");
    }

    @Test
    public void scaledType() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION, 15);

        // zero
        assertThat(t.fromLong(0).isZero()).isTrue();

        // one equals unscaled 10^15 = 0x00038D7EA4C68000
        assertThat(t.fromLong(1)).isNotEqualTo(t.fromUnscaledLong(1));
        assertThat(t.fromLong(1)).isEqualTo(t.fromUnsignedLong(1));

        assertThat(t.fromLong(1).getHigh()).isEqualTo(0);
        assertThat(t.fromLong(1).getLow()).isEqualTo(0x00038D7EA4C68000L);
        assertThat(t.fromLong(1).toLong()).isEqualTo(1);
        assertThat(t.fromLong(1).toBigDecimal()).isEqualTo(BigDecimal.ONE.setScale(15));
        assertThat(t.fromLong(1).toBigIntegerValue()).isEqualTo(BigInteger.ONE);
        assertThat(t.fromLong(1).toUnscaledBigInteger()).isEqualTo(new BigInteger("1000000000000000"));
        assertThat(t.fromLong(1).toString()).isEqualTo("1.000000000000000");
        assertThat(t.fromLong(1).toUnscaledString()).isEqualTo("1000000000000000");

        assertThat(t.fromUnscaledLong(1).getHigh()).isEqualTo(0);
        assertThat(t.fromUnscaledLong(1).getLow()).isEqualTo(1);
        assertThat(t.fromUnscaledLong(1).toLong()).isEqualTo(0); // round 1^-15 to zero
        assertThat(t.fromUnscaledLong(1).toBigDecimal()).isEqualTo(BigDecimal.valueOf(1, t.getScale()));
        assertThat(t.fromUnscaledLong(1).toBigIntegerValue()).isEqualTo(BigInteger.ZERO); // round 1^-15 to zero
        assertThat(t.fromUnscaledLong(1).toUnscaledBigInteger()).isEqualTo(BigInteger.ONE);
        assertThat(t.fromUnscaledLong(1).toString()).isEqualTo("0.000000000000001");
        assertThat(t.fromUnscaledLong(1).toUnscaledString()).isEqualTo("1");

        // -1 equals 0xFFFFFFFFFFFFFFFF or 18446744073709551615
        // scaled value equals 18446744073709551615 * 10^15 or 0x38D7EA4C67FFFFFFC72815B398000
        assertThat(t.fromLong(-1)).isNotEqualTo(t.fromUnscaledLong(-1));
        assertThat(t.fromLong(-1)).isNotEqualTo(t.fromUnsignedLong(-1));

        assertThat(t.fromLong(-1).getHigh()).isEqualTo(-1);
        assertThat(t.fromLong(-1).getLow()).isEqualTo(-0x00038D7EA4C68000L);
        assertThat(t.fromLong(-1).toLong()).isEqualTo(-1);
        assertThat(t.fromLong(-1).toBigDecimal()).isEqualTo(BigDecimal.ONE.negate().setScale(15));
        assertThat(t.fromLong(-1).toBigIntegerValue()).isEqualTo(BigInteger.ONE.negate());
        assertThat(t.fromLong(-1).toUnscaledBigInteger()).isEqualTo(new BigInteger("-1000000000000000"));
        assertThat(t.fromLong(-1).toString()).isEqualTo("-1.000000000000000");
        assertThat(t.fromLong(-1).toUnscaledString()).isEqualTo("-1000000000000000");

        assertThat(t.fromUnsignedLong(-1).getHigh()).isEqualTo(0x00038D7EA4C67FFFL);
        assertThat(t.fromUnsignedLong(-1).getLow()).isEqualTo(0xFFFC72815B398000L);
        assertThat(t.fromUnsignedLong(-1).toBigDecimal()).isEqualTo(new BigDecimal("18446744073709551615").setScale(15));
        assertThat(t.fromUnsignedLong(-1).toBigIntegerValue()).isEqualTo(new BigInteger("18446744073709551615"));
        assertThat(t.fromUnsignedLong(-1).toUnscaledBigInteger()).isEqualTo(new BigInteger("18446744073709551615000000000000000"));
        assertThat(t.fromUnsignedLong(-1).toString()).isEqualTo("18446744073709551615.000000000000000");
        assertThat(t.fromUnsignedLong(-1).toUnscaledString()).isEqualTo("18446744073709551615000000000000000");

        assertThat(t.fromUnscaledLong(-1).getHigh()).isEqualTo(-1);
        assertThat(t.fromUnscaledLong(-1).getLow()).isEqualTo(-1);
        assertThat(t.fromUnscaledLong(-1).toLong()).isEqualTo(0); // round -1^-15 to zero
        assertThat(t.fromUnscaledLong(-1).toBigDecimal()).isEqualTo(new BigDecimal("-0.000000000000001"));
        assertThat(t.fromUnscaledLong(-1).toBigIntegerValue()).isEqualTo(BigInteger.ZERO); // round -1^-15 to zero
        assertThat(t.fromUnscaledLong(-1).toUnscaledBigInteger()).isEqualTo(BigInteger.ONE.negate());
        assertThat(t.fromUnscaledLong(-1).toString()).isEqualTo("-0.000000000000001");
        assertThat(t.fromUnscaledLong(-1).toUnscaledString()).isEqualTo("-1");

        // Long.MAX_VALUE equals 0x7fffffffffffffff or 9223372036854775807
        // scaled value equals 9223372036854775807 * 10^15 or 0x1C6BF52633FFFFFFC72815B398000
        assertThat(t.fromLong(Long.MAX_VALUE)).isNotEqualTo(t.fromUnscaledLong(Long.MAX_VALUE));
        assertThat(t.fromLong(Long.MAX_VALUE)).isEqualTo(t.fromUnsignedLong(Long.MAX_VALUE));

        assertThat(t.fromLong(Long.MAX_VALUE).getHigh()).isEqualTo(0x0001C6BF52633FFFL);
        assertThat(t.fromLong(Long.MAX_VALUE).getLow()).isEqualTo(0xFFFC72815B398000L);
        assertThat(t.fromLong(Long.MAX_VALUE).toLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(t.fromLong(Long.MAX_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("9223372036854775807").setScale(15));
        assertThat(t.fromLong(Long.MAX_VALUE).toBigIntegerValue()).isEqualTo(new BigInteger("9223372036854775807"));
        assertThat(t.fromLong(Long.MAX_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("9223372036854775807000000000000000"));
        assertThat(t.fromLong(Long.MAX_VALUE).toString()).isEqualTo("9223372036854775807.000000000000000");
        assertThat(t.fromLong(Long.MAX_VALUE).toUnscaledString()).isEqualTo("9223372036854775807000000000000000");

        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).getHigh()).isEqualTo(0);
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).getLow()).isEqualTo(Long.MAX_VALUE);
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).toLong()).isEqualTo(9223); // round 9223.372036854775807
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).toBigDecimal()).isEqualTo(BigDecimal.valueOf(Long.MAX_VALUE, t.getScale()));
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).toBigIntegerValue()).isEqualTo(BigInteger.valueOf(9223));
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("9223372036854775807"));
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).toString()).isEqualTo("9223.372036854775807");
        assertThat(t.fromUnscaledLong(Long.MAX_VALUE).toUnscaledString()).isEqualTo("9223372036854775807");

        // Long.MIN_VALUE equals 0x8000000000000000
        // scaled value equals  9223372036854775808 * 10^15 = 0x0001C6BF526340000000000000000000 for unsigned
        // scaled value equals -9223372036854775808 * 10^15 = 0xFFFE3940AD9CC0000000000000000000 for signed
        assertThat(t.fromLong(Long.MIN_VALUE)).isNotEqualTo(t.fromUnscaledLong(Long.MIN_VALUE));
        assertThat(t.fromLong(Long.MIN_VALUE)).isNotEqualTo(t.fromUnsignedLong(Long.MIN_VALUE));

        assertThat(t.fromLong(Long.MIN_VALUE).getHigh()).isEqualTo(0xFFFE3940AD9CC000L);
        assertThat(t.fromLong(Long.MIN_VALUE).getLow()).isEqualTo(0);
        assertThat(t.fromLong(Long.MIN_VALUE).toLong()).isEqualTo(Long.MIN_VALUE);
        assertThat(t.fromLong(Long.MIN_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("-9223372036854775808").setScale(15));
        assertThat(t.fromLong(Long.MIN_VALUE).toBigIntegerValue()).isEqualTo(new BigInteger("-9223372036854775808"));
        assertThat(t.fromLong(Long.MIN_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("-9223372036854775808000000000000000"));
        assertThat(t.fromLong(Long.MIN_VALUE).toString()).isEqualTo("-9223372036854775808.000000000000000");
        assertThat(t.fromLong(Long.MIN_VALUE).toUnscaledString()).isEqualTo("-9223372036854775808000000000000000");

        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).getHigh()).isEqualTo(0x0001C6BF52634000L);
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).getLow()).isEqualTo(0);
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("9223372036854775808").setScale(15));
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toBigIntegerValue()).isEqualTo(new BigInteger("9223372036854775808"));
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("9223372036854775808000000000000000"));
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toString()).isEqualTo("9223372036854775808.000000000000000");
        assertThat(t.fromUnsignedLong(Long.MIN_VALUE).toUnscaledString()).isEqualTo("9223372036854775808000000000000000");

        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).getHigh()).isEqualTo(-1);
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).getLow()).isEqualTo(Long.MIN_VALUE);
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).toLong()).isEqualTo(-9223); // round -9223.372036854775807
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).toBigDecimal()).isEqualTo(new BigDecimal("-9223.372036854775808").setScale(15));
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).toBigIntegerValue()).isEqualTo(BigInteger.valueOf(-9223));
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).toUnscaledBigInteger()).isEqualTo(new BigInteger("-9223372036854775808"));
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).toString()).isEqualTo("-9223.372036854775808");
        assertThat(t.fromUnscaledLong(Long.MIN_VALUE).toUnscaledString()).isEqualTo("-9223372036854775808");
    }

    @Test
    public void maxScaledType() {
        // If precision == scale, values must be in (-1, 1)
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION, DecimalType.MAX_PRECISION);
        String maxValue = "0." + String.join("", Collections.nCopies(t.getScale(), "9"));
        String minValue = "-" + maxValue;

        // zero
        assertThat(t.fromLong(0).isZero()).isTrue();

        assertThat(t.fromString(maxValue).isInf()).isFalse();
        assertThat(t.fromString(maxValue).toString()).isEqualTo(maxValue);

        assertThat(t.fromString(minValue).isNegativeInf()).isFalse();
        assertThat(t.fromString(minValue).toString()).isEqualTo(minValue);

        assertThat(t.fromLong(1).getHigh()).isEqualTo(0x0013426172C74D82L);
        assertThat(t.fromLong(1).getLow()).isEqualTo(0x2B878FE800000000L);
        assertThat(t.fromLong(1).isInf()).isTrue();
        assertThat(t.fromLong(1).toString()).isEqualTo("inf");

        assertThat(t.fromLong(2).getHigh()).isEqualTo(0x0013426172C74D82L);
        assertThat(t.fromLong(2).getLow()).isEqualTo(0x2B878FE800000000L);
        assertThat(t.fromLong(2).isInf()).isTrue();
        assertThat(t.fromLong(2).toString()).isEqualTo("inf");

        assertThat(t.fromLong(Long.MAX_VALUE).getHigh()).isEqualTo(0x0013426172C74D82L);
        assertThat(t.fromLong(Long.MAX_VALUE).getLow()).isEqualTo(0x2B878FE800000000L);
        assertThat(t.fromLong(Long.MAX_VALUE).isInf()).isTrue();
        assertThat(t.fromLong(Long.MAX_VALUE).toString()).isEqualTo("inf");

        assertThat(t.fromLong(-1).getHigh()).isEqualTo(0xFFECBD9E8D38B27DL);
        assertThat(t.fromLong(-1).getLow()).isEqualTo(0xD478701800000000L);
        assertThat(t.fromLong(-1).isNegativeInf()).isTrue();
        assertThat(t.fromLong(-1).toString()).isEqualTo("-inf");

        assertThat(t.fromLong(-2).getHigh()).isEqualTo(0xFFECBD9E8D38B27DL);
        assertThat(t.fromLong(-2).getLow()).isEqualTo(0xD478701800000000L);
        assertThat(t.fromLong(-2).isNegativeInf()).isTrue();
        assertThat(t.fromLong(-2).toString()).isEqualTo("-inf");

        assertThat(t.fromLong(Long.MIN_VALUE).getHigh()).isEqualTo(0xFFECBD9E8D38B27DL);
        assertThat(t.fromLong(Long.MIN_VALUE).getLow()).isEqualTo(0xD478701800000000L);
        assertThat(t.fromLong(Long.MIN_VALUE).isNegativeInf()).isTrue();
        assertThat(t.fromLong(Long.MIN_VALUE).toString()).isEqualTo("-inf");

    }

    @Test
    public void scaledTypeRound() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION, 3);

        assertThat(t.fromUnscaledLong(0).toLong()).isEqualTo(0);
        assertThat(t.fromUnscaledLong(0).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);

        assertThat(t.fromUnscaledLong(499).toLong()).isEqualTo(0);
        assertThat(t.fromUnscaledLong(499).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);

        assertThat(t.fromUnscaledLong(500).toLong()).isEqualTo(1);
        assertThat(t.fromUnscaledLong(500).toBigIntegerValue()).isEqualTo(BigInteger.ONE);

        assertThat(t.fromUnscaledLong(1000).toLong()).isEqualTo(1);
        assertThat(t.fromUnscaledLong(1000).toBigIntegerValue()).isEqualTo(BigInteger.ONE);

        assertThat(t.fromUnscaledLong(-499).toLong()).isEqualTo(0);
        assertThat(t.fromUnscaledLong(-499).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);

        assertThat(t.fromUnscaledLong(-500).toLong()).isEqualTo(-1);
        assertThat(t.fromUnscaledLong(-500).toBigIntegerValue()).isEqualTo(BigInteger.ONE.negate());

        assertThat(t.fromUnscaledLong(-1000).toLong()).isEqualTo(-1);
        assertThat(t.fromUnscaledLong(-1000).toBigIntegerValue()).isEqualTo(BigInteger.ONE.negate());
    }

    @Test
    public void toUnscaledBigInteger() {
        DecimalType t = DecimalType.getDefault();

        // (1) zero
        assertThat(t.fromLong(0).toUnscaledBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromUnsignedLong(0).toUnscaledBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromUnscaledBigInteger(BigInteger.ZERO).toUnscaledBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromBigInteger(BigInteger.ZERO).toUnscaledBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromBigDecimal(BigDecimal.ZERO).toUnscaledBigInteger()).isEqualTo(BigInteger.ZERO);

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(t.fromUnscaledBigInteger(value).toUnscaledBigInteger()).isEqualTo(value);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(t.fromUnscaledBigInteger(value).toUnscaledBigInteger()).isEqualTo(value);
        }

        // (4) -inf, +inf, nan
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        assertThat(DecimalValue.INF.toUnscaledBigInteger()).isEqualTo(inf);
        assertThat(DecimalValue.NEG_INF.toUnscaledBigInteger()).isEqualTo(inf.negate());
        assertThat(DecimalValue.NAN.toUnscaledBigInteger()).isEqualTo(inf.add(BigInteger.ONE));
    }

    @Test
    public void toBigIntegerValue() {
        DecimalType t = DecimalType.of(DecimalType.MAX_PRECISION);

        // (1) zero
        assertThat(t.fromLong(0).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromUnsignedLong(0).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromUnscaledBigInteger(BigInteger.ZERO).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromBigInteger(BigInteger.ZERO).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);
        assertThat(t.fromBigDecimal(BigDecimal.ZERO).toBigIntegerValue()).isEqualTo(BigInteger.ZERO);

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(t.fromString(s).toBigIntegerValue()).isEqualTo(value);
            assertThat(t.fromBigInteger(value).toBigIntegerValue()).isEqualTo(value);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            BigInteger value = new BigInteger(s);
            assertThat(t.fromString(s).toBigIntegerValue()).isEqualTo(value);
            assertThat(t.fromBigInteger(value).toBigIntegerValue()).isEqualTo(value);
        }

        // (4) -inf, +inf, nan
        BigInteger inf = BigInteger.TEN.pow(DecimalType.MAX_PRECISION);
        assertThat(DecimalValue.INF.toBigIntegerValue()).isEqualTo(inf);
        assertThat(DecimalValue.NEG_INF.toBigIntegerValue()).isEqualTo(inf.negate());
        assertThat(DecimalValue.NAN.toBigIntegerValue()).isEqualTo(inf.add(BigInteger.ONE));
    }

    @Test
    public void toBigDecimal() {
        // (1) special values
        BigDecimal inf = BigDecimal.TEN.pow(DecimalType.MAX_PRECISION);
        assertThat(DecimalValue.INF.toBigDecimal()).isEqualTo(inf);
        assertThat(DecimalValue.NEG_INF.toBigDecimal()).isEqualTo(inf.negate());
        assertThat(DecimalValue.NAN.toBigDecimal()).isEqualTo(inf.add(BigDecimal.ONE));

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
        DecimalType t = DecimalType.getDefault();

        // (1) zero
        assertThat(t.fromBits(0, 0).toUnscaledString()).isEqualTo("0");
        assertThat(t.fromBigInteger(BigInteger.ZERO).toUnscaledString()).isEqualTo("0");
        assertThat(t.fromBigDecimal(BigDecimal.ZERO).toUnscaledString()).isEqualTo("0");

        // (2) positive numbers: 1, 12, 123, ...
        String s = "";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            assertThat(t.fromUnscaledBigInteger(new BigInteger(s)).toUnscaledString()).isEqualTo(s);
        }

        // (3) negative numbers: -1, -12, -123, ...
        s = "-";
        for (int i = 1; i < DecimalType.MAX_PRECISION; i++) {
            s += Integer.toString(i % 10);
            assertThat(t.fromUnscaledBigInteger(new BigInteger(s)).toUnscaledString()).isEqualTo(s);
        }

        // (4) -inf, +inf, nan
        assertThat(DecimalValue.INF.toUnscaledString()).isEqualTo("100000000000000000000000000000000000"); // 10^35
        assertThat(DecimalValue.NEG_INF.toUnscaledString()).isEqualTo("-100000000000000000000000000000000000"); // -10^35
        assertThat(DecimalValue.NAN.toUnscaledString()).isEqualTo("100000000000000000000000000000000001"); // 10^35 + 1
    }

    @Test
    public void toStringTest() {
        // (1) special values
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
        return type.fromUnscaledLong(value);
    }
}
