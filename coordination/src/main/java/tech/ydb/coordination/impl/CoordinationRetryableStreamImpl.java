package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
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

public class CoordinationRetryableStreamImpl implements CoordinationStream {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationRetryableStreamImpl.class);
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];
    private final CoordinationRpc coordinationRpc;
    private final Map<Long, SessionRequest> requestMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicBoolean isRetryState = new AtomicBoolean(false);
    private final AtomicLong sessionId = new AtomicLong();
    private final AtomicInteger innerRequestId = new AtomicInteger(1);
    private final String nodePath;
    private final AtomicInteger attemptsToRetry;
    private final Duration timeoutInRetryAttempt;
    private final Duration timeoutBetweenAttempts;
    private final ScheduledExecutorService executorService;
    private final Map<Long, CompletableFuture<Status>> createSemaphoreFutures =
            new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Status>> createEphemeralSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Result<Boolean>>> acquireSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Result<Boolean>>> releaseSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Result<SemaphoreDescription>>> describeSemaphoreFutures =
            new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Status>> deleteSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<DescribeSemaphoreChanged>> updateWatchers = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Status>> updateSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<String, Integer> semaphoreId = new ConcurrentHashMap<>();
    private final AtomicInteger nextSemaphoreId = new AtomicInteger(1);
    private GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream;
    private CompletableFuture<Status> stoppedFuture;
    private byte[] protectionKey;

    protected CoordinationRetryableStreamImpl(CoordinationRpc coordinationRpc, ScheduledExecutorService executorService,
                                              String nodePath,
                                              Duration timeoutInRetryAttempt,
                                              int attemptsToRetry,
                                              Duration timeoutBetweenAttempts) {
        this.coordinationRpc = coordinationRpc;
        this.coordinationStream = coordinationRpc.session();
        this.executorService = executorService;
        this.nodePath = nodePath;
        this.timeoutInRetryAttempt = timeoutInRetryAttempt;
        this.attemptsToRetry = new AtomicInteger(attemptsToRetry);
        this.timeoutBetweenAttempts = timeoutBetweenAttempts;
    }

    protected CoordinationRetryableStreamImpl(CoordinationRpc coordinationRpc, ScheduledExecutorService executorService,
                                              String nodePath) {
        this(coordinationRpc,
                executorService,
                nodePath,
                Duration.ofSeconds(3),
                10,
                Duration.ofMillis(1000));
    }

    private static Status getStatus(
            StatusCodesProtos.StatusIds.StatusCode statusCode,
            List<IssueMessage> issueMessages
    ) {
        return Status.of(StatusCode.fromProto(statusCode))
                .withIssues(Issue.fromPb(issueMessages));
    }

    protected CompletableFuture<Long> start(Duration timeout) {
        final CompletableFuture<Long> sessionStartFuture = new CompletableFuture<>();
        this.stoppedFuture = coordinationStream.start(message -> {
            Status status;
            long requestId;
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
                        requestId = message.getAcquireSemaphoreResult().getReqId();
                        status = getStatus(
                                message.getAcquireSemaphoreResult().getStatus(),
                                message.getAcquireSemaphoreResult().getIssuesList()
                        );
                        // TODO: maybe ephemeral is no need
                        requestMap.remove(requestId);
                        final CompletableFuture<Status> acquireEphemeralSemaphore;
                        if ((acquireEphemeralSemaphore = createEphemeralSemaphoreFutures.remove(requestId)) != null) {
                            acquireEphemeralSemaphore.complete(status);
                        } else {
                            acquireSemaphoreFutures.remove(requestId).complete(status.isSuccess() ?
                                    Result.success(message.getAcquireSemaphoreResult().getAcquired()) :
                                    Result.fail(status));
                        }
                        break;
                    case ACQUIRE_SEMAPHORE_PENDING:
                        break;
                    case FAILURE:
                        if (!isRetryState.get()) {
                            retry();
                        }
                        break;
                    case DESCRIBE_SEMAPHORE_RESULT:
                        requestId = message.getDescribeSemaphoreResult().getReqId();
                        status = getStatus(
                                message.getDescribeSemaphoreResult().getStatus(),
                                message.getDescribeSemaphoreResult().getIssuesList()
                        );
                        final CompletableFuture<Result<SemaphoreDescription>> describeFuture =
                                describeSemaphoreFutures.remove(requestId);
                        requestMap.remove(message.getDescribeSemaphoreResult().getReqId());
                        if (status.isSuccess()) {
                            describeFuture.complete(Result.success(new SemaphoreDescription(
                                    message.getDescribeSemaphoreResult().getSemaphoreDescription())));
                        } else {
                            describeFuture.complete(Result.fail(status));
                        }
                        break;
                    case DESCRIBE_SEMAPHORE_CHANGED:
                        requestId = message.getDescribeSemaphoreChanged().getReqId();
                        final Consumer<DescribeSemaphoreChanged> watcher =
                                updateWatchers.remove(getUserRequestId(requestId));
                        if (watcher != null) {
                            watcher.accept(new DescribeSemaphoreChanged(
                                    message.getDescribeSemaphoreChanged().getDataChanged(),
                                    message.getDescribeSemaphoreChanged().getOwnersChanged(),
                                    false));
                        }
                        break;
                    case DELETE_SEMAPHORE_RESULT:
                        requestId = message.getDeleteSemaphoreResult().getReqId();
                        status = getStatus(
                                message.getDeleteSemaphoreResult().getStatus(),
                                message.getDeleteSemaphoreResult().getIssuesList()
                        );
                        requestMap.remove(requestId);
                        deleteSemaphoreFutures.remove(requestId).complete(status);
                        break;
                    case CREATE_SEMAPHORE_RESULT:
                        requestId = message.getCreateSemaphoreResult().getReqId();
                        status = getStatus(
                                message.getCreateSemaphoreResult().getStatus(),
                                message.getCreateSemaphoreResult().getIssuesList()
                        );
                        requestMap.remove(message.getCreateSemaphoreResult().getReqId());
                        createSemaphoreFutures.remove(requestId).complete(status);
                        break;
                    case RELEASE_SEMAPHORE_RESULT:
                        requestId = message.getReleaseSemaphoreResult().getReqId();
                        status = getStatus(
                                message.getReleaseSemaphoreResult().getStatus(),
                                message.getReleaseSemaphoreResult().getIssuesList()
                        );
                        requestMap.remove(message.getReleaseSemaphoreResult().getReqId());
                        releaseSemaphoreFutures.remove(requestId).complete(status.isSuccess() ?
                                Result.success(message.getReleaseSemaphoreResult().getReleased()) :
                                Result.fail(status));
                        break;
                    case UPDATE_SEMAPHORE_RESULT:
                        requestId = message.getUpdateSemaphoreResult().getReqId();
                        status = getStatus(
                                message.getUpdateSemaphoreResult().getStatus(),
                                message.getUpdateSemaphoreResult().getIssuesList()
                        );
                        requestMap.remove(message.getUpdateSemaphoreResult().getReqId());
                        updateSemaphoreFutures.remove(requestId).complete(status);
                        break;
                    default:
                }
            }
        });

        stoppedFuture.thenRun(() -> {
            if (!isRetryState.get() && isWorking.get()) {
                isRetryState.set(true);
                retry();
            }
        });

        if (protectionKey == null) {
            protectionKey = new byte[16];
            ThreadLocalRandom.current().nextBytes(protectionKey);
        }
        coordinationStream.sendNext(
                SessionRequest.newBuilder().setSessionStart(
                        SessionStart.newBuilder()
                                .setSessionId(sessionId.get())
                                .setPath(nodePath)
                                .setTimeoutMillis(timeout.toMillis())
                                .setProtectionKey(ByteString.copyFrom(protectionKey))
                                .build()
                ).build()
        );
        return sessionStartFuture;
    }

    private void retry() {
        isRetryState.set(true);
        final int attemptsBefore = attemptsToRetry.get();
        executorService.schedule(this::retryInner, timeoutBetweenAttempts.toMillis(), TimeUnit.MILLISECONDS);
        attemptsToRetry.set(attemptsBefore);
    }

    private void retryInner() {
        if (isWorking.get()) {
            attemptsToRetry.decrementAndGet();
            logger.debug("Attempt to retry stream: left {} attempts.", attemptsToRetry.get());
            try {
                this.coordinationStream.close();
                this.coordinationStream = coordinationRpc.session();
                start(timeoutInRetryAttempt).get(timeoutInRetryAttempt.toMillis(), TimeUnit.MILLISECONDS);
                resendRequests();
                isRetryState.set(false);
                if (stoppedFuture.isDone() && isWorking.get()) {
                    executorService.schedule(this::retryInner,
                            timeoutBetweenAttempts.toMillis(), TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                logger.trace("Exception while retry stream: exception = {}", e.toString());
                if (isWorking.get()) {
                    executorService.schedule(this::retryInner,
                            timeoutBetweenAttempts.toMillis(), TimeUnit.MILLISECONDS);
                }
            } finally {
                if (attemptsToRetry.get() < 0) {
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
        updateWatchers.forEach((id, watcher) ->
            watcher.accept(new DescribeSemaphoreChanged(false, false, true))
        );
        updateWatchers.clear();
    }

    public CompletableFuture<Result<Boolean>> sendAcquireSemaphore(String semaphoreName, long count, Duration timeout,
                                                                   boolean ephemeral, int requestId) {
        return sendAcquireSemaphore(semaphoreName, count, timeout, ephemeral, BYTE_ARRAY_STUB, requestId);
    }

    public CompletableFuture<Result<Boolean>> sendAcquireSemaphore(String semaphoreName, long count, Duration timeout,
                                                                   boolean ephemeral, byte[] data, int requestId) {
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
        final CompletableFuture<Result<Boolean>> acquireFuture = new CompletableFuture<>();
        acquireSemaphoreFutures.put(fullRequestId, acquireFuture);
        send(request);
        return acquireFuture;
    }

    public CompletableFuture<Result<Boolean>> sendReleaseSemaphore(String semaphoreName, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setReleaseSemaphore(
                ReleaseSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        final CompletableFuture<Result<Boolean>> releaseFuture = new CompletableFuture<>();
        releaseSemaphoreFutures.put(fullRequestId, releaseFuture);
        send(request);
        return releaseFuture;
    }

    public CompletableFuture<Result<SemaphoreDescription>> sendDescribeSemaphore(
            String semaphoreName, boolean includeOwners, boolean includeWaiters) {
        final long fullRequestId = getFullRequestId(innerRequestId.get());
        final SessionRequest request = SessionRequest.newBuilder().setDescribeSemaphore(
                DescribeSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setIncludeOwners(includeOwners)
                        .setIncludeWaiters(includeWaiters)
                        .setWatchData(false)
                        .setWatchOwners(false)
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        final CompletableFuture<Result<SemaphoreDescription>> describeFuture = new CompletableFuture<>();
        describeSemaphoreFutures.put(fullRequestId, describeFuture);
        send(request);
        return describeFuture;
    }

    public CompletableFuture<Result<SemaphoreDescription>> sendDescribeSemaphore(
            String semaphoreName, boolean includeOwners, boolean includeWaiters,
            boolean watchData, boolean watchOwners, Consumer<DescribeSemaphoreChanged> updateWatcher) {
        final long fullRequestId = getFullRequestId(semaphoreId.compute(semaphoreName,
                (key, value) -> value == null ? nextSemaphoreId.incrementAndGet() : value));
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
        final CompletableFuture<Result<SemaphoreDescription>> describeFuture = new CompletableFuture<>();
        describeSemaphoreFutures.put(fullRequestId, describeFuture);
        updateWatchers.put(getUserRequestId(fullRequestId), updateWatcher);
        send(request);
        return describeFuture;
    }

    public CompletableFuture<Status> sendCreateSemaphore(String semaphoreName, long limit, int requestId) {
        return sendCreateSemaphore(semaphoreName, limit, BYTE_ARRAY_STUB, requestId);
    }

    public CompletableFuture<Status> sendCreateSemaphore(String semaphoreName, long limit, @Nonnull byte[] data,
                                                         int requestId) {
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
        final CompletableFuture<Status> createSemaphoreFuture = new CompletableFuture<>();
        createSemaphoreFutures.put(fullRequestId, createSemaphoreFuture);
        send(request);
        return createSemaphoreFuture;
    }

    public CompletableFuture<Status> sendUpdateSemaphore(String semaphoreName, int requestId) {
        return sendUpdateSemaphore(semaphoreName, BYTE_ARRAY_STUB, requestId);
    }

    public CompletableFuture<Status> sendUpdateSemaphore(String semaphoreName, byte[] data, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setUpdateSemaphore(
                UpdateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setData(ByteString.copyFrom(data))
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        final CompletableFuture<Status> updateFuture = new CompletableFuture<>();
        updateSemaphoreFutures.put(fullRequestId, updateFuture);
        send(request);
        return updateFuture;
    }

    public CompletableFuture<Status> sendDeleteSemaphore(String semaphoreName, boolean force, int requestId) {
        final long fullRequestId = getFullRequestId(requestId);
        final SessionRequest request = SessionRequest.newBuilder().setDeleteSemaphore(
                DeleteSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setForce(force)
                        .setReqId(fullRequestId)
                        .build()
        ).build();
        requestMap.put(fullRequestId, request);
        final CompletableFuture<Status> deleteFuture = new CompletableFuture<>();
        deleteSemaphoreFutures.put(fullRequestId, deleteFuture);
        send(request);
        return deleteFuture;
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

    public void stop() {
        isWorking.set(false);
        coordinationStream.close();
    }
}
