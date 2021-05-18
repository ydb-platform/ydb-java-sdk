package tech.ydb.table.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import tech.ydb.ValueProtos;
import tech.ydb.core.Result;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoType;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
final class DataQueryImpl implements DataQuery {

    private final SessionImpl session;
    private final String queryId;
    private final ImmutableMap<String, Type> types;
    private final ImmutableMap<String, ValueProtos.Type> typesPb;
    @Nullable
    private final String text;
    private final String textHash;

    DataQueryImpl(
            SessionImpl session,
            String queryId,
            String text,
            boolean keepText,
            Map<String, ValueProtos.Type> parametersTypes) {
        this.session = session;
        this.queryId = queryId;
        this.types = buildTypes(parametersTypes);
        this.typesPb = ImmutableMap.copyOf(parametersTypes);
        this.text = keepText ? text : null;
        this.textHash = makeHash(text);
    }

    static String makeHash(String text) {
        return Hashing.sha256()
                .hashString(text, StandardCharsets.UTF_8)
                .toString();
    }

    private static ImmutableMap<String, Type> buildTypes(Map<String, ValueProtos.Type> parametersTypes) {
        ImmutableMap.Builder<String, Type> types = new ImmutableMap.Builder<>();
        for (Map.Entry<String, ValueProtos.Type> e : parametersTypes.entrySet()) {
            types.put(e.getKey(), ProtoType.fromPb(e.getValue()));
        }
        return types.build();
    }

    @Override
    public String getId() {
        return queryId;
    }

    @Override
    public Params newParams() {
        return new DataQueryParams(types, typesPb);
    }

    @Override
    public Map<String, Type> types() {
        return types;
    }

    @Override
    public Optional<String> getText() {
        return Optional.ofNullable(text);
    }

    String getTextHash() {
        return textHash;
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> execute(
            TxControl txControl, Params params, ExecuteDataQuerySettings settings) {
        return session.executePreparedDataQuery(queryId, text, txControl, params, settings);
    }

    /**
     * Special implementation of the {@link Params} interface that will check
     * types of all values added to this container and reuses known in data query
     * protobuf {@link tech.ydb.ValueProtos.TypedValue} objects.
     */
    @ParametersAreNonnullByDefault
    static final class DataQueryParams implements Params {
        private final ImmutableMap<String, Type> types;
        private final ImmutableMap<String, ValueProtos.Type> typesPb;
        private final HashMap<String, Value<?>> params;

        DataQueryParams(ImmutableMap<String, Type> types, ImmutableMap<String, ValueProtos.Type> typesPb) {
            this.types = types;
            this.typesPb = typesPb;
            this.params = new HashMap<>(types.size());
        }

        @Override
        public boolean isEmpty() {
            return params.isEmpty();
        }

        @Override
        public <T extends Type> Params put(String name, Value<T> value) {
            Type type = types.get(name);
            checkArgument(type != null, "unknown parameter: %s", name);

            //TODO: This check will not work with Decimal type
            //checkArgument(type.equals(value.getType()), "types mismatch: expected %s, got %s", type, value.getType());

            Value<?> prev = params.putIfAbsent(name, value);
            Preconditions.checkArgument(prev == null, "duplicate parameter: %s", name);
            return this;
        }

        @Override
        public Map<String, ValueProtos.TypedValue> toPb() {
            Map<String, ValueProtos.TypedValue> result = new HashMap<>(params.size());
            for (Map.Entry<String, Value<?>> entry : params.entrySet()) {
                Value<?> value = entry.getValue();
                String name = entry.getKey();

                ValueProtos.Type typePb = Objects.requireNonNull(typesPb.get(name));
                ValueProtos.TypedValue valuePb = ValueProtos.TypedValue.newBuilder()
                        .setType(typePb)
                        .setValue(value.toPb())
                        .build();

                result.put(name, valuePb);
            }
            return Collections.unmodifiableMap(result);
        }

        @Override
        public Map<String, Value<?>> values() {
            return Collections.unmodifiableMap(params);
        }
    }
}
