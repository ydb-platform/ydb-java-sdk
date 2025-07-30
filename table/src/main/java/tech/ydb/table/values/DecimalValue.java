package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;

/**
 * @author Sergey Polovko
 */
public class DecimalValue implements Value<DecimalType> {
    private static final DecimalType MAX_DECIMAL = DecimalType.of(DecimalType.MAX_PRECISION);

    private static final long HALF_LONG_MASK = 0xFFFFFFFFL;
    private static final long LONG_SIGN_BIT = 0x8000000000000000L;
    private static final long LONG_MAX_DIGITS = 18;

    private static final BigInteger BIGINT_TWO = BigInteger.valueOf(2);

    static final long INF_HIGH = 0x0013426172C74D82L;
    static final long INF_LOW = 0x2B878FE800000000L;
    static final long NEG_INF_HIGH = 0xFFECBD9E8D38B27DL;
    static final long NEG_INF_LOW = 0xD478701800000000L;
    static final long NAN_HIGH = 0x0013426172C74D82L;
    static final long NAN_LOW = 0x2B878FE800000001L;


    /**
     * @deprecated
     * Positive infinity 10^MAX_PRECISION.
     * Use {@link DecimalType#getInf() } instead
     */
    @Deprecated
    public static final DecimalValue INF = new DecimalValue(
        MAX_DECIMAL, 0x0013426172C74D82L, 0x2B878FE800000000L);

    /**
     * @deprecated
     * Negative infinity -10^MAX_PRECISI0ON.
     * Use {@link DecimalType#getNegInf() } instead
     */
    @Deprecated
    public static final DecimalValue NEG_INF = new DecimalValue(
        MAX_DECIMAL, 0xFFECBD9E8D38B27DL, 0xD478701800000000L);

    /**
     * @deprecated
     * Not a number 10^MAX_PRECISION + 1.
     * Use {@link DecimalType#getNaN() } instead
     */
    @Deprecated
    public static final DecimalValue NAN = new DecimalValue(
        MAX_DECIMAL, 0x0013426172C74D82L, 0x2B878FE800000001L);

    private static final long serialVersionUID = 1362798151263983188L;

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
        return this.high == INF_HIGH && this.low == INF_LOW;
    }

    public boolean isNegativeInf() {
        return this.high == NEG_INF_HIGH && this.low == NEG_INF_LOW;
    }

    public boolean isNan() {
        return this.high == NAN_HIGH && this.low == NAN_LOW;
    }

    public boolean isZero() {
        return high == 0 && low == 0;
    }

    public boolean isNegative() {
        return (high & LONG_SIGN_BIT) != 0;
    }

    public BigInteger toUnscaledBigInteger() {
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

    public BigInteger toBigInteger() {
        if (isZero()) {
            return BigInteger.ZERO;
        }

        BigInteger unscaled = toUnscaledBigInteger();
        if (type.getScale() == 0) {
            return unscaled;
        }

        BigInteger scale = BigInteger.TEN.pow(type.getScale());
        BigInteger halfEven = scale.divide(BIGINT_TWO);
        BigInteger[] scaled = unscaled.divideAndRemainder(BigInteger.TEN.pow(type.getScale()));

        // round positive value
        if (unscaled.signum() > 0 && scaled[1].compareTo(halfEven) >= 0) {
            return scaled[0].add(BigInteger.ONE);
        }
        // round negative value
        if (unscaled.signum() < 0 && scaled[1].negate().compareTo(halfEven) >= 0) {
            return scaled[0].add(BigInteger.ONE.negate());
        }
        return scaled[0];
    }

    public BigDecimal toBigDecimal() {
        if (isZero()) {
            return BigDecimal.ZERO.setScale(type.getScale());
        }
        if (isInf() || isNegativeInf() || isNan()) {
            return new BigDecimal(toUnscaledBigInteger()).setScale(type.getScale());
        }

        return new BigDecimal(toUnscaledBigInteger(), type.getScale());
    }

    public long toLong() {
        return toBigInteger().longValueExact();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DecimalValue that = (DecimalValue) o;
        return high == that.high && low == that.low && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + Long.hashCode(high);
        result = 31 * result + Long.hashCode(low);
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

        StringBuilder sb = new StringBuilder(DecimalType.MAX_PRECISION + 4);
        writeAsString(sb, type.getScale(), high, low);
        return sb.toString();
    }

    public String toUnscaledString() {
        if (isZero()) {
            return "0";
        }

        StringBuilder sb = new StringBuilder(DecimalType.MAX_PRECISION + 2);
        writeAsString(sb, 0, high, low);
        return sb.toString();
    }

    private static void writeAsString(StringBuilder sb, int scale, long ahigh, long alow) {
        long high = ahigh;
        long low = alow;
        boolean isNegative = (ahigh & LONG_SIGN_BIT) != 0;
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
        long lowLo = low & HALF_LONG_MASK;

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

            sb.append(Character.forDigit((int) (remainder % divisor), divisor));

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

        if (isNegative) {
            sb.append('-');
        }

        sb.reverse();
    }

    @Override
    public ValueProtos.Value toPb() {
        return ProtoValue.fromDecimal(high, low);
    }

    @Override
    public int compareTo(Value<?> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare with null value");
        }

        // Handle comparison with OptionalValue
        if (other instanceof OptionalValue) {
            OptionalValue otherOptional = (OptionalValue) other;

            // Check that the item type matches this decimal type
            if (!getType().equals(otherOptional.getType().getItemType())) {
                throw new IllegalArgumentException(
                    "Cannot compare DecimalValue with OptionalValue of different item type: " +
                    getType() + " vs " + otherOptional.getType().getItemType());
            }

            // Non-empty value is greater than empty optional
            if (!otherOptional.isPresent()) {
                return 1;
            }

            // Compare with the wrapped value
            return compareTo(otherOptional.get());
        }

        if (!(other instanceof DecimalValue)) {
            throw new IllegalArgumentException("Cannot compare DecimalValue with " + other.getClass().getSimpleName());
        }

        DecimalValue otherDecimal = (DecimalValue) other;

        // Handle special values first
        if (isNan() || otherDecimal.isNan()) {
            // NaN is not comparable, but we need to provide a consistent ordering
            if (isNan() && otherDecimal.isNan()) {
                return 0;
            }
            if (isNan()) {
                return 1; // NaN is considered greater than any other value
            }
            return -1;
        }

        if (isInf() && otherDecimal.isInf()) {
            return 0;
        }
        if (isInf()) {
            return 1; // Positive infinity is greater than any finite value
        }
        if (otherDecimal.isInf()) {
            return -1;
        }

        if (isNegativeInf() && otherDecimal.isNegativeInf()) {
            return 0;
        }
        if (isNegativeInf()) {
            return -1; // Negative infinity is less than any finite value
        }
        if (otherDecimal.isNegativeInf()) {
            return 1;
        }

        // Compare finite values
        if (isNegative() != otherDecimal.isNegative()) {
            return isNegative() ? -1 : 1;
        }

        // Both have the same sign, compare magnitudes
        int highComparison = Long.compareUnsigned(high, otherDecimal.high);
        if (highComparison != 0) {
            return isNegative() ? -highComparison : highComparison;
        }

        int lowComparison = Long.compareUnsigned(low, otherDecimal.low);
        return isNegative() ? -lowComparison : lowComparison;
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

    private static boolean isNan(long high, long low) {
        return NAN.high == high && NAN.low == low;
    }

    static DecimalValue fromUnscaledLong(DecimalType type, long low) {
        if (low == 0) {
            return new DecimalValue(type, 0, 0);
        }

        long high = low > 0 ? 0 : -1;

        if (type.isInf(high, low)) {
            return type.getInf();
        }

        if (type.isNegInf(high, low)) {
            return type.getNegInf();
        }

        return new DecimalValue(type, high, low);
    }

    static DecimalValue fromBits(DecimalType type, long high, long low) {
        if (high == 0 && low == 0) {
            return new DecimalValue(type, 0, 0);
        }

        if (isNan(high, low)) {
            return type.getNaN();
        }

        if (type.isInf(high, low)) {
            return type.getInf();
        }

        if (type.isNegInf(high, low)) {
            return type.getNegInf();
        }

        return new DecimalValue(type, high, low);
    }

    static DecimalValue fromUnscaledBigInteger(DecimalType type, BigInteger value) {
        int bitLength = value.bitLength();
        if (bitLength < 64) {
            return fromUnscaledLong(type, value.longValue());
        }

        boolean negative = value.signum() < 0;
        if (bitLength > 128) {
            return negative ? type.getNegInf() : type.getInf();
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
        return fromBits(type, high, low);
    }

    private static DecimalValue fromUnsignedLong(DecimalType type, boolean positive, long value) {
        if (value == 0) {
            return new DecimalValue(type, 0L, 0L);
        }

        long high = 0;
        long lowHi = value >>> 32;
        long lowLo = value & HALF_LONG_MASK;

        for (int scale = 0; scale < type.getScale(); scale += 1) {
            lowLo = lowLo * 10;
            lowHi = lowHi * 10 + (lowLo >>> 32);
            high = high * 10 + (lowHi >>> 32);

            lowLo = lowLo & HALF_LONG_MASK;
            lowHi = lowHi & HALF_LONG_MASK;
            if ((high & LONG_SIGN_BIT) != 0) {
                // number is too big, return infinite
                return positive ? type.getInf() : type.getNegInf();
            }
        }

        long low = lowHi << 32 | lowLo;

        if (!positive && (high != LONG_SIGN_BIT || low != 0)) {
            // restore negative number
            high = ~high;
            low = ~low;
            if (++low == 0) {
                high++;
            }
        }
        return fromBits(type, high, low);
    }

    static DecimalValue fromUnsignedLong(DecimalType type, long value) {
        return fromUnsignedLong(type, true, value);
    }

    static DecimalValue fromLong(DecimalType type, long value) {
        boolean positive = value > 0;
        return fromUnsignedLong(type, positive, positive ? value : -value);
    }

    static DecimalValue fromString(DecimalType type, String value) {
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
                return negative ? type.getNegInf() : type.getInf();
            }

            if ((c1 == 'n' || c1 == 'N') && (c2 == 'a' || c2 == 'A') || (c3 == 'n' || c3 == 'N')) {
                return type.getNaN();
            }
        }

        // skip leading zeros
        while (cursor < end && value.charAt(cursor) == '0') {
            ++cursor;
        }

        if (cursor == end) {
            return new DecimalValue(type, 0, 0);
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
                    if (unscaledValue.equals(BigInteger.ZERO)) {
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
            if (unscaledValue.equals(BigInteger.ZERO)) {
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

        return fromUnscaledBigInteger(type, unscaledValue);
    }

    static DecimalValue fromBigInteger(DecimalType type, BigInteger value) {
        BigInteger rawValue = value;
        int scale = type.getScale();
        if (scale > 0) {
            rawValue = rawValue.multiply(BigInteger.TEN.pow(scale));
        }
        return DecimalValue.fromUnscaledBigInteger(type, rawValue);
    }

    static DecimalValue fromBigDecimal(DecimalType type, BigDecimal value) {
        BigInteger rawValue = value.unscaledValue();

        int scaleAdjust = type.getScale() - value.scale();
        if (scaleAdjust > 0) {
            rawValue = rawValue.multiply(BigInteger.TEN.pow(scaleAdjust));
        } else if (scaleAdjust < 0) {
            rawValue = rawValue.divide(BigInteger.TEN.pow(-scaleAdjust));
        }

        return DecimalValue.fromUnscaledBigInteger(type, rawValue);
    }
}
