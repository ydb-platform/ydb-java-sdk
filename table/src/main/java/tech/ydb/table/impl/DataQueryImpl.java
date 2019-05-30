package tech.ydb.table.impl;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import tech.ydb.ValueProtos;
import tech.ydb.core.Result;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.types.Type;
import tech.ydb.table.types.proto.ProtoType;


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
        Map<String, ValueProtos.Type> parametersTypes)
    {
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
    public Params.KnownTypes newParams() {
        return Params.withKnownTypes(types, typesPb);
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
        TxControl txControl, Params params, ExecuteDataQuerySettings settings)
    {
        return session.executePreparedDataQuery(queryId, text, txControl, params, settings);
    }
}
