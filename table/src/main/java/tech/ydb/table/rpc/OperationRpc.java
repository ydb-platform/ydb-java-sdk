package tech.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.OperationProtos.GetOperationRequest;
import tech.ydb.OperationProtos.GetOperationResponse;
import tech.ydb.core.Result;
import tech.ydb.core.rpc.Rpc;


/**
 * @author Sergey Polovko
 */
public interface OperationRpc extends Rpc {

    /**
     * Check status for a given operation.
     */
    CompletableFuture<Result<GetOperationResponse>> getOperation(GetOperationRequest request);

}
