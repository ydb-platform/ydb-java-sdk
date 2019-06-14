package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;

import static tech.ydb.table.values.DecimalType.MAX_PRECISION;


/**
 * @author Sergey Polovko
 */
public class DecimalValue implements Value<DecimalType> {

    static final long LONG_MASK = 0xFFFFFFFFL;
    static final long LONG_SIGN_BIT = 0x8000000000000000L;
    static final long LONG_MAX_DIGITS = 18;

    /**
     * Positive infinity 10^{@value DecimalType#MAX_PRECISION}.
     */
    public static final DecimalValue INF = new DecimalValue(
        DecimalType.of(), 0x0013426172C74D82L, 0x2B878FE800000000L);

    /**
     * Negative infinity -10^{@value DecimalType#MAX_PRECISION}.
     */
    public static final DecimalValue NEG_INF = new DecimalValue(
        DecimalType.of(), 0xFFECBD9E8D38B27DL, 0xD478701800000000L);

    /**
     * Not a number 10^{@value DecimalType#MAX_PRECISION} + 1.
     */
    public static final DecimalValue NAN = new DecimalValue(
        DecimalType.of(), 0x0013426172C74D82L, 0x2B878FE800000001L);

    /**
     * Zero value.
     */
    public static final DecimalValue ZERO = new DecimalValue(DecimalType.of(), 0, 0);


    private final DecimalType type;
    private final long high;
    private final long low;

    DecimalValue(DecimalType type, long high, long low) {
        this.type = type;
        this.high = high;
        this.low = low;
    }

    @Override
    public DecimalType getType() {
        return type;
    }

    public long getHigh() {
        return high;
    }

    public long getLow() {
        return low;
    }

    public boolean isInf() {
        return this == INF;
    }

    public boolean isNegativeInf() {
        return this == NEG_INF;
    }

    public boolean isNan() {
        return this == NAN;
    }

    public boolean isZero() {
        return this == ZERO;
    }

    public boolean isNegative() {
        return (high & LONG_SIGN_BIT) != 0;
    }

    public BigInteger toBigInteger() {
        if (isZero()) {
            return BigInteger.ZERO;
        }

        if (high == 0 && low > 0) {
            return BigInteger.valueOf(low);
        }

        byte[] buf = new byte[16];
        putLongBe(buf, 0, high);
        putLongBe(buf, 8, low);
        return new BigInteger(buf);
    }

    public BigDecimal toBigDecimal() {
        if (isZero()) {
            return BigDecimal.ZERO;
        }

        MathContext mc = new MathContext(type.getPrecision(), RoundingMode.HALF_EVEN);
        return new BigDecimal(toBigInteger(), type.getScale(), mc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecimalValue that = (DecimalValue) o;

        if (high != that.high) return false;
        if (low != that.low) return false;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (int) (high ^ (high >>> 32));
        result = 31 * result + (int) (low ^ (low >>> 32));
        return result;
    }

    @Override
    public String toString() {
        if (isInf()) {
            return "inf";
        }
        if (isNegativeInf()) {
            return "-inf";
        }
        if (isNan()) {
            return "nan";
        }
        if (isZero()) {
            return "0";
        }

        StringBuilder sb = new StringBuilder(MAX_PRECISION + 4);
        writeAsString(sb, type.getScale());
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        if (isInf()) {
            sb.append("inf");
        }
        if (isNegativeInf()) {
            sb.append("-inf");
        }
        if (isNan()) {
            sb.append("nan");
        }
        if (isZero()) {
            sb.append("0");
        }
        writeAsString(sb, type.getScale());
    }

    public String toUnscaledString() {
        if (isZero()) {
            return "0";
        }

        StringBuilder sb = new StringBuilder(MAX_PRECISION + 2);
        writeAsString(sb, 0);
        return sb.toString();
    }

    private void writeAsString(StringBuilder sb, int scale) {
        long high = this.high;
        long low = this.low;
        boolean dot = false;

        // make positive number
        if ((high & LONG_SIGN_BIT) != 0 && (high != LONG_SIGN_BIT || low != 0)) {
            high = ~high;
            low = ~low;
            if (++low == 0) {
                ++high;
            }
        }

        long lowHi = low >>> 32;
        long lowLo = low & LONG_MASK;

        final int divisor = 10;
        do {
            // (1) high part
            long remainder = high % divisor;
            high /= divisor;

            // (2.1) high part of low
            remainder = lowHi + (remainder << 32);
            lowHi = remainder / divisor;
            remainder %= divisor;

            // (2.2) low part of low
            remainder = lowLo + (remainder << 32);
            lowLo = remainder / divisor;

            sb.append((char) ('0' + (remainder % divisor)));

            if (--scale == 0) {
                sb.append('.');
                dot = true;
            }
        } while (high != 0 || lowHi != 0 || lowLo != 0);

        if (dot && scale == 0) {
            sb.append('0');
        } else if (!dot && scale > 0) {
            while (scale-- > 0) {
                sb.append('0');
            }

            sb.append('.');
            sb.append('0');
        }

        if (isNegative()) {
            sb.append('-');
        }

        sb.reverse();
    }

    @Override
    public ValueProtos.Value toPb(DecimalType type) {
        if (!this.type.equals(type)) {
            throw new IllegalArgumentException("types mismatch, expected " + type + ", but was " + this.type);
        }
        return ProtoValue.decimal(high, low);
    }

    /**
     * Write long to a big-endian buffer.
     */
    private static void putLongBe(byte[] buf, int index, long value) {
        buf[index] = (byte) ((value >>> 56) & 0xff);
        buf[index + 1] = (byte) ((value >>> 48) & 0xff);
        buf[index + 2] = (byte) ((value >>> 40) & 0xff);
        buf[index + 3] = (byte) ((value >>> 32) & 0xff);
        buf[index + 4] = (byte) ((value >>> 24) & 0xff);
        buf[index + 5] = (byte) ((value >>> 16) & 0xff);
        buf[index + 6] = (byte) ((value >>> 8) & 0xff);
        buf[index + 7] = (byte) (value & 0xff);
    }
}
