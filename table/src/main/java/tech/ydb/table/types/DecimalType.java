package tech.ydb.table.types;

import com.google.common.base.Preconditions;


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
}
