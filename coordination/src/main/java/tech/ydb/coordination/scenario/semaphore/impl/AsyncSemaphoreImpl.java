package tech.ydb.coordination.scenario.semaphore.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSession.Observer;
import tech.ydb.coordination.scenario.semaphore.AsyncSemaphore;
import tech.ydb.coordination.scenario.semaphore.exceptions.SemaphoreCreationException;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.coordination.SemaphoreDescription;
import tech.ydb.proto.coordination.SessionRequest.AcquireSemaphore;
import tech.ydb.proto.coordination.SessionRequest.CreateSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DeleteSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DescribeSemaphore;
import tech.ydb.proto.coordination.SessionRequest.ReleaseSemaphore;
import tech.ydb.proto.coordination.SessionRequest.SessionStart;

public class AsyncSemaphoreImpl implements AsyncSemaphore {
    private static final Logger logger = LoggerFactory.getLogger(AsyncSemaphoreImpl.class);
    protected final SemaphoreObserver observer = new SemaphoreObserver();
    private final String nodePath;
    private final CoordinationSession session;
    private final long limit;
    private final String semaphoreName;

    protected AsyncSemaphoreImpl(CoordinationClient client, String nodePath, String semaphoreName, long limit) {
        this.session = client.createSession();
        this.nodePath = nodePath;
        this.semaphoreName = semaphoreName;
        this.limit = limit;

        client.createNode(
                nodePath,
                CoordinationNodeSettings.newBuilder()
                        .build()
        ).thenAccept(status -> {
            if (!status.isSuccess()) {
                observer.createFuture.completeExceptionally(
                        new RuntimeException("Node creation wasn't success, response status = " + status));
            }
        }).thenRun(this::startSession);
    }

    public static CompletableFuture<? extends AsyncSemaphore> newSemaphore(
            CoordinationClient client, String path, String semaphoreName, long limit) {
        final AsyncSemaphoreImpl semaphore = new AsyncSemaphoreImpl(client, path, semaphoreName, limit);
        return semaphore.observer.createFuture;
    }


    public static CompletableFuture<Status> deleteSemaphoreAsync(CoordinationClient client, String path,
                                                                 String semaphoreName, boolean force) {
        final CompletableFuture<Void> sessionStartedFuture = new CompletableFuture<>();
        final CompletableFuture<Status> deleteFuture = new CompletableFuture<>();
        CoordinationSession session = client.createSession();
        final byte[] protectionKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(protectionKey);

        session.start(new Observer() {
                          @Override
                          public void onDeleteSemaphoreResult(Status status) {
                              logger.trace("DELETE SEMAPHORE: onDeleteSemaphoreResult, " + status);
                              deleteFuture.complete(status);
                          }

                          @Override
                          public void onSessionStarted() {
                              logger.trace("DELETE SEMAPHORE: onSessionStarted");
                              sessionStartedFuture.complete(null);
                          }
                      }
        );

        session.sendStartSession(SessionStart.newBuilder()
                .setSessionId(session.getSessionId())
                .setPath(path)
                .setProtectionKey(ByteString.copyFrom(protectionKey))
                .build());

        sessionStartedFuture.thenRun(() -> session.sendDeleteSemaphore(DeleteSemaphore.newBuilder()
                .setName(semaphoreName)
                .setForce(force)
                .setReqId(ThreadLocalRandom.current().nextLong())
                .build())
        );
        return deleteFuture;
    }

    @Override
    public CompletableFuture<Boolean> acquireAsync(SemaphoreSettings settings) {
        observer.acquiredFuture = new CompletableFuture<>();
        session.sendAcquireSemaphore(AcquireSemaphore.newBuilder()
                .setName(semaphoreName)
                .setCount(settings.getCount())
                .setEphemeral(false)
                .setTimeoutMillis(settings.getTimeout())
                .build());
        return observer.checkConsistency.thenCompose(ignored -> observer.acquiredFuture);
    }

    @Override
    public CompletableFuture<Boolean> releaseAsync() {
        observer.releaseFuture = new CompletableFuture<>();
        session.sendReleaseSemaphore(
                ReleaseSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setReqId(ThreadLocalRandom.current().nextInt())
                        .build()
        );
        return observer.checkConsistency.thenCompose(ignored -> observer.releaseFuture).thenCompose(isReleased -> {
            if (isReleased) {
                observer.acquiredFuture = CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(isReleased);
        });
    }

    @Override
    public boolean isAcquired() {
        return observer.acquiredFuture.getNow(false);
    }

    private void startSession() {
        session.start(observer);

        final byte[] protectionKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(protectionKey);

        session.sendStartSession(
                SessionStart.newBuilder()
                        .setSessionId(session.getSessionId())
                        .setPath(nodePath)
                        .setProtectionKey(ByteString.copyFrom(protectionKey))
                        .build());
    }

    protected class SemaphoreObserver implements Observer {
        protected final CompletableFuture<AsyncSemaphoreImpl> createFuture = new CompletableFuture<>();
        private final CompletableFuture<Void> checkConsistency = CompletableFuture.completedFuture(null);
        private CompletableFuture<Boolean> acquiredFuture = new CompletableFuture<>();
        private CompletableFuture<Boolean> releaseFuture = new CompletableFuture<>();

        @Override
        public void onAcquireSemaphoreResult(boolean acquired, Status status) {
            logger.trace("Semaphore.onAcquireSemaphoreResult: " + status.toString() + ", acquired = " + acquired);
            if (!status.isSuccess()) {
                acquiredFuture.completeExceptionally(
                        new RuntimeException("Acquire query return unsuccessful status = " + status));
                return;
            }
            acquiredFuture.complete(acquired);
        }

        @Override
        public void onAcquireSemaphorePending() {
            logger.trace("Semaphore.onAcquireSemaphorePending");
        }

        @Override
        public void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription, Status status) {
            logger.trace("Semaphore.onDescribeSemaphoreResult: " + status);
            if (status.isSuccess()) {
                if (semaphoreDescription.getName().equals(semaphoreName) &&
                        semaphoreDescription.getLimit() == limit) {
                    createFuture.complete(AsyncSemaphoreImpl.this);
                } else {
                    createFuture.completeExceptionally(
                            new SemaphoreCreationException(
                                    "The semaphore has already been created and its settings are different from " +
                                            "yours.", semaphoreDescription));
                }
            } else {
                createFuture.completeExceptionally(
                        new SemaphoreCreationException(
                                "The semaphore has already been created and failed to compare settings with " +
                                        "status = " + status + "."));
            }
        }

        @Override
        public void onDeleteSemaphoreResult(Status status) {
            logger.trace("Semaphore.onDeleteSemaphoreResult: " + status);
            checkConsistency.completeExceptionally(new RuntimeException(
                    "Semaphore with name " + semaphoreName + " on node path " + nodePath + " was deleted."));
        }

        @Override
        public void onCreateSemaphoreResult(Status status) {
            logger.trace("Semaphore.onCreateSemaphoreResult: " + status);
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
                    createFuture.complete(AsyncSemaphoreImpl.this);
                } else {
                    createFuture.completeExceptionally(
                            new SemaphoreCreationException(
                                    "Failed to create semaphore with status = " + status + "."));
                }
            }
        }

        @Override
        public void onFailure(Status status) {
            logger.trace("Semaphore.onFailure: " + status);
            RuntimeException exception =
                    new RuntimeException("Connection or session failure with status = " + status);
            if (createFuture.isDone()) {
                checkConsistency.completeExceptionally(exception);
            } else {
                createFuture.completeExceptionally(exception);
            }
        }

        @Override
        public void onReleaseSemaphoreResult(boolean released, Status status) {
            logger.trace("Semaphore.onReleaseSemaphoreResult: " + status);
            if (!status.isSuccess()) {
                releaseFuture.completeExceptionally(
                        new RuntimeException("Release query return unsuccessful status = " + status));
            }
            releaseFuture.complete(released);
        }

        @Override
        public void onSessionStarted() {
            logger.trace("Semaphore.onSessionStarted");

            session.sendCreateSemaphore(
                    CreateSemaphore.newBuilder()
                            .setName(semaphoreName)
                            .setReqId(ThreadLocalRandom.current().nextInt())
                            .setLimit(limit)
                            .build()
            );
        }
    }
}
