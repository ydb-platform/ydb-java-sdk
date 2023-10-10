package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.YdbIssueMessage.IssueMessage;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionRequest.AcquireSemaphore;
import tech.ydb.proto.coordination.SessionRequest.CreateSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DeleteSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DescribeSemaphore;
import tech.ydb.proto.coordination.SessionRequest.ReleaseSemaphore;
import tech.ydb.proto.coordination.SessionRequest.SessionStart;
import tech.ydb.proto.coordination.SessionRequest.UpdateSemaphore;
import tech.ydb.proto.coordination.SessionResponse;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

// TODO: interface
public class CoordinationRetryableStreamImpl {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationRetryableStreamImpl.class);
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];
    private final CoordinationRpc coordinationRpc;
    private final Map<Long, SessionRequest> requestMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicBoolean isRetryState = new AtomicBoolean(false);
    private final AtomicLong sessionId = new AtomicLong();
    private final AtomicInteger innerRequestId = new AtomicInteger(1);
    private final String nodePath;
    private final AtomicInteger leftRetryAttempts = new AtomicInteger(10); // TODO: request from user
    private final Duration oneRetryAttemptTimeout = Duration.ofSeconds(3); // TODO: request from user
    private GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream;
    private CompletableFuture<Status> stoppedFuture;
    private byte[] protectionKey;
    private volatile CoordinationSession.Observer observer;
    private final ScheduledExecutorService executorService;

    protected CoordinationRetryableStreamImpl(CoordinationRpc coordinationRpc, ScheduledExecutorService executorService,
                                              String nodePath) {
        this.coordinationRpc = coordinationRpc;
        this.coordinationStream = coordinationRpc.session();
        this.executorService = executorService;
        this.nodePath = nodePath;
    }

    private static Status getStatus(
            StatusCodesProtos.StatusIds.StatusCode statusCode,
            List<IssueMessage> issueMessages
    ) {
        return Status.of(StatusCode.fromProto(statusCode))
                .withIssues(Issue.fromPb(issueMessages));
    }

    public long getSessionId() {
        return sessionId.get();
    }

    protected CompletableFuture<Long> start(Duration timeout, CoordinationSession.Observer observer) {
        this.observer = observer;
        final CompletableFuture<Long> sessionStartFuture = new CompletableFuture<>();
        this.stoppedFuture = coordinationStream.start(message -> {
            if (logger.isTraceEnabled()) {
                logger.trace("Message received:\n{}", message);
            }

            if (message.hasSessionStopped()) {
                if (!stoppedFuture.isDone()) {
                    stoppedFuture.complete(Status.SUCCESS);
                    coordinationStream.close();
                }
                return;
            }

            if (isWorking.get()) {
                switch (message.getResponseCase()) {
                    case SESSION_STARTED:
                        sessionId.set(message.getSessionStarted().getSessionId());
                        sessionStartFuture.complete(message.getSessionStarted().getSessionId());
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
                        requestMap.remove(message.getAcquireSemaphoreResult().getReqId());
                        observer.onAcquireSemaphoreResult(
                                message.getAcquireSemaphoreResult().getAcquired(),
                                getStatus(
                                        message.getAcquireSemaphoreResult().getStatus(),
                                        message.getAcquireSemaphoreResult().getIssuesList()
                                ),
                                getUserRequestId(message.getAcquireSemaphoreResult().getReqId())
                        );
                        break;
                    case ACQUIRE_SEMAPHORE_PENDING:
                        break;
                    case FAILURE:
                        if (!isRetryState.get()) {
                            retry();
                        }
                        break;
                    case DESCRIBE_SEMAPHORE_RESULT:
                        requestMap.remove(message.getDescribeSemaphoreResult().getReqId());
                        observer.onDescribeSemaphoreResult(
                                new SemaphoreDescription(
                                        message.getDescribeSemaphoreResult().getSemaphoreDescription()),
                                getStatus(
                                        message.getDescribeSemaphoreResult().getStatus(),
                                        message.getDescribeSemaphoreResult().getIssuesList()
                                ),
                                getUserRequestId(message.getDescribeSemaphoreResult().getReqId())
                        );
                        break;
                    case DESCRIBE_SEMAPHORE_CHANGED:
                        observer.onDescribeSemaphoreChanged(
                                message.getDescribeSemaphoreChanged().getDataChanged(),
                                message.getDescribeSemaphoreChanged().getOwnersChanged(),
                                getUserRequestId(message.getDescribeSemaphoreChanged().getReqId())
                        );
                        break;
                    case DELETE_SEMAPHORE_RESULT:
                        requestMap.remove(message.getDeleteSemaphoreResult().getReqId());
                        observer.onDeleteSemaphoreResult(
                                getStatus(
                                        message.getDeleteSemaphoreResult().getStatus(),
                                        message.getDeleteSemaphoreResult().getIssuesList()
                                ),
                                getUserRequestId(message.getDeleteSemaphoreResult().getReqId())
                        );
                        break;
                    case CREATE_SEMAPHORE_RESULT:
                        requestMap.remove(message.getCreateSemaphoreResult().getReqId());
                        observer.onCreateSemaphoreResult(
                                getStatus(
                                        message.getCreateSemaphoreResult().getStatus(),
                                        message.getCreateSemaphoreResult().getIssuesList()
                                ),
                                getUserRequestId(message.getCreateSemaphoreResult().getReqId())
                        );
                        break;
                    case RELEASE_SEMAPHORE_RESULT:
                        requestMap.remove(message.getReleaseSemaphoreResult().getReqId());
                        observer.onReleaseSemaphoreResult(
                                message.getReleaseSemaphoreResult().getReleased(),
                                getStatus(
                                        message.getReleaseSemaphoreResult().getStatus(),
                                        message.getReleaseSemaphoreResult().getIssuesList()
                                ),
                                getUserRequestId(message.getReleaseSemaphoreResult().getReqId())
                        );
                        break;
                    case UPDATE_SEMAPHORE_RESULT:
                        requestMap.remove(message.getUpdateSemaphoreResult().getReqId());
                        observer.onUpdateSemaphoreResult(
                                getStatus(
                                        message.getUpdateSemaphoreResult().getStatus(),
                                        message.getUpdateSemaphoreResult().getIssuesList()
                                ),
                                getUserRequestId(message.getUpdateSemaphoreResult().getReqId())
                        );
                        break;
                    default:
                }
            }
        });

        stoppedFuture.thenRun(() -> {
            if (!isRetryState.get()) {
                isRetryState.set(true);
                retry();
            }
        });

        if (protectionKey == null) {
            protectionKey = new byte[16];
            ThreadLocalRandom.current().nextBytes(protectionKey);
        }
        Arrays.stream(Thread.currentThread().getStackTrace()).map(e -> "stack: " + e).forEach(logger::debug);
        sendStartSession(
                SessionStart.newBuilder()
                        .setSessionId(sessionId.get())
                        .setPath(nodePath)
                        .setTimeoutMillis(timeout.toMillis())
                        .setProtectionKey(ByteString.copyFrom(protectionKey))
                        .build()
        );
        return sessionStartFuture;
    }

    protected void retry() {
        isRetryState.set(true);
        final int attemptsBefore = leftRetryAttempts.get();
        executorService.schedule(this::retryInner, 1000, TimeUnit.MILLISECONDS);
        leftRetryAttempts.set(attemptsBefore);
    }

    protected void retryInner() {
        if (isWorking.get()) {
            leftRetryAttempts.decrementAndGet();
            logger.debug("Attempt to retry stream: left {} attempts.", leftRetryAttempts.get());
            try {
                this.coordinationStream.close();
                this.coordinationStream = coordinationRpc.session();
                start(oneRetryAttemptTimeout, observer).get(oneRetryAttemptTimeout.toMillis(), TimeUnit.MILLISECONDS);
                resendRequests();
                isRetryState.set(false);
                if (stoppedFuture.isDone() && isWorking.get()) {
                    executorService.schedule(this::retryInner, 1000, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                logger.trace("Exception while retry stream: exception = {}", e.toString());
                if (isWorking.get()) {
                    executorService.schedule(this::retryInner, 1000, TimeUnit.MILLISECONDS);
                }
            } finally {
                if (leftRetryAttempts.get() < 0) {
                    stop();
                }
            }
        }
    }

    private void resendRequests() {
        final List<Long> requestIds = new ArrayList<>(requestMap.keySet());
        for (int i = 0; i < requestIds.size() && !stoppedFuture.isDone(); i++) {
            final Long requestId = requestIds.get(i);
            logger.trace("Resend request: {}", requestMap.get(requestId).toString());
            send(requestMap.get(requestId));
        }
    }

    public void sendStartSession(SessionStart sessionStart) {
        send(
                SessionRequest.newBuilder()
                        .setSessionStart(sessionStart)
                        .build()
        );
    }

    public void sendAcquireSemaphore(String semaphoreName, long count, Duration timeout, boolean ephemeral,
                                     int requestId) {
        sendAcquireSemaphore(semaphoreName, count, timeout, ephemeral, BYTE_ARRAY_STUB, requestId);
    }

    public void sendAcquireSemaphore(String semaphoreName, long count, Duration timeout, boolean ephemeral,
                                     byte[] data, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setAcquireSemaphore(
                AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(count)
                        .setTimeoutMillis(timeout.toMillis())
                        .setEphemeral(ephemeral)
                        .setData(ByteString.copyFrom(data))
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        send(request);
    }

    public void sendReleaseSemaphore(String semaphoreName, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setReleaseSemaphore(
                ReleaseSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        send(request);
    }

    public void sendDescribeSemaphore(String semaphoreName, boolean includeOwners, boolean includeWaiters,
                                      boolean watchData, boolean watchOwners, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setDescribeSemaphore(
                DescribeSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setIncludeOwners(includeOwners)
                        .setIncludeWaiters(includeWaiters)
                        .setWatchData(watchData)
                        .setWatchOwners(watchOwners)
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        send(request);
    }

    public void sendCreateSemaphore(String semaphoreName, long limit, int requestId) {
        sendCreateSemaphore(semaphoreName, limit, BYTE_ARRAY_STUB, requestId);
    }

    public void sendCreateSemaphore(String semaphoreName, long limit, @Nonnull byte[] data, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setCreateSemaphore(
                CreateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setLimit(limit)
                        .setData(ByteString.copyFrom(data))
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        send(request);
    }

    public void sendUpdateSemaphore(String semaphoreName, int requestId) {
        sendUpdateSemaphore(semaphoreName, BYTE_ARRAY_STUB, requestId);
    }

    public void sendUpdateSemaphore(String semaphoreName, byte[] data, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setUpdateSemaphore(
                UpdateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setData(ByteString.copyFrom(data))
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        send(request);
    }

    public void sendDeleteSemaphore(String semaphoreName, boolean force, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setDeleteSemaphore(
                DeleteSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setForce(force)
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        send(request);
    }

    private long getFullRequestId(int userRequestId) {
        return (((long) userRequestId) << 32) | (innerRequestId.getAndIncrement() & 0xffffffffL);
    }

    private int getUserRequestId(long fullRequestId) {
        return (int) (fullRequestId >> 32);
    }

    private void send(SessionRequest sessionRequest) {
        if (logger.isTraceEnabled()) {
            logger.trace("Send message: {}", sessionRequest);
        }

        if (isWorking.get()) {
            try {
                coordinationStream.sendNext(sessionRequest);
            } catch (IllegalStateException e) {
                logger.error("Error sending message {}", sessionRequest, e);
            }
        }
    }

    public CompletableFuture<Status> getLifetimeFuture() {
        return stoppedFuture;
    }

    public void stop() {
        isWorking.set(false);
        coordinationStream.close();
    }
}
