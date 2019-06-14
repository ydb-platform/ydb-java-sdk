package tech.ydb.table.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Maps;
import tech.ydb.ValueProtos.TypedValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;

import static com.google.common.base.Preconditions.checkArgument;
import static tech.ydb.table.values.proto.ProtoValue.toTypedValue;


/**
 * Immutable implementation of the {@link Params} interface.
 *
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
final class ParamsImmutableMap implements Params {

    static final ParamsImmutableMap EMPTY = new ParamsImmutableMap(Collections.emptyMap());

    private final Map<String, TypedValue> params;

    private ParamsImmutableMap(Map<String, TypedValue> params) {
        this.params = params;
    }

    static ParamsImmutableMap create(String name, Value<?> value) {
        return new ParamsImmutableMap(Collections.singletonMap(name, toTypedValue(value)));
    }

    static ParamsImmutableMap create(String name1, Value<?> value1, String name2, Value<?> value2) {
        checkArgument(!name1.equals(name2), "parameter duplicate: %s", name1);
        HashMap<String, TypedValue> params = Maps.newHashMapWithExpectedSize(2);
        params.put(name1, toTypedValue(value1));
        params.put(name2, toTypedValue(value2));
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    static ParamsImmutableMap create(
        String name1, Value<?> value1,
        String name2, Value<?> value2,
        String name3, Value<?> value3)
    {
        HashMap<String, TypedValue> params = Maps.newHashMapWithExpectedSize(3);
        params.put(name1, toTypedValue(value1));
        putParam(params, name2, value2);
        putParam(params, name3, value3);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    static ParamsImmutableMap create(
        String name1, Value<?> value1,
        String name2, Value<?> value2,
        String name3, Value<?> value3,
        String name4, Value<?> value4)
    {
        HashMap<String, TypedValue> params = Maps.newHashMapWithExpectedSize(4);
        params.put(name1, toTypedValue(value1));
        putParam(params, name2, value2);
        putParam(params, name3, value3);
        putParam(params, name4, value4);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    static ParamsImmutableMap create(
        String name1, Value<?> value1,
        String name2, Value<?> value2,
        String name3, Value<?> value3,
        String name4, Value<?> value4,
        String name5, Value<?> value5)
    {
        HashMap<String, TypedValue> params = Maps.newHashMapWithExpectedSize(5);
        params.put(name1, toTypedValue(value1));
        putParam(params, name2, value2);
        putParam(params, name3, value3);
        putParam(params, name4, value4);
        putParam(params, name5, value5);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    private static void putParam(HashMap<String, TypedValue> params, String name, Value<?> value) {
        checkArgument(params.putIfAbsent(name, toTypedValue(value)) == null, "parameter duplicate: %s", name);
    }

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public <T extends Type> Params put(String name, Value<T> value) {
        throw new UnsupportedOperationException("cannot put parameter into immutable params map");
    }

    @Override
    public Map<String, TypedValue> toPb() {
        return params;
    }
}
