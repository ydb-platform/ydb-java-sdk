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
    private final CompletableFuture<Status> streamStatus = new CompletableFuture<>();
    private volatile String token;

    public TopicStreamBase(Logger logger, String debugId, GrpcReadWriteStream<R, W> stream) {
        this.logger = logger;
        this.debugId = debugId;
        this.stream = stream;
        this.token = stream.authToken();
    }

    protected abstract W updateTokenMessage(String token);
    protected abstract Status parseMessageStatus(R message);

    @Override
    public CompletableFuture<Status> start(W initReq, Consumer<R> messageHandler) {
        this.logger.debug("[{}] is about to start", debugId);
        this.stream.start((R msg) -> {
            Status messageStatus = parseMessageStatus(msg);
            if (messageStatus.isSuccess()) {
                messageHandler.accept(msg);
            } else {
                logger.warn("[{}] stopped by getting status {}", debugId, messageStatus);
                if (streamStatus.complete(messageStatus)) {
                    stream.close();
                }
            }
        }).whenComplete((st, th) -> {
            Status status = st != null ? st : Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
            logger.debug("[{}] finished with status {}", debugId, status);
            streamStatus.complete(status);
        });

        if (!streamStatus.isDone()) {
            stream.sendNext(initReq);
        }

        return streamStatus;
    }

    @Override
    public void close() {
        logger.debug("[{}] closed by app", debugId);
        if (!streamStatus.isDone()) {
            stream.close();
        }
    }

    @Override
    public void send(W req) {
        if (streamStatus.isDone()) {
            logger.warn("[{}] is already closed. Next message with type {} was NOT sent", debugId,
                    req.getDescriptorForType().getName());
            return;
        }

        String currentToken = stream.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.info("{} sends new token", this);
            stream.sendNext(updateTokenMessage(token));
        }

        stream.sendNext(req);
    }
}
