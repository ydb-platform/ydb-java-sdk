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

    private static final DecimalType YDB_DEFAULT = DecimalType.of(22, 9);

    private final int precision;
    private final int scale;

    private DecimalType(int precision, int scale) {
        this.precision = precision;
        this.scale = scale;
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
}
