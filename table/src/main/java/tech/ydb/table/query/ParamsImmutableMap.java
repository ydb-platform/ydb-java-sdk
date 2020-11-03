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

    private final Map<String, Value<?>> params;

    private ParamsImmutableMap(Map<String, Value<?>> params) {
        this.params = params;
    }

    static ParamsImmutableMap create(String name, Value<?> value) {
        return new ParamsImmutableMap(Collections.singletonMap(name, value));
    }

    static ParamsImmutableMap create(String name1, Value<?> value1, String name2, Value<?> value2) {
        checkArgument(!name1.equals(name2), "parameter duplicate: %s", name1);
        HashMap<String, Value<?>> params = Maps.newHashMapWithExpectedSize(2);
        params.put(name1, value1);
        params.put(name2, value2);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    static ParamsImmutableMap create(
            String name1, Value<?> value1,
            String name2, Value<?> value2,
            String name3, Value<?> value3) {
        HashMap<String, Value<?>> params = Maps.newHashMapWithExpectedSize(3);
        params.put(name1, value1);
        putParam(params, name2, value2);
        putParam(params, name3, value3);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    static ParamsImmutableMap create(
            String name1, Value<?> value1,
            String name2, Value<?> value2,
            String name3, Value<?> value3,
            String name4, Value<?> value4) {
        HashMap<String, Value<?>> params = Maps.newHashMapWithExpectedSize(4);
        params.put(name1, value1);
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
            String name5, Value<?> value5) {
        HashMap<String, Value<?>> params = Maps.newHashMapWithExpectedSize(5);
        params.put(name1, value1);
        putParam(params, name2, value2);
        putParam(params, name3, value3);
        putParam(params, name4, value4);
        putParam(params, name5, value5);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    static ParamsImmutableMap create(
            String name1, Value<?> value1,
            String name2, Value<?> value2,
            String name3, Value<?> value3,
            String name4, Value<?> value4,
            String name5, Value<?> value5,
            String name6, Value<?> value6,
            String name7, Value<?> value7,
            String name8, Value<?> value8,
            String name9, Value<?> value9) {
        HashMap<String, Value<?>> params = Maps.newHashMapWithExpectedSize(9);
        params.put(name1, value1);
        putParam(params, name2, value2);
        putParam(params, name3, value3);
        putParam(params, name4, value4);
        putParam(params, name5, value5);
        putParam(params, name6, value6);
        putParam(params, name7, value7);
        putParam(params, name8, value8);
        putParam(params, name9, value9);
        return new ParamsImmutableMap(Collections.unmodifiableMap(params));
    }

    private static void putParam(HashMap<String, Value<?>> params, String name, Value<?> value) {
        checkArgument(params.putIfAbsent(name, value) == null, "parameter duplicate: %s", name);
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
        Map<String, TypedValue> result = new HashMap<>(
                (int) ((((float) params.size()) / 0.75f) + 1.0f)
        );
        for (Map.Entry<String, Value<?>> entry : params.entrySet()) {
            result.put(entry.getKey(), toTypedValue(entry.getValue()));
        }
        return result;
    }

    @Override
    public Map<String, Value<?>> values() {
        return Collections.unmodifiableMap(params);
    }
}
