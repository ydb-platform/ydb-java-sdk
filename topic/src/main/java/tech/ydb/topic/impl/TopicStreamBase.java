package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.protobuf.Message;
import org.slf4j.Logger;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

public abstract class TopicStreamBase<R extends Message, W extends Message> implements TopicStream<R, W> {
    private final Logger logger;
    private final String debugId;
    private final GrpcReadWriteStream<R, W> stream;
    private final W initRequest;
    private final CompletableFuture<Completion> completion = new CompletableFuture<>();
    private final CompletableFuture<Status> streamStatus = completion.thenApply(value -> value.status);
    private volatile String token;
    private volatile boolean isStopped = false;

    public TopicStreamBase(Logger logger, String debugId, GrpcReadWriteStream<R, W> stream, W initRequest) {
        this.logger = logger;
        this.debugId = debugId;
        this.stream = stream;
        this.initRequest = initRequest;
        this.token = stream.authToken();
    }

    protected abstract W updateTokenMessage(String token);
    protected abstract Status parseMessageStatus(R message);

    @Override
    public CompletableFuture<Status> start(Consumer<R> messageHandler) {
        this.logger.debug("[{}] is about to start", debugId);
        this.stream.start((R msg) -> {
            Status messageStatus = parseMessageStatus(msg);
            if (messageStatus.isSuccess()) {
                messageHandler.accept(msg);
            } else {
                logger.warn("[{}] stopped by getting status {}", debugId, messageStatus);
                if (completion.complete(new Completion(messageStatus, null))) {
                    stream.close();
                }
            }
        }).whenComplete((st, th) -> {
            isStopped = true;
            Status status = st != null ? st : Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
            logger.debug("[{}] finished with status {}", debugId, status);
            completion.complete(new Completion(status, null));
        });

        if (!completion.isDone()) {
            stream.sendNext(initRequest);
        }

        return streamStatus;
    }

    @Override
    public void close() {
        logger.debug("[{}] closed by app", debugId);
        if (!completion.isDone()) {
            isStopped = true;
            stream.close();
        }
    }

    protected void fail(Status status) {
        fail(status, null);
    }

    protected void fail(Status status, String reason) {
        logger.warn("[{}] stopped by fail {}", debugId, status);
        if (completion.complete(new Completion(status, reason))) {
            isStopped = true;
            stream.close();
        }
    }

    @Override
    public String getFailureReason(Status status) {
        Completion value = completion.getNow(null);
        return value != null && value.status == status ? value.reason : null;
    }

    @Override
    public void send(W req) {
        if (isStopped) {
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

    private static final class Completion {
        private final Status status;
        private final String reason;

        Completion(Status status, String reason) {
            this.status = status;
            this.reason = reason;
        }
    }
}
