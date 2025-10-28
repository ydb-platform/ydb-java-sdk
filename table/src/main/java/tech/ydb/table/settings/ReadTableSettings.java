package tech.ydb.table.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

import tech.ydb.core.grpc.GrpcFlowControl;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.proto.ProtoValue;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class ReadTableSettings extends BaseRequestSettings {

    private final boolean ordered;
    private final ValueProtos.TypedValue fromKey;
    private final boolean fromInclusive;
    private final ValueProtos.TypedValue toKey;
    private final boolean toInclusive;
    private final int rowLimit;
    private final ImmutableList<String> columns;
    private final int batchLimitBytes;
    private final int batchLimitRows;
    private final GrpcFlowControl flowControl;

    private ReadTableSettings(Builder builder) {
        super(builder);
        this.ordered = builder.ordered;
        this.fromKey = builder.fromKey;
        this.fromInclusive = builder.fromInclusive;
        this.toKey = builder.toKey;
        this.toInclusive = builder.toInclusive;
        this.rowLimit = builder.rowLimit;
        this.columns = ImmutableList.copyOf(builder.columns);
        this.batchLimitBytes = builder.batchLimitBytes;
        this.batchLimitRows = builder.batchLimitRows;
        this.flowControl = builder.flowControl;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public boolean isOrdered() {
        return ordered;
    }

    @Nullable
    public TupleValue getFromKey() {
        return fromKey == null ? null : ProtoValueReaders.forTypedValue(fromKey).getValue().asTuple();
    }

    @Nullable
    public ValueProtos.TypedValue getFromKeyRaw() {
        return fromKey;
    }

    public boolean isFromInclusive() {
        return fromInclusive;
    }

    @Nullable
    public TupleValue getToKey() {
        return toKey == null ? null : ProtoValueReaders.forTypedValue(toKey).getValue().asTuple();
    }

    @Nullable
    public ValueProtos.TypedValue getToKeyRaw() {
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

    public int batchLimitBytes() {
        return batchLimitBytes;
    }

    public int batchLimitRows() {
        return batchLimitRows;
    }

    public GrpcFlowControl getGrpcFlowControl() {
        return flowControl;
    }

    /**
     * BUILDER
     */
    public static final class Builder extends BaseBuilder<Builder> {
        private boolean ordered = false;
        private ValueProtos.TypedValue fromKey = null;
        private boolean fromInclusive = false;
        private ValueProtos.TypedValue toKey = null;
        private boolean toInclusive = false;
        private int rowLimit = 0;
        private List<String> columns = Collections.emptyList();
        private int batchLimitBytes = 0;
        private int batchLimitRows = 0;
        private GrpcFlowControl flowControl = null;

        public Builder orderedRead(boolean ordered) {
            this.ordered = ordered;
            return this;
        }

        public Builder fromKey(TupleValue value, boolean inclusive) {
            this.fromKey = ProtoValue.toTypedValue(value);
            this.fromInclusive = inclusive;
            return this;
        }

        public Builder fromKey(ValueProtos.TypedValue value, boolean inclusive) {
            this.fromKey = value;
            this.fromInclusive = inclusive;
            return this;
        }

        public Builder toKey(TupleValue value, boolean inclusive) {
            this.toKey = ProtoValue.toTypedValue(value);
            this.toInclusive = inclusive;
            return this;
        }

        public Builder toKey(ValueProtos.TypedValue value, boolean inclusive) {
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

        public Builder batchLimitBytes(int batchLimitBytes) {
            this.batchLimitBytes = batchLimitBytes;
            return this;
        }

        public Builder batchLimitRows(int batchLimitRows) {
            this.batchLimitRows = batchLimitRows;
            return this;
        }

        public Builder withGrpcFlowControl(GrpcFlowControl ctrl) {
            this.flowControl = ctrl;
            return this;
        }

        @Override
        public ReadTableSettings build() {
            return new ReadTableSettings(this);
        }
    }
}

