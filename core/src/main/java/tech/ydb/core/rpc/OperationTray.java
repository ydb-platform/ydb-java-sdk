package tech.ydb.core.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.protobuf.Message;
import tech.ydb.OperationProtos.Operation;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;


/**
 *
 * @author Sergey Polovko
 */
public interface OperationTray extends AutoCloseable {

    /**
     * Awaits operation completion and retrieves its status.
     *
     * @param operation     operation to await for
     * @param settings      rpc call settings
     * @return future which will be completed with operation status
     */
    CompletableFuture<Status> waitStatus(Operation operation, GrpcRequestSettings settings);

    /**
     * Awaits operation completion and retrieves its result.
     *
     * @param operation     operation to await for
     * @param resultClass   class of operation result message
     * @param mapper        function to map result message to appropriate result*
     * @param settings      rpc call settings
     * @return future which will be completed with operation result
     */
    <M extends Message, R> CompletableFuture<Result<R>> waitResult(
        Operation operation,
        Class<M> resultClass,
        Function<M, R> mapper,
        GrpcRequestSettings settings);

    /**
     * Stops active tasks and release resources.
     */
    @Override
    void close();
}
