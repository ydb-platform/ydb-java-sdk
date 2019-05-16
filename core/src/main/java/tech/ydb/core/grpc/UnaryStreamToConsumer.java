package tech.ydb.core.grpc;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;

import static com.yandex.yql.proto.IssueSeverity.TSeverityIds.ESeverityId.S_ERROR;


/**
 * @author Sergey Polovko
 */
public class UnaryStreamToConsumer<T> extends ClientCall.Listener<T> {

    private static final AtomicIntegerFieldUpdater<UnaryStreamToConsumer> acceptedUpdater =
        AtomicIntegerFieldUpdater.newUpdater(UnaryStreamToConsumer.class, "accepted");

    private final Consumer<Result<T>> consumer;
    private volatile int accepted = 0;
    private T value;

    public UnaryStreamToConsumer(Consumer<Result<T>> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            Issue issue = Issue.of("More than one value received for gRPC unary call", S_ERROR);
            accept(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        } else {
            this.value = value;
        }
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        if (status.isOk()) {
            if (value == null) {
                Issue issue = Issue.of("No value received for gRPC unary call", S_ERROR);
                accept(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
            } else {
                accept(Result.success(value));
            }
        } else {
            accept(GrpcStatuses.translate(status));
        }
    }

    void accept(Result<T> result) {
        // protect from double accept
        if (acceptedUpdater.compareAndSet(this, 0, 1)) {
            consumer.accept(result);
        }
    }
}
