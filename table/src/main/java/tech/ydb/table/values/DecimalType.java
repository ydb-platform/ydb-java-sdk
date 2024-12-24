package tech.ydb.table.values;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.google.common.base.Preconditions;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public class DecimalType implements Type {

    public static final int MAX_PRECISION = 35;

    private static final InfValues[] INF_VALUES;
    private static final DecimalType YDB_DEFAULT;

    static {
        // Precalculate +inf/-inf values for all precisions
        INF_VALUES = new InfValues[DecimalType.MAX_PRECISION];

        long mask32 = 0xFFFFFFFFL;
        long infHigh = 0;
        long infLow = 1;

        for (int precision = 1; precision <= DecimalType.MAX_PRECISION; precision++) {
            // multiply by 10
            long ll = 10 * (infLow & mask32);
            long lh = 10 * (infLow >>> 32) + (ll >>> 32);
            long hl = 10 * (infHigh & mask32) + (lh >>> 32);
            long hh = 10 * (infHigh >>> 32) + (hl >>> 32);

            infLow = (lh << 32) + (ll & mask32);
            infHigh = (hh << 32) + (hl & mask32);

            INF_VALUES[precision - 1] = new InfValues(infHigh, infLow);
        }

        YDB_DEFAULT = DecimalType.of(22, 9);
    }

    private final int precision;
    private final int scale;
    private final InfValues inf;

    private final DecimalValue infValue;
    private final DecimalValue negInfValue;
    private final DecimalValue nanValue;

    private DecimalType(int precision, int scale) {
        this.precision = precision;
        this.scale = scale;
        this.inf = INF_VALUES[precision - 1];

        this.infValue = new DecimalValue(this, DecimalValue.INF_HIGH, DecimalValue.INF_LOW);
        this.negInfValue = new DecimalValue(this, DecimalValue.NEG_INF_HIGH, DecimalValue.NEG_INF_LOW);
        this.nanValue = new DecimalValue(this, DecimalValue.NAN_HIGH, DecimalValue.NAN_LOW);
    }

    public static DecimalType getDefault() {
        return YDB_DEFAULT;
    }

    public static DecimalType of(int precision) {
        return of(precision, 0);
    }

    public static DecimalType of(int precision, int scale) {
        Preconditions.checkArgument(precision >= 1 && precision <= MAX_PRECISION,
            "precision (%s) is out of range [1, %s]", precision, MAX_PRECISION);
        Preconditions.checkArgument(scale >= 0 && scale <= precision,
            "scale (%s) is out of range [0, %s]", scale, precision);
        return new DecimalType(precision, scale);
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

    public DecimalValue getInf() {
        return infValue;
    }

    public DecimalValue getNegInf() {
        return negInfValue;
    }

    public DecimalValue getNaN() {
        return nanValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DecimalType that = (DecimalType) o;
        return precision == that.precision && scale == that.scale;
    }

    @Override
    public int hashCode() {
        return MAX_PRECISION * precision + scale;
    }

    @Override
    public String toString() {
        return "Decimal(" + precision + ", " + scale + ')';
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.getDecimal(precision, scale);
    }

    public DecimalValue newValue(long high, long low) {
        return DecimalValue.fromBits(this, high, low);
    }

    public DecimalValue newValue(long value) {
        return DecimalValue.fromLong(this, value);
    }

    public DecimalValue newValueUnsigned(long value) {
        return DecimalValue.fromUnsignedLong(this, value);
    }

    public DecimalValue newValueUnscaled(long value) {
        return DecimalValue.fromUnscaledLong(this, value);
    }

    public DecimalValue newValue(BigDecimal value) {
        return DecimalValue.fromBigDecimal(this, value);
    }

    public DecimalValue newValue(BigInteger value) {
        return DecimalValue.fromBigInteger(this, value);
    }

    public DecimalValue newValueUnscaled(BigInteger value) {
        return DecimalValue.fromUnscaledBigInteger(this, value);
    }

    public DecimalValue newValue(String value) {
        return DecimalValue.fromString(this, value);
    }

    boolean isInf(long high, long low) {
        return high > inf.posHigh || (high == inf.posHigh && Long.compareUnsigned(low, inf.posLow) >= 0);
    }

    boolean isNegInf(long high, long low) {
        return high < inf.negHigh || (high == inf.negHigh && Long.compareUnsigned(low, inf.negLow) <= 0);
    }

    private static class InfValues {
        private final long posHigh;
        private final long posLow;
        private final long negHigh;
        private final long negLow;

        InfValues(long infHigh, long infLow) {
            this.posHigh = infHigh;
            this.posLow = infLow;
            this.negHigh = 0xFFFFFFFFFFFFFFFFL - infHigh;
            this.negLow = 0xFFFFFFFFFFFFFFFFL - infLow + 1;
        }
    }
}
