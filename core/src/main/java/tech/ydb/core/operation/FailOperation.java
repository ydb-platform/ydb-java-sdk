package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Message;

import tech.ydb.core.Result;

/**
 * @author Kirill Kurdyukov
 */
class FailOperation<V extends Message> extends Operation<V> {

    FailOperation(Result<V> resultFailed) {
        super(null, null, null);

        resultCompletableFuture.complete(resultFailed);
    }

    @Override
    public CompletableFuture<Result<V>> cancel() {
        return getResultFuture();
    }
}
