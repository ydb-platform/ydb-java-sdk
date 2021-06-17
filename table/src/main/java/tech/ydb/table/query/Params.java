package tech.ydb.table.query;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public interface Params {

    /**
     * Returns an immutable implementation of {@link Params} with no parameters.
     *
     * @return an empty {@link Params}
     */
    static Params empty() {
        return ParamsImmutableMap.EMPTY;
    }

    /**
     * Returns a modifiable implementation of {@link Params} with no parameters.
     *
     * @return an empty {@link Params}
     */
    static Params create() {
        return new ParamsMutableMap();
    }

    /**
     * Returns a modifiable implementation of {@link Params} with no parameters.
     *
     * @param initialCapacity initial capacity of backing map
     * @return an empty {@link Params}
     */
    static Params create(int initialCapacity) {
        return new ParamsMutableMap(initialCapacity);
    }

    /**
     * Returns an immutable implementation of {@link Params} with single parameter.
     *
     * @return non empty {@link Params} with single parameter
     */
    static Params of(String name, Value<?> value) {
        return ParamsImmutableMap.create(name, value);
    }

    /**
     * Returns an immutable implementation of {@link Params} with two parameters.
     *
     * @return non empty {@link Params} with two parameters
     */
    static Params of(String name1, Value<?> value1, String name2, Value<?> value2) {
        return ParamsImmutableMap.create(name1, value1, name2, value2);
    }

    /**
     * Returns an immutable implementation of {@link Params} with three parameters.
     *
     * @return non empty {@link Params} with three parameters
     */
    static Params of(
        String name1, Value<?> value1,
        String name2, Value<?> value2,
        String name3, Value<?> value3)
    {
        return ParamsImmutableMap.create(name1, value1, name2, value2, name3, value3);
    }

    /**
     * Returns an immutable implementation of {@link Params} with four parameters.
     *
     * @return non empty {@link Params} with four parameters
     */
    static Params of(
        String name1, Value<?> value1,
        String name2, Value<?> value2,
        String name3, Value<?> value3,
        String name4, Value<?> value4)
    {
        return ParamsImmutableMap.create(name1, value1, name2, value2, name3, value3, name4, value4);
    }

    /**
     * Returns an immutable implementation of {@link Params} with five parameters.
     *
     * @return non empty {@link Params} with five parameters
     */
    static Params of(
        String name1, Value<?> value1,
        String name2, Value<?> value2,
        String name3, Value<?> value3,
        String name4, Value<?> value4,
        String name5, Value<?> value5)
    {
        return ParamsImmutableMap.create(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5);
    }

    /**
     * Returns an immutable implementation of {@link Params} with six parameters.
     *
     * @return non empty {@link Params} with six parameters
     */
    static Params of(
            String name1, Value<?> value1,
            String name2, Value<?> value2,
            String name3, Value<?> value3,
            String name4, Value<?> value4,
            String name5, Value<?> value5,
            String name6, Value<?> value6)
    {
        return ParamsImmutableMap.create(
                name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6);
    }

    /**
     * Returns an immutable implementation of {@link Params} with eight parameters.
     *
     * @return non empty {@link Params} with eight parameters
     */
    static Params of(
            String name1, Value<?> value1,
            String name2, Value<?> value2,
            String name3, Value<?> value3,
            String name4, Value<?> value4,
            String name5, Value<?> value5,
            String name6, Value<?> value6,
            String name7, Value<?> value7,
            String name8, Value<?> value8)
    {
        return ParamsImmutableMap.create(
                name1, value1, name2, value2, name3, value3, name4, value4, name5, value5,
                name6, value6, name7, value7, name8, value8
        );
    }

    /**
     * Returns an immutable implementation of {@link Params} with nine parameters.
     *
     * @return non empty {@link Params} with nine parameters
     */
    static Params of(
            String name1, Value<?> value1,
            String name2, Value<?> value2,
            String name3, Value<?> value3,
            String name4, Value<?> value4,
            String name5, Value<?> value5,
            String name6, Value<?> value6,
            String name7, Value<?> value7,
            String name8, Value<?> value8,
            String name9, Value<?> value9)
    {
        return ParamsImmutableMap.create(
                name1, value1, name2, value2, name3, value3, name4, value4, name5, value5,
                name6, value6, name7, value7, name8, value8, name9, value9);
    }

    /**
     * Returns a mutable implementation of {@link Params} containing the same entries as given map.
     *
     * @param values    entries to be copied
     * @return {@link Params} containing specified values
     */
    static Params copyOf(Map<String, Value<?>> values) {
        return new ParamsMutableMap(values);
    }

    /**
     * Returns a mutable implementation of {@link Params} containing the same entries as given parameters.
     *
     * @param params    parameters to be copied
     * @return {@link Params} containing specified parameters
     */
    static Params copyOf(Params params) {
        return new ParamsMutableMap(params.values());
    }

    /**
     * Returns {@code true} if there are no defined parameters in this container.
     *
     * @return {@code true} if there are no defined parameters in this container.
     */
    boolean isEmpty();

    /**
     * Associates the specified value with the specified name in this params container.
     *
     * @param name      name with which the specified value is to be associated
     * @param value     value to be associated with the specified name
     * @return this params container
     */
    <T extends Type> Params put(String name, Value<T> value);

    /**
     * Converts each parameter value into Protobuf message {@link tech.ydb.ValueProtos.TypedValue}
     * and return them as unmodifiable map.
     *
     * @return map of converted parameters
     */
    Map<String, ValueProtos.TypedValue> toPb();

    /**
     * Returns original values as unmodifiable map.
     *
     * @return unmodifiable map of values
     */
    Map<String, Value<?>> values();
}
