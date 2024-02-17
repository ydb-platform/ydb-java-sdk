package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.YdbIssueMessage;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationStream implements GrpcReadWriteStream.Observer<SessionResponse> {
    private static final int SHUTDOWN_TIMEOUT_MS = 1000;
    private static final Logger logger = LoggerFactory.getLogger(CoordinationStream.class);

    private final ScheduledExecutorService scheduler;
    private final GrpcReadWriteStream<SessionResponse, SessionRequest> stream;
    private final CompletableFuture<Status> stopFuture = new CompletableFuture<>();
    private final CompletableFuture<Result<Long>> startFuture = new CompletableFuture<>();

    public CoordinationStream(CoordinationRpc rpc, GrpcRequestSettings settings) {
        this.scheduler = rpc.getScheduler();
        this.stream = rpc.createSession(settings);
    }

    public CompletableFuture<Status> start() {
        stream.start(this).whenComplete((status, th) -> {
            if (th != null) {
                stopFuture.completeExceptionally(th);
                startFuture.completeExceptionally(th);
            }
            if (status != null) {
                stopFuture.complete(status);
                startFuture.complete(Result.fail(status));
            }
        });
        return stopFuture;
    }

    public void cancel() {
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
        return startFuture;
    }

    public CompletableFuture<Status> sendSessionStop() {
        if (stopFuture.isDone()) {
            return stopFuture;
        }

        stream.sendNext(
                SessionRequest.newBuilder().setSessionStop(
                        SessionRequest.SessionStop.newBuilder().build()
                ).build()
        );

        final ScheduledFuture<?> timer = scheduler.schedule(this::cancel, SHUTDOWN_TIMEOUT_MS, TimeUnit.MICROSECONDS);
        stopFuture.whenComplete((st, ex) -> {
            if (ex != null && timer != null && timer.isDone()) {
                timer.cancel(true);
            }
        });

        return stopFuture;
    }


    private void sendAcquireSemaphore(long reqId, String name, long count, boolean ephe, ByteString data,
            long timeout) {
        stream.sendNext(
                SessionRequest.newBuilder().setAcquireSemaphore(
                        SessionRequest.AcquireSemaphore.newBuilder()
                                .setName(name)
                                .setCount(count)
                                .setTimeoutMillis(timeout)
                                .setEphemeral(ephe)
                                .setData(data)
                                .setReqId(reqId)
                                .build()
                ).build()
        );
    }

    public void sendCreateSemaphore(long reqId, String name, long limit, ByteString data, Consumer<Status> consumer) {
        SessionRequest request = SessionRequest.newBuilder().setCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(name)
                        .setLimit(limit)
                        .setData(data)
                        .setReqId(reqId)
                        .build()
        ).build();

//        registerHanlder()
//
//        Consumer<SessionResponse> old = requests.put(reqId, resp -> {
//        });
//        if ()

        stream.sendNext(request);
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

        if (resp.hasSessionStopped()) {
            onSessionStopped(resp.getSessionStopped());
            return;
        }

        if (resp.hasAcquireSemaphorePending()) {
            // ignore, just logging
            long reqId = resp.getAcquireSemaphorePending().getReqId();
            logger.trace("the stream {} got pending acquire msg {}", hashCode(), reqId);
            return;
        }

        if (resp.hasCreateSemaphoreResult()) {
            onNextResponse(resp.getCreateSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasDeleteSemaphoreResult()) {
            onNextResponse(resp.getDeleteSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasAcquireSemaphoreResult()) {
            onNextResponse(resp.getAcquireSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasDescribeSemaphoreResult()) {
            onNextResponse(resp.getDescribeSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasDescribeSemaphoreChanged()) {
            onNextResponse(resp.getDescribeSemaphoreChanged().getReqId(), resp);
        }

        if (resp.hasReleaseSemaphoreResult()) {
            onNextResponse(resp.getReleaseSemaphoreResult().getReqId(), resp);
        }
    }

    public void onNextResponse(long id, SessionResponse resp) {

    }

    private void onFail(SessionResponse.Failure msg) {
        Status status = makeStatus(msg.getStatus(), msg.getIssuesList());
        stopFuture.complete(status);
        startFuture.complete(Result.fail(status));
    }

    private void onSessionStarted(SessionResponse.SessionStarted msg) {
        long id = msg.getSessionId();
        if (startFuture.complete(Result.success(id))) {
            logger.trace("the coordination session {} started", id);
        } else {
            logger.warn("lost the coordination session {} start message", id);
        }
    }

    private void onSessionStopped(SessionResponse.SessionStopped msg) {
        logger.trace("the coordination session {} stopped", msg.getSessionId());
    }

    private static Status makeStatus(StatusCodesProtos.StatusIds.StatusCode c, List<YdbIssueMessage.IssueMessage> il) {
        return Status.of(StatusCode.fromProto(c)).withIssues(Issue.fromPb(il));
    }

//    private class RequestHandler {
//        public boolean
//    }
}
