package tech.ydb.table.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
public abstract class Params {

    public abstract boolean isEmpty();

    public abstract Map<String, ValueProtos.TypedValue> toPb();

    public static Empty empty() {
        return Empty.INSTANCE;
    }

    public static UnknownTypes withUnknownTypes() {
        return new UnknownTypes();
    }

    public static KnownTypes withKnownTypes(
        ImmutableMap<String, Type> types,
        ImmutableMap<String, ValueProtos.Type> typesPb)
    {
        return new KnownTypes(types, typesPb);
    }

    public static KnownTypes withKnownTypes(ImmutableMap<String, Type> types) {
        ImmutableMap.Builder<String, ValueProtos.Type> typesPb = new ImmutableMap.Builder<>();
        for (Map.Entry<String, Type> e : types.entrySet()) {
            typesPb.put(e.getKey(), e.getValue().toPb());
        }
        return new KnownTypes(types, typesPb.build());
    }

    /**
     * EMPTY
     */
    public static final class Empty extends Params {
        private static final Empty INSTANCE = new Empty();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Map<String, ValueProtos.TypedValue> toPb() {
            return Collections.emptyMap();
        }
    }

    /**
     * UNKNOWN TYPES
     */
    public static final class UnknownTypes extends Params {

        private final Map<String, ValueProtos.TypedValue> params = new HashMap<>();

        private UnknownTypes() {
        }

        @Override
        public boolean isEmpty() {
            return params.isEmpty();
        }

        @Override
        public Map<String, ValueProtos.TypedValue> toPb() {
            return Collections.unmodifiableMap(params);
        }

        public <T extends Type> UnknownTypes put(String name, T type, Value<T> value) {
            this.params.put(name, ValueProtos.TypedValue.newBuilder()
                .setType(type.toPb())
                .setValue(value.toPb())
                .build());
            return this;
        }

        public UnknownTypes put(String name, PrimitiveValue value) {
            this.params.put(name, ValueProtos.TypedValue.newBuilder()
                .setType(value.getType().toPb())
                .setValue(value.toPb())
                .build());
            return this;
        }
    }

    /**
     * KNOWN TYPES
     */
    public static final class KnownTypes extends Params {

        private final ImmutableMap<String, Type> types;
        private final ImmutableMap<String, ValueProtos.Type> typesPb;
        private final Map<String, ValueProtos.TypedValue> params = new HashMap<>();

        private KnownTypes(
            ImmutableMap<String, Type> types,
            ImmutableMap<String, ValueProtos.Type> typesPb)
        {
            this.types = types;
            this.typesPb = typesPb;
        }

        @Override
        public boolean isEmpty() {
            return types.isEmpty();
        }

        @Override
        public Map<String, ValueProtos.TypedValue> toPb() {
            return Collections.unmodifiableMap(params);
        }

        public <T extends Type> KnownTypes put(String name, Value<T> value) {
            Type type = types.get(name);
            checkArgument(type != null, "unknown parameter: %s", name);
            checkArgument(type.equals(value.getType()), "types mismatch: expected %s, got %s", type, value.getType());

            params.put(name, ValueProtos.TypedValue.newBuilder()
                .setType(Objects.requireNonNull(typesPb.get(name)))
                .setValue(value.toPb())
                .build());
            return this;
        }
    }
}
