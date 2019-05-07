package ru.yandex.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.types.DecimalType;
import ru.yandex.ydb.table.values.proto.ProtoValue;

import static ru.yandex.ydb.table.types.DecimalType.MAX_PRECISION;


/**
 * @author Sergey Polovko
 */
public class DecimalValue implements Value<DecimalType> {

    private static final long LONG_MASK = 0xFFFFFFFFL;
    private static final long LONG_SIGN_BIT = 0x8000000000000000L;
    private static final long LONG_MAX_DIGITS = 18;

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

    private DecimalValue(DecimalType type, long high, long low) {
        this.type = type;
        this.high = high;
        this.low = low;
    }

    public static DecimalValue of(DecimalType type, long high, long low) {
        if (high == 0 && low == 0) {
            return ZERO;
        }
        if (NAN.high == high && NAN.low == low) {
            return NAN;
        }

        if (high > INF.high || high == INF.high && Long.compareUnsigned(low, INF.low) >= 0) {
            return INF;
        }

        if (high < NEG_INF.high || high == NEG_INF.high && Long.compareUnsigned(low, NEG_INF.low) <= 0) {
            return NEG_INF;
        }

        return new DecimalValue(type, high, low);
    }

    public static DecimalValue of(DecimalType type, long value) {
        if (value == 0) {
            return ZERO;
        }
        long high = value > 0 ? 0 : -1;
        return new DecimalValue(type, high, value);
    }

    public static DecimalValue ofUnsigned(DecimalType type, long value) {
        if (value == 0) {
            return ZERO;
        }
        return new DecimalValue(type, 0, value);
    }

    public static DecimalValue of(DecimalType type, BigInteger value) {
        int bitLength = value.bitLength();
        if (bitLength < 64) {
            return of(type, value.longValue());
        }

        boolean negative = value.signum() < 0;
        if (bitLength > 128) {
            return negative ? NEG_INF : INF;
        }

        byte[] buf = value.abs().toByteArray();
        long high = getLongBe(buf, 0, buf.length - 8);
        long low = getLongBe(buf, buf.length - 8, buf.length);

        if (negative && (high != LONG_SIGN_BIT || low != 0)) {
            // restore negative number
            high = ~high;
            low = ~low;
            if (++low == 0) {
                high++;
            }
        }
        return of(type, high, low);
    }

    public static DecimalValue of(DecimalType type, BigDecimal value) {
        return of(type, value.unscaledValue());
    }

    public static DecimalValue of(DecimalType type, String value) {
        if (value.isEmpty()) {
            throw new NumberFormatException("cannot parse decimal from empty string");
        }

        final int end = value.length();
        int cursor = 0;

        // sign
        boolean negative = false;
        if (value.charAt(cursor) == '+') {
            cursor++;
        } else if (value.charAt(cursor) == '-') {
            cursor++;
            negative = true;
        }

        // text literals
        if (end - cursor == 3) {
            char c1 = value.charAt(cursor);
            char c2 = value.charAt(cursor + 1);
            char c3 = value.charAt(cursor + 2);

            if ((c1 == 'i' || c1 == 'I') && (c2 == 'n' || c2 == 'N') || (c3 == 'f' || c3 == 'F')) {
                return negative ? NEG_INF : INF;
            }

            if ((c1 == 'n' || c1 == 'N') && (c2 == 'a' || c2 == 'A') || (c3 == 'n' || c3 == 'N')) {
                return NAN;
            }
        }

        // skip leading zeros
        while (cursor < end && value.charAt(cursor) == '0') {
            ++cursor;
        }

        if (cursor == end) {
            return ZERO;
        }

        long accumulated = 0;
        int accumulatedCount = 0;
        boolean fractional = false; // after '.'
        int fractionalDigits = 0;
        BigInteger unscaledValue = BigInteger.ZERO;

        while (cursor < end) {
            char ch = value.charAt(cursor);
            if (ch >= '0' && ch <= '9') {
                if (accumulatedCount == LONG_MAX_DIGITS) {
                    if (unscaledValue == BigInteger.ZERO) {
                        unscaledValue = BigInteger.valueOf(accumulated);
                    } else {
                        unscaledValue = unscaledValue.multiply(BigInteger.TEN.pow(accumulatedCount));
                        unscaledValue = unscaledValue.add(BigInteger.valueOf(accumulated));
                    }
                    accumulated = 0;
                    accumulatedCount = 0;
                }
                int digit = ch - '0';
                accumulated = accumulated * 10 + digit;
                ++accumulatedCount;
                if (fractional) {
                    ++fractionalDigits;
                }
            } else if (ch == '.') {
                if (fractional) {
                    throw new NumberFormatException("invalid string: " + value);
                }
                fractional = true;
            } else {
                throw new NumberFormatException("invalid string: " + value);
            }

            ++cursor;
        }

        if (accumulatedCount > 0) {
            if (unscaledValue == BigInteger.ZERO) {
                unscaledValue = BigInteger.valueOf(accumulated);
            } else {
                unscaledValue = unscaledValue.multiply(BigInteger.TEN.pow(accumulatedCount));
                unscaledValue = unscaledValue.add(BigInteger.valueOf(accumulated));
            }
        }

        int scaleAdjust = type.getScale() - fractionalDigits;
        if (scaleAdjust > 0) {
            unscaledValue = unscaledValue.multiply(BigInteger.TEN.pow(scaleAdjust));
        } else if (scaleAdjust < 0) {
            unscaledValue = unscaledValue.divide(BigInteger.TEN.pow(-scaleAdjust));
        }

        if (negative) {
            unscaledValue = unscaledValue.negate();
        }

        return of(type, unscaledValue);
    }

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
     * Read long from a big-endian buffer.
     */
    private static long getLongBe(byte[] buf, int from, int to) {
        long r = 0;
        for (int i = from; i < to; i++) {
            r = (r << 8) | (buf[i] & 0xff);
        }
        return r;
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
