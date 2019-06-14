package tech.ydb.table.settings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.TupleValue;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class ReadTableSettings {

    private final boolean ordered;
    private final TupleValue fromKey;
    private final boolean fromInclusive;
    private final TupleValue toKey;
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
    public TupleValue getFromKey() {
        return fromKey;
    }

    public boolean isFromInclusive() {
        return fromInclusive;
    }

    @Nullable
    public TupleValue getToKey() {
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
        private TupleValue fromKey = null;
        private boolean fromInclusive = false;
        private TupleValue toKey = null;
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

        public Builder fromKey(TupleValue value, boolean inclusive) {
            this.fromKey = value;
            this.fromInclusive = inclusive;
            return this;
        }

        public Builder toKey(TupleValue value, boolean inclusive) {
            this.toKey = value;
            this.toInclusive = inclusive;
            return this;
        }

        public Builder fromKeyInclusive(TupleValue value) {
            return fromKey(value, true);
        }

        public Builder fromKeyExclusive(TupleValue value) {
            return fromKey(value, false);
        }

        public Builder fromKeyInclusive(PrimitiveValue value) {
            return fromKey(TupleValue.of(value.makeOptional()), true);
        }

        public Builder fromKeyExclusive(PrimitiveValue value) {
            return fromKey(TupleValue.of(value.makeOptional()), false);
        }

        public Builder toKeyInclusive(TupleValue value) {
            return toKey(value, true);
        }

        public Builder toKeyExclusive(TupleValue value) {
            return toKey(value, false);
        }

        public Builder toKeyInclusive(PrimitiveValue value) {
            return toKey(TupleValue.of(value.makeOptional()), true);
        }

        public Builder toKeyExclusive(PrimitiveValue value) {
            return toKey(TupleValue.of(value.makeOptional()), false);
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

