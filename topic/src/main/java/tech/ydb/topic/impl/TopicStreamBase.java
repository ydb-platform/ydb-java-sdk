package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.protobuf.Message;
import org.slf4j.Logger;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

public abstract class TopicStreamBase<R extends Message, W extends Message> implements TopicStream<R, W> {
    private final Logger logger;
    private final String debugId;
    private final W initRequest;
    private final CompletableFuture<Status> streamStatus = new CompletableFuture<>();
    private final AtomicReference<GrpcReadWriteStream<R, W>> ref;
    private volatile String token;

    public TopicStreamBase(Logger logger, String debugId, GrpcReadWriteStream<R, W> stream, W initRequest) {
        this.logger = logger;
        this.debugId = debugId;
        this.ref = new AtomicReference<>(stream);
        this.initRequest = initRequest;
        this.token = stream.authToken();
    }

    protected abstract W updateTokenMessage(String token);
    protected abstract Status parseMessageStatus(R message);

    @Override
    public CompletableFuture<Status> start(Consumer<R> messageHandler) {
        this.logger.debug("[{}] is about to start", debugId);
        GrpcReadWriteStream<R, W> stream = ref.get();
        if (stream == null) {
            return CompletableFuture.completedFuture(Status.of(StatusCode.CLIENT_INTERNAL_ERROR));
        }

        stream.start((R msg) -> {
            Status messageStatus = parseMessageStatus(msg);
            if (messageStatus.isSuccess()) {
                messageHandler.accept(msg);
            } else {
                logger.warn("[{}] stopped by getting status {}", debugId, messageStatus);
                preDestroy();
                streamStatus.complete(messageStatus);
                closeStream();
            }
        }).whenComplete((st, th) -> {
            ref.set(null);
            Status status = st != null ? st : Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
            logger.debug("[{}] finished with status {}", debugId, status);
            preDestroy();
            streamStatus.complete(status);
        });

        GrpcReadWriteStream<R, W> started = ref.get();
        if (started != null) {
            started.sendNext(initRequest);
        }
        return streamStatus;
    }

    @Override
    public void close() {
        logger.debug("[{}] closed by app", debugId);
        preDestroy();
        closeStream();
    }

    protected void preDestroy() {
        // nothing
    }

    private void closeStream() {
        GrpcReadWriteStream<R, W> stream = ref.getAndSet(null);
        if (stream != null) {
            stream.close();
        }
    }

    protected void fail(Status status) {
        logger.warn("[{}] stopped by fail {}", debugId, status);
        preDestroy();
        streamStatus.complete(status);
        closeStream();
    }

    @Override
    public void send(W req) {
        GrpcReadWriteStream<R, W> stream = ref.get();
        if (stream == null) {
            logger.warn("[{}] is already closed. Next message with type {} was NOT sent", debugId,
                    req.getDescriptorForType().getName());
            return;
        }

        String currentToken = stream.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.info("[{}] sends new token", debugId);
            stream.sendNext(updateTokenMessage(token));
        }

        stream.sendNext(req);
    }
}
