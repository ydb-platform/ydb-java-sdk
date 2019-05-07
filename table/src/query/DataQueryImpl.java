package ru.yandex.ydb.table.query;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.table.OperationsTray;
import ru.yandex.ydb.table.YdbTable;
import ru.yandex.ydb.table.rpc.TableRpc;
import ru.yandex.ydb.table.settings.ExecuteDataQuerySettings;
import ru.yandex.ydb.table.transaction.TxControl;
import ru.yandex.ydb.table.types.Type;
import ru.yandex.ydb.table.types.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public class DataQueryImpl implements DataQuery {

    private final String sessionId;
    private final TableRpc tableRpc;
    private final OperationsTray operationsTray;
    private final String queryId;
    private final ImmutableMap<String, Type> types;
    private final ImmutableMap<String, ValueProtos.Type> typesPb;

    public DataQueryImpl(
        String sessionId, TableRpc tableRpc, OperationsTray operationsTray,
        String queryId, Map<String, ValueProtos.Type> parametersTypes)
    {
        this.sessionId = sessionId;
        this.tableRpc = tableRpc;
        this.operationsTray = operationsTray;
        this.queryId = queryId;

        ImmutableMap.Builder<String, Type> types = new ImmutableMap.Builder<>();
        for (Map.Entry<String, ValueProtos.Type> e : parametersTypes.entrySet()) {
            types.put(e.getKey(), ProtoType.fromPb(e.getValue()));
        }
        this.types = types.build();

        this.typesPb = ImmutableMap.copyOf(parametersTypes);
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
    public CompletableFuture<Result<DataQueryResult>> execute(
        TxControl txControl, Params params, ExecuteDataQuerySettings settings)
    {
        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
            .setSessionId(sessionId)
            .setTxControl(txControl.toPb());

        request.getQueryBuilder().setId(queryId);
        request.putAllParameters(params.toPb());

        return tableRpc.executeDataQuery(request.build())
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("executeDataQuery()").getOperation(),
                    YdbTable.ExecuteQueryResult.class,
                    DataQueryImpl::mapExecuteDataQuery);
            });
    }

    private static DataQueryResult mapExecuteDataQuery(YdbTable.ExecuteQueryResult result) {
        YdbTable.TransactionMeta txMeta = result.getTxMeta();
        return new DataQueryResult(txMeta.getId(), result.getResultSetsList());
    }
}
