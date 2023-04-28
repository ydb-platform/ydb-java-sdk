package tech.ydb.coordination.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.YdbIssueMessage;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationSessionImpl implements CoordinationSession {

    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionImpl.class);

    private final GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final CompletableFuture<SessionResponse> stoppedFuture = new CompletableFuture<>();
    private final AtomicLong sessionId = new AtomicLong();

    public CoordinationSessionImpl(GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream) {
        this.coordinationStream = coordinationStream;
    }

    @Override
    public  long getSessionId() {
        return sessionId.get();
    }

    @Override
    public CompletableFuture<Status> start(CoordinationSession.Observer observer) {
        return coordinationStream.start(
                message -> {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Message received:\n{}", message);
                    }

                    if (message.hasSessionStopped()) {
                        stoppedFuture.complete(message);

                        return;
                    }

                    if (isWorking.get()) {
                        switch (message.getResponseCase()) {
                            case SESSION_STARTED:
                                sessionId.set(message.getSessionStarted().getSessionId());

                                observer.onSessionStarted();
                                break;
                            case PING:
                                coordinationStream.sendNext(
                                        SessionRequest.newBuilder().setPong(
                                                SessionRequest.PingPong.newBuilder()
                                                        .setOpaque(message.getPing().getOpaque())
                                                        .build()
                                        ).build()
                                );
                                break;
                            case ACQUIRE_SEMAPHORE_RESULT:
                                observer.onAcquireSemaphoreResult(
                                        message.getAcquireSemaphoreResult().getAcquired(),
                                        getStatus(
                                                message.getAcquireSemaphoreResult().getStatus(),
                                                message.getAcquireSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case ACQUIRE_SEMAPHORE_PENDING:
                                observer.onAcquireSemaphorePending();
                                break;
                            case FAILURE:
                                observer.onFailure(
                                        getStatus(
                                                message.getFailure().getStatus(),
                                                message.getFailure().getIssuesList()
                                        )
                                );
                                break;
                            case DESCRIBE_SEMAPHORE_RESULT:
                                observer.onDescribeSemaphoreResult(
                                        message.getDescribeSemaphoreResult().getSemaphoreDescription(),
                                        getStatus(
                                                message.getDescribeSemaphoreResult().getStatus(),
                                                message.getDescribeSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case DESCRIBE_SEMAPHORE_CHANGED:
                                observer.onDescribeSemaphoreChanged(
                                        message.getDescribeSemaphoreChanged().getDataChanged(),
                                        message.getDescribeSemaphoreChanged().getOwnersChanged()
                                );
                                break;
                            case DELETE_SEMAPHORE_RESULT:
                                observer.onDeleteSemaphoreResult(
                                        getStatus(
                                                message.getDeleteSemaphoreResult().getStatus(),
                                                message.getDeleteSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case CREATE_SEMAPHORE_RESULT:
                                observer.onCreateSemaphoreResult(
                                        getStatus(
                                                message.getCreateSemaphoreResult().getStatus(),
                                                message.getCreateSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case RELEASE_SEMAPHORE_RESULT:
                                observer.onReleaseSemaphoreResult(
                                        message.getReleaseSemaphoreResult().getReleased(),
                                        getStatus(
                                                message.getReleaseSemaphoreResult().getStatus(),
                                                message.getReleaseSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case UPDATE_SEMAPHORE_RESULT:
                                observer.onUpdateSemaphoreResult(
                                        message.getUpdateSemaphoreResult().getReqId(),
                                        getStatus(
                                                message.getUpdateSemaphoreResult().getStatus(),
                                                message.getUpdateSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case PONG:
                                observer.onPong(
                                        message.getPong().getOpaque()
                                );
                                break;
                            default:
                        }
                    }
                }
        );
    }

    @Override
    public void sendStartSession(SessionRequest.SessionStart sessionStart) {
        send(
                SessionRequest.newBuilder()
                        .setSessionStart(sessionStart)
                        .build()
        );
    }

    @Override
    public void sendPingPong(SessionRequest.PingPong pingPong) {
        send(
                SessionRequest.newBuilder()
                        .setPing(pingPong)
                        .build()
        );
    }

    @Override
    public void sendAcquireSemaphore(SessionRequest.AcquireSemaphore acquireSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setAcquireSemaphore(acquireSemaphore)
                        .build()
        );
    }

    @Override
    public void sendReleaseSemaphore(SessionRequest.ReleaseSemaphore releaseSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setReleaseSemaphore(releaseSemaphore)
                        .build()
        );
    }

    @Override
    public void sendDescribeSemaphore(SessionRequest.DescribeSemaphore describeSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setDescribeSemaphore(describeSemaphore)
                        .build()
        );
    }

    @Override
    public void sendCreateSemaphore(SessionRequest.CreateSemaphore createSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setCreateSemaphore(createSemaphore)
                        .build()
        );
    }

    @Override
    public void sendUpdateSemaphore(SessionRequest.UpdateSemaphore updateSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setUpdateSemaphore(updateSemaphore)
                        .build()
        );
    }

    @Override
    public void sendDeleteSemaphore(SessionRequest.DeleteSemaphore deleteSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setDeleteSemaphore(deleteSemaphore)
                        .build()
        );
    }

    @Override
    public void stop() {
        if (isWorking.compareAndSet(true, false)) {
            coordinationStream.sendNext(
                    SessionRequest.newBuilder()
                            .setSessionStop(
                                    SessionRequest.SessionStop.newBuilder()
                                            .build()
                            ).build()
            );

            try {
                stoppedFuture.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("Failed stopping awaiting", e);
            }

            coordinationStream.close();
        }
    }

    private void send(SessionRequest sessionRequest) {
        if (logger.isTraceEnabled()) {
            logger.trace("Send message: {}", sessionRequest);
        }

        coordinationStream.sendNext(sessionRequest);
    }

    private static Status getStatus(
            StatusCodesProtos.StatusIds.StatusCode statusCode,
            List<YdbIssueMessage.IssueMessage> issueMessages
    ) {
        return Status.of(StatusCode.fromProto(statusCode))
                .withIssues(Issue.fromPb(issueMessages));
    }
}
