package tech.ydb.coordination.instruments.semaphore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSession.Observer;
import tech.ydb.coordination.instruments.semaphore.exceptions.SemaphoreCreationException;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.coordination.SemaphoreDescription;
import tech.ydb.proto.coordination.SessionRequest.AcquireSemaphore;
import tech.ydb.proto.coordination.SessionRequest.CreateSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DescribeSemaphore;
import tech.ydb.proto.coordination.SessionRequest.ReleaseSemaphore;
import tech.ydb.proto.coordination.SessionRequest.SessionStart;




public class SemaphoreImpl implements Semaphore {
    private static final Logger logger = LoggerFactory.getLogger(SemaphoreImpl.class);
    private final String nodePath;
    private final CoordinationSession session;
    private final long limit;
    private final String semaphoreName;
    private final CompletableFuture<SemaphoreImpl> createFuture = new CompletableFuture<>();
    private CompletableFuture<Boolean> acquiredFuture = new CompletableFuture<>();
    private CompletableFuture<Boolean> releaseFuture = new CompletableFuture<>();

    protected SemaphoreImpl(CoordinationClient client, String nodePath, String semaphoreName, long limit) {
        this.session = client.createSession();
        this.nodePath = nodePath;
        this.semaphoreName = semaphoreName;
        this.limit = limit;

        client.createNode(
                nodePath,
                CoordinationNodeSettings.newBuilder()
                        .build()
        ).thenRun(this::startSession);
    }

    public static CompletableFuture<? extends Semaphore> newSemaphore(
            CoordinationClient client, String path, String semaphoreName, long limit) {
        final SemaphoreImpl semaphore = new SemaphoreImpl(client, path, semaphoreName, limit);
        return semaphore.createFuture;
    }

    @Override
    public boolean acquire(SemaphoreSettings settings) {
        try {
            return acquireAsync(settings).get(settings.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> acquireAsync(SemaphoreSettings settings) {
        acquiredFuture = new CompletableFuture<>();
        session.sendAcquireSemaphore(
                AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(settings.getCount())
                        .setEphemeral(false)
                        .setTimeoutMillis(settings.getTimeout())
                        .build()
        );
        return acquiredFuture;
    }

    @Override
    public boolean release() {
        return releaseAsync().join();
    }

    @Override
    public CompletableFuture<Boolean> releaseAsync() {
        releaseFuture = new CompletableFuture<>();
        session.sendReleaseSemaphore(
                ReleaseSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setReqId(ThreadLocalRandom.current().nextInt())
                        .build()
        );
        return releaseFuture.thenCompose(isReleased -> {
            if (isReleased) {
                acquiredFuture = CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(isReleased);
        });
    }

    @Override
    public boolean isAcquired() {
        return acquiredFuture.getNow(false);
    }

    private void startSession() {
        session.start(new Observer() {
            @Override
            public void onAcquireSemaphoreResult(boolean acquired, Status status) {
                logger.trace("onAcquireSemaphoreResult: " + status.toString() + ", acquired = " + acquired);
                acquiredFuture.complete(acquired);
            }

            @Override
            public void onAcquireSemaphorePending() {
                logger.trace("onAcquireSemaphorePending");
            }

            @Override
            public void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription, Status status) {
                if (status.isSuccess()) {
                    if (semaphoreDescription.getName().equals(semaphoreName) &&
                            semaphoreDescription.getLimit() == limit) {
                        createFuture.complete(SemaphoreImpl.this);
                    } else {
                        createFuture.completeExceptionally(
                                new SemaphoreCreationException(
                                        "The semaphore has already been created and its settings are different from " +
                                                "yours.",
                                        semaphoreDescription));
                    }
                } else {
                    createFuture.completeExceptionally(
                            new SemaphoreCreationException(
                                    "The semaphore has already been created and failed to compare settings (" + status +
                                            ")."));
                }
            }

            @Override
            public void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
                logger.info("onDescribeSemaphoreChanged: {dataChanged=" + dataChanged + ", ownersChanged=" +
                        ownersChanged + "}");
            }

            @Override
            public void onDeleteSemaphoreResult(Status status) {
                logger.trace("onDeleteSemaphoreResult: " + status);
            }

            @Override
            public void onCreateSemaphoreResult(Status status) {
                logger.trace("onCreateSemaphoreResult: " + status);
                if (status.getCode() == StatusCode.ALREADY_EXISTS) {
                    session.sendDescribeSemaphore(
                            DescribeSemaphore.newBuilder()
                                    .setReqId(ThreadLocalRandom.current().nextLong())
                                    .setName(semaphoreName)
                                    .setIncludeOwners(false)
                                    .setIncludeWaiters(false)
                                    .setWatchData(false)
                                    .setWatchOwners(false)
                                    .build()
                    );
                } else {
                    if (status.isSuccess()) {
                        createFuture.complete(SemaphoreImpl.this);
                    } else {
                        createFuture.completeExceptionally(
                                new SemaphoreCreationException("Failed to create semaphore (" + status + ")"));
                    }
                }
            }

            @Override
            public void onFailure(Status status) {
                logger.trace("onFailure: " + status);
                RuntimeException exception =
                        new RuntimeException("Connection or session failure " + status);
                if (createFuture.isDone()) {
                    throw exception;
                } else {
                    createFuture.completeExceptionally(exception);
                }
            }

            @Override
            public void onReleaseSemaphoreResult(boolean released, Status status) {
                logger.trace("onReleaseSemaphoreResult: " + status);
                releaseFuture.complete(released);
            }

            @Override
            public void onUpdateSemaphoreResult(long reqId, Status status) {
                logger.trace("onUpdateSemaphoreResult: " + status);
            }

            @Override
            public void onSessionStarted() {
                logger.trace("onSessionStarted");

                session.sendCreateSemaphore(
                        CreateSemaphore.newBuilder()
                                .setName(semaphoreName)
                                .setReqId(ThreadLocalRandom.current().nextInt())
                                .setLimit(limit)
                                .build()
                );
            }

            @Override
            public void onPong(long pingValue) {
                Observer.super.onPong(pingValue);
            }
        });

        final byte[] protectionKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(protectionKey);

        session.sendStartSession(
                SessionStart.newBuilder()
                        .setSessionId(session.getSessionId())
                        .setPath(nodePath)
                        .setProtectionKey(ByteString.copyFrom(protectionKey))
                        .build());
    }
}
