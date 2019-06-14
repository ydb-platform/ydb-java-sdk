package tech.ydb.core.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.protobuf.Message;
import tech.ydb.OperationProtos.Operation;
import tech.ydb.core.Result;
import tech.ydb.core.Status;


/**
 *
 * @author Sergey Polovko
 */
public interface OperationTray extends AutoCloseable {

    /**
     * Awaits operation completion and retrieves its status.
     *
     * @param operation     operation to await for
     * @param deadlineAfter point in time when request must expire
     * @return future which will be completed with operation status
     */
    CompletableFuture<Status> waitStatus(Operation operation, long deadlineAfter);

    /**
     * Awaits operation completion and retrieves its result.
     *
     * @param operation     operation to await for
     * @param resultClass   class of operation result message
     * @param mapper        function to map result message to appropriate result*
     * @param deadlineAfter point in time when request must expire
     * @return future which will be completed with operation result
     */
    <M extends Message, R> CompletableFuture<Result<R>> waitResult(
        Operation operation,
        Class<M> resultClass,
        Function<M, R> mapper,
        long deadlineAfter);

    /**
     * Stops active tasks and release resources.
     */
    @Override
    void close();
}
