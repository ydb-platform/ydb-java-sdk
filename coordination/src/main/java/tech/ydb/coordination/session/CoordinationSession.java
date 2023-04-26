package tech.ydb.coordination.session;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.YdbIssueMessage;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.coordination.observer.CoordinationSessionObserver;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationSession {

    private static final Logger logger = LoggerFactory.getLogger(CoordinationSession.class);

    private final GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final CompletableFuture<SessionResponse> stoppedFuture = new CompletableFuture<>();
    private final AtomicLong sessionId = new AtomicLong();

    public CoordinationSession(GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream) {
        this.coordinationStream = coordinationStream;
    }

    public long getSessionId() {
        return sessionId.get();
    }

    public CompletableFuture<Status> start(CoordinationSessionObserver coordinationSessionObserver) {
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

                                coordinationSessionObserver.onSessionStarted();
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
                                coordinationSessionObserver.onAcquireSemaphoreResult(
                                        message.getAcquireSemaphoreResult().getAcquired(),
                                        getStatus(
                                                message.getAcquireSemaphoreResult().getStatus(),
                                                message.getAcquireSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case ACQUIRE_SEMAPHORE_PENDING:
                                coordinationSessionObserver.onAcquireSemaphorePending();
                                break;
                            case FAILURE:
                                coordinationSessionObserver.onFailure(
                                        getStatus(
                                                message.getFailure().getStatus(),
                                                message.getFailure().getIssuesList()
                                        )
                                );
                                break;
                            case DESCRIBE_SEMAPHORE_RESULT:
                                coordinationSessionObserver.onDescribeSemaphoreResult(
                                        message.getDescribeSemaphoreResult().getSemaphoreDescription(),
                                        getStatus(
                                                message.getDescribeSemaphoreResult().getStatus(),
                                                message.getDescribeSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case DESCRIBE_SEMAPHORE_CHANGED:
                                coordinationSessionObserver.onDescribeSemaphoreChanged(
                                        message.getDescribeSemaphoreChanged().getDataChanged(),
                                        message.getDescribeSemaphoreChanged().getOwnersChanged()
                                );
                                break;
                            case DELETE_SEMAPHORE_RESULT:
                                coordinationSessionObserver.onDeleteSemaphoreResult(
                                        getStatus(
                                                message.getDeleteSemaphoreResult().getStatus(),
                                                message.getDeleteSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case CREATE_SEMAPHORE_RESULT:
                                coordinationSessionObserver.onCreateSemaphoreResult(
                                        getStatus(
                                                message.getCreateSemaphoreResult().getStatus(),
                                                message.getCreateSemaphoreResult().getIssuesList()
                                        )
                                );
                                break;
                            case RELEASE_SEMAPHORE_RESULT:
                                coordinationSessionObserver.onReleaseSemaphoreResult(
                                        message.getReleaseSemaphoreResult().getReleased(),
                                        getStatus(
                                                message.getReleaseSemaphoreResult().getStatus(),
                                                message.getReleaseSemaphoreResult().getIssuesList()
                                        )
                                );
                            case UPDATE_SEMAPHORE_RESULT:
                                coordinationSessionObserver.onUpdateSemaphoreResult(
                                        message.getUpdateSemaphoreResult().getReqId(),
                                        getStatus(
                                                message.getUpdateSemaphoreResult().getStatus(),
                                                message.getUpdateSemaphoreResult().getIssuesList()
                                        )
                                );
                            case PONG:
                                coordinationSessionObserver.onPong(
                                        message.getPong().getOpaque()
                                );
                            default:
                        }
                    }
                }
        );
    }

    /**
     * First message used to start/restore a session
     *
     * @param sessionStart session start of proto body
     */
    public void sendStartSession(SessionRequest.SessionStart sessionStart) {
        send(
                SessionRequest.newBuilder()
                        .setSessionStart(sessionStart)
                        .build()
        );
    }

    /**
     * Used for checking liveness of the connection
     *
     * @param pingPong ping pong of proto body
     */
    public void sendPingPong(SessionRequest.PingPong pingPong) {
        send(
                SessionRequest.newBuilder()
                        .setPing(pingPong)
                        .build()
        );
    }

    /**
     * Used to acquire a semaphore
     * <p>
     * WARNING: a single session cannot acquire the same semaphore multiple times
     * <p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data.
     *
     * @param acquireSemaphore acquire semaphore of proto body
     */
    public void sendAcquireSemaphore(SessionRequest.AcquireSemaphore acquireSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setAcquireSemaphore(acquireSemaphore)
                        .build()
        );
    }

    /**
     * Used to release a semaphore
     * <p>
     * WARNING: a single session cannot release the same semaphore multiple times
     * <p>
     * The release operation will either remove current session from waiters
     * queue or release an already owned semaphore.
     *
     * @param releaseSemaphore release semaphore of proto body
     */
    public void sendReleaseSemaphore(SessionRequest.ReleaseSemaphore releaseSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setReleaseSemaphore(releaseSemaphore)
                        .build()
        );
    }

    /**
     * Used to describe semaphores and watch them for changes
     * <p>
     * WARNING: a describe operation will cancel previous watches on the same semaphore
     *
     * @param describeSemaphore describe semaphore of proto body
     */
    public void sendDescribeSemaphore(SessionRequest.DescribeSemaphore describeSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setDescribeSemaphore(describeSemaphore)
                        .build()
        );
    }

    /**
     * Used to create a new semaphore
     *
     * @param createSemaphore create semaphore of proto body
     */
    public void sendCreateSemaphore(SessionRequest.CreateSemaphore createSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setCreateSemaphore(createSemaphore)
                        .build()
        );
    }

    /**
     * Used to change semaphore data
     *
     * @param updateSemaphore update semaphore of proto body
     */
    public void sendUpdateSemaphore(SessionRequest.UpdateSemaphore updateSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setUpdateSemaphore(updateSemaphore)
                        .build()
        );
    }

    /**
     * Used to delete an existing semaphore
     *
     * @param deleteSemaphore delete semaphore of proto body
     */
    public void sendDeleteSemaphore(SessionRequest.DeleteSemaphore deleteSemaphore) {
        send(
                SessionRequest.newBuilder()
                        .setDeleteSemaphore(deleteSemaphore)
                        .build()
        );
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

    @PreDestroy
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
}
