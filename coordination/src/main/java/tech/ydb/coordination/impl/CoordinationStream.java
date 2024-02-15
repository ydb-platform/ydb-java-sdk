package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationStream implements AutoCloseable, GrpcReadWriteStream.Observer<SessionResponse> {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationStream.class);

    private final GrpcReadWriteStream<SessionResponse, SessionRequest> stream;
    private final CompletableFuture<Status> finishFuture = new CompletableFuture<>();
    private final CompletableFuture<Result<Long>> initFuture = new CompletableFuture<>();

    public CoordinationStream(CoordinationRpc rpc, GrpcRequestSettings settings) {
        this.stream = rpc.createSession(settings);
    }

    public CompletableFuture<Status> connect() {
        stream.start(this).whenComplete((status, th) -> {
            if (th != null) {
                finishFuture.completeExceptionally(th);
                initFuture.completeExceptionally(th);
            }
            if (status != null) {
                finishFuture.complete(status);
                initFuture.complete(Result.fail(status));
            }
        });
        return finishFuture;
    }

    @Override
    public void close() {
        stream.close();
    }

    public CompletableFuture<Result<Long>> sendSessionStart(long id, String node, Duration timeout, ByteString key) {
        stream.sendNext(
                SessionRequest.newBuilder().setSessionStart(
                        SessionRequest.SessionStart.newBuilder()
                                .setSessionId(id)
                                .setPath(node)
                                .setTimeoutMillis(timeout.toMillis())
                                .setProtectionKey(key)
                                .build()
                ).build()
        );
        return initFuture;
    }

    @Override
    public void onNext(SessionResponse resp) {
        if (resp.hasFailure()) {
            onFail(resp.getFailure());
            return;
        }

        if (resp.hasSessionStarted()) {
            onSessionStarted(resp.getSessionStarted());
            return;
        }
    }

    private void onFail(SessionResponse.Failure msg) {
        Status status = Status
                .of(StatusCode.fromProto(msg.getStatus()))
                .withIssues(Issue.fromPb(msg.getIssuesList()));
        finishFuture.complete(status);
        initFuture.complete(Result.fail(status));
    }

    private void onSessionStarted(SessionResponse.SessionStarted msg) {
        long id = msg.getSessionId();
        if (!initFuture.complete(Result.success(id))) {
            logger.warn("lost session started message with id {}", id);
        }
    }
}
