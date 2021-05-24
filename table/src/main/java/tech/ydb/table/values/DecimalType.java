package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.google.common.base.Preconditions;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public class DecimalType implements Type {

    public static final int MAX_PRECISION = 35;

    private static final DecimalType DEFAULT_TYPE = DecimalType.of(MAX_PRECISION);

    private final byte precision;
    private final byte scale;

    private DecimalType(byte precision, byte scale) {
        this.precision = precision;
        this.scale = scale;
    }

    public static DecimalType of() {
        return DEFAULT_TYPE;
    }

    public static DecimalType of(int precision) {
        return of(precision, 0);
    }

    public static DecimalType of(int precision, int scale) {
        Preconditions.checkArgument(precision >= 1 && precision <= MAX_PRECISION,
            "precision (%s) is out of range [1, %s]", precision, MAX_PRECISION);
        Preconditions.checkArgument(scale >= 0 && scale <= precision,
            "scale (%s) is out of range [0, %s]", scale, precision);
        return new DecimalType((byte) precision, (byte) scale);
    }

    @Override
    public Kind getKind() {
        return Kind.DECIMAL;
    }

    public int getPrecision() {
        return precision;
    }

    public int getScale() {
        return scale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecimalType that = (DecimalType) o;
        if (precision != that.precision) return false;
        return scale == that.scale;
    }

    @Override
    public int hashCode() {
        return 31 * precision + scale;
    }

    @Override
    public String toString() {
        return "Decimal(" + precision + ", " + scale + ')';
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.decimal((int) precision, (int) scale);
    }

    private DecimalValue toZero() {
        return this.precision == DecimalType.MAX_PRECISION && this.scale == 0 ?
                DecimalValue.ZERO :
                new DecimalValue(this, 0, 0);
    }

    public DecimalValue newValue(long high, long low) {
        if (high == 0 && low == 0) {
            return toZero();
        }

        DecimalValue nan = DecimalValue.NAN;
        if (nan.getHigh() == high && nan.getLow() == low) {
            return nan;
        }

        DecimalValue inf = DecimalValue.INF;
        if (high > inf.getHigh() || high == inf.getHigh() && Long.compareUnsigned(low, inf.getLow()) >= 0) {
            return inf;
        }

        DecimalValue negInf = DecimalValue.NEG_INF;
        if (high < negInf.getHigh() || high == negInf.getHigh() && Long.compareUnsigned(low, negInf
            .getLow()) <= 0) {
            return negInf;
        }

        return new DecimalValue(this, high, low);
    }

    public DecimalValue newValue(long value) {
        if (value == 0) {
            return toZero();
        }
        long high = value > 0 ? 0 : -1;
        return new DecimalValue(this, high, value);
    }

    public DecimalValue newValueUnsigned(long value) {
        if (value == 0) {
            return toZero();
        }
        return new DecimalValue(this, 0, value);
    }

    public DecimalValue newValue(BigInteger value) {
        int bitLength = value.bitLength();
        if (bitLength < 64) {
            return newValue(value.longValue());
        }

        boolean negative = value.signum() < 0;
        if (bitLength > 128) {
            return negative ? DecimalValue.NEG_INF : DecimalValue.INF;
        }

        byte[] buf = value.abs().toByteArray();
        long high = getLongBe(buf, 0, buf.length - 8);
        long low = getLongBe(buf, buf.length - 8, buf.length);

        if (negative && (high != DecimalValue.LONG_SIGN_BIT || low != 0)) {
            // restore negative number
            high = ~high;
            low = ~low;
            if (++low == 0) {
                high++;
            }
        }
        return newValue(high, low);
    }

    public DecimalValue newValue(BigDecimal value) {
        return newValue(value.unscaledValue());
    }

    public DecimalValue newValue(String value) {
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
                return negative ? DecimalValue.NEG_INF : DecimalValue.INF;
            }

            if ((c1 == 'n' || c1 == 'N') && (c2 == 'a' || c2 == 'A') || (c3 == 'n' || c3 == 'N')) {
                return DecimalValue.NAN;
            }
        }

        // skip leading zeros
        while (cursor < end && value.charAt(cursor) == '0') {
            ++cursor;
        }

        if (cursor == end) {
            return toZero();
        }

        long accumulated = 0;
        int accumulatedCount = 0;
        boolean fractional = false; // after '.'
        int fractionalDigits = 0;
        BigInteger unscaledValue = BigInteger.ZERO;

        while (cursor < end) {
            char ch = value.charAt(cursor);
            if (ch >= '0' && ch <= '9') {
                if (accumulatedCount == DecimalValue.LONG_MAX_DIGITS) {
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

        int scaleAdjust = getScale() - fractionalDigits;
        if (scaleAdjust > 0) {
            unscaledValue = unscaledValue.multiply(BigInteger.TEN.pow(scaleAdjust));
        } else if (scaleAdjust < 0) {
            unscaledValue = unscaledValue.divide(BigInteger.TEN.pow(-scaleAdjust));
        }

        if (negative) {
            unscaledValue = unscaledValue.negate();
        }

        return newValue(unscaledValue);
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
}
