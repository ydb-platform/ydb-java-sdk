package ru.yandex.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.OperationProtos.GetOperationRequest;
import ru.yandex.ydb.OperationProtos.GetOperationResponse;
import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.rpc.Rpc;


/**
 * @author Sergey Polovko
 */
public interface OperationRpc extends Rpc {

    /**
     * Check status for a given operation.
     */
    CompletableFuture<Result<GetOperationResponse>> getOperation(GetOperationRequest request);

}
