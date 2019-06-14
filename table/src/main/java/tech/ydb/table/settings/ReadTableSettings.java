package tech.ydb.table.settings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import tech.ydb.table.types.TupleType;
import tech.ydb.table.types.Type;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.TypedValue;
import tech.ydb.table.values.Value;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class ReadTableSettings {

    private final boolean ordered;
    private final TypedValue<?> fromKey;
    private final boolean fromInclusive;
    private final TypedValue<?> toKey;
    private final boolean toInclusive;
    private final int rowLimit;
    private final ImmutableList<String> columns;
    private final long timeoutNanos;

    private ReadTableSettings(Builder b) {
        this.ordered = b.ordered;
        this.fromKey = b.fromKey;
        this.fromInclusive = b.fromInclusive;
        this.toKey = b.toKey;
        this.toInclusive = b.toInclusive;
        this.rowLimit = b.rowLimit;
        this.columns = ImmutableList.copyOf(b.columns);
        this.timeoutNanos = b.timeoutNanos;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public boolean isOrdered() {
        return ordered;
    }

    @Nullable
    public TypedValue<?> getFromKey() {
        return fromKey;
    }

    public boolean isFromInclusive() {
        return fromInclusive;
    }

    @Nullable
    public TypedValue<?> getToKey() {
        return toKey;
    }

    public boolean isToInclusive() {
        return toInclusive;
    }

    public int getRowLimit() {
        return rowLimit;
    }

    public ImmutableList<String> getColumns() {
        return columns;
    }

    public long getTimeoutNanos() {
        return timeoutNanos;
    }

    public long getDeadlineAfter() {
        return System.nanoTime() + timeoutNanos;
    }

    /**
     * BUILDER
     */
    @ParametersAreNonnullByDefault
    public static final class Builder {
        private boolean ordered = false;
        private TypedValue<?> fromKey = null;
        private boolean fromInclusive = false;
        private TypedValue<?> toKey = null;
        private boolean toInclusive = false;
        private int rowLimit = 0;
        private List<String> columns = Collections.emptyList();
        private long timeoutNanos = 0;

        Builder() {
        }

        public Builder orderedRead(boolean ordered) {
            this.ordered = ordered;
            return this;
        }

        private <T extends Type> Builder fromKey(T type, Value<T> value, boolean inclusive) {
            checkArgument(type.getKind() == Type.Kind.TUPLE, "from key must have a tuple type");
            checkArgument(value instanceof TupleValue, "from key must be a tuple value");
            this.fromKey = new TypedValue<>(type, value);
            this.fromInclusive = inclusive;
            return this;
        }

        private <T extends Type> Builder toKey(T type, Value<T> value, boolean inclusive) {
            checkArgument(type.getKind() == Type.Kind.TUPLE, "to key must have a tuple type");
            checkArgument(value instanceof TupleValue, "to key must be a tuple value");
            this.toKey = new TypedValue<>(type, value);
            this.toInclusive = inclusive;
            return this;
        }

        public <T extends Type> Builder fromKeyInclusive(T type, Value<T> value) {
            return fromKey(type, value, true);
        }

        public <T extends Type> Builder fromKeyExclusive(T type, Value<T> value) {
            return fromKey(type, value, false);
        }

        public Builder fromKeyInclusive(PrimitiveValue value) {
            TupleType keyType = TupleType.of(value.getType().makeOptional());
            TupleValue keyValue = TupleValue.of(value.makeOptional());
            return fromKey(keyType, keyValue, true);
        }

        public Builder fromKeyExclusive(PrimitiveValue value) {
            TupleType keyType = TupleType.of(value.getType().makeOptional());
            TupleValue keyValue = TupleValue.of(value.makeOptional());
            return fromKey(keyType, keyValue, false);
        }

        public <T extends Type> Builder toKeyInclusive(T type, Value<T> value) {
            return toKey(type, value, true);
        }

        public <T extends Type> Builder toKeyExclusive(T type, Value<T> value) {
            return toKey(type, value, false);
        }

        public Builder toKeyInclusive(PrimitiveValue value) {
            TupleType keyType = TupleType.of(value.getType().makeOptional());
            TupleValue keyValue = TupleValue.of(value.makeOptional());
            return toKey(keyType, keyValue, true);
        }

        public Builder toKeyExclusive(PrimitiveValue value) {
            TupleType keyType = TupleType.of(value.getType().makeOptional());
            TupleValue keyValue = TupleValue.of(value.makeOptional());
            return toKey(keyType, keyValue, false);
        }

        public Builder rowLimit(int rowLimit) {
            checkArgument(rowLimit >= 0, "rowLimit(%d) is negative", rowLimit);
            this.rowLimit = rowLimit;
            return this;
        }

        public Builder columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public Builder columns(String... columns) {
            this.columns = ImmutableList.copyOf(columns);
            return this;
        }

        public Builder column(String column) {
            if (!(this.columns instanceof ArrayList)) {
                this.columns = new ArrayList<>(this.columns);
            }
            this.columns.add(column);
            return this;
        }

        public Builder timeout(long duration, TimeUnit unit) {
            this.timeoutNanos = unit.toNanos(duration);
            return this;
        }

        public Builder timeout(Duration duration) {
            this.timeoutNanos = duration.toNanos();
            return this;
        }

        public ReadTableSettings build() {
            return new ReadTableSettings(this);
        }
    }
}

