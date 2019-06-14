package tech.ydb.table.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * Mutable implementation of {@link Params} interface.
 *
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
final class ParamsMutableMap implements Params {

    private final HashMap<String, ValueProtos.TypedValue> params;

    ParamsMutableMap() {
        this.params = new HashMap<>();
    }

    ParamsMutableMap(Map<String, ValueProtos.TypedValue> params) {
        this.params = new HashMap<>(params);
    }

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public <T extends Type> Params put(String name, Value<T> value) {
        ValueProtos.TypedValue valuePb = ProtoValue.toTypedValue(value);
        ValueProtos.TypedValue prev = params.putIfAbsent(name, valuePb);
        Preconditions.checkArgument(prev == null, "duplicate parameter: %s", name);
        return this;
    }

    @Override
    public Map<String, ValueProtos.TypedValue> toPb() {
        return Collections.unmodifiableMap(params);
    }
}
