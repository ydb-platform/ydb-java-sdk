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
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.coordination.scenario.semaphore.util.Pair;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.proto.coordination.SemaphoreDescription;
import tech.ydb.proto.coordination.SessionRequest.AcquireSemaphore;
import tech.ydb.proto.coordination.SessionRequest.CreateSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DeleteSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DescribeSemaphore;
import tech.ydb.proto.coordination.SessionRequest.ReleaseSemaphore;
import tech.ydb.proto.coordination.SessionRequest.SessionStart;


public class AsyncSemaphoreImpl implements AsyncSemaphore {
    private static final Logger logger = LoggerFactory.getLogger(AsyncSemaphoreImpl.class);
    protected final SemaphoreObserver observer;
    private final CoordinationSession session;
    private final String semaphoreName;

    protected AsyncSemaphoreImpl(CoordinationSession session, SemaphoreObserver observer) {
        this.observer = observer;
        this.session = session;
        this.semaphoreName = observer.semaphoreName;
    }

    public static CompletableFuture<AsyncSemaphore> newAsyncSemaphore(CoordinationClient client, String path,
                                                                      String semaphoreName, long limit) {
        final CompletableFuture<AsyncSemaphore> creationFuture = new CompletableFuture<>();
        prepareSessionAndCreateCoordinationSemaphore(client, path, semaphoreName, limit, creationFuture)
                .handle((pair, ex) -> {
                    if (ex == null) {
                        return creationFuture.complete(new AsyncSemaphoreImpl(pair.getKey(), pair.getValue()));
                    } else {
                        return creationFuture.completeExceptionally(ex);
                    }
                });
        return creationFuture;
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
        });

        session.sendStartSession(SessionStart.newBuilder().setSessionId(session.getSessionId()).setPath(path)
                .setProtectionKey(ByteString.copyFrom(protectionKey)).build());

        sessionStartedFuture.thenRun(() -> session.sendDeleteSemaphore(
                DeleteSemaphore.newBuilder().setName(semaphoreName).setForce(force)
                        .setReqId(ThreadLocalRandom.current().nextLong()).build()));
        return deleteFuture;
    }

    protected static <T> CompletableFuture<Pair<CoordinationSession, SemaphoreObserver>>
    prepareSessionAndCreateCoordinationSemaphore(CoordinationClient client, String path, String semaphoreName,
                                                 long limit, CompletableFuture<T> cancelFuture) {
        final SemaphoreObserver[] observer = new SemaphoreObserver[1];
        final CoordinationSession[] session = new CoordinationSession[1];
        return client.createNode(path, CoordinationNodeSettings.newBuilder().build()
        ).thenCompose(status -> {
            if (!status.isSuccess()) {
                throw new UnexpectedResultException("Node creation wasn't success", status);
            }
            if (cancelFuture.isCancelled()) {
                return getCanceledFuture();
            }
            session[0] = client.createSession();
            observer[0] = new SemaphoreObserver(session[0], path, semaphoreName, limit);
            session[0].start(observer[0]);
            return observer[0].createSemaphore(cancelFuture);
        }).thenApply(status -> {
            if (!status.isSuccess()) {
                session[0].stop();
                throw new UnexpectedResultException("Couldn't create semaphore", status);
            }
            return new Pair<>(session[0], observer[0]);
        });
    }

    private static <T> CompletableFuture<T> getCanceledFuture() {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.cancel(true);
        return future;
    }

    @Override
    public CompletableFuture<Boolean> acquireAsync(SemaphoreSettings settings) {
        observer.acquiredFuture = new CompletableFuture<>();
        session.sendAcquireSemaphore(
                AcquireSemaphore.newBuilder().setName(semaphoreName).setCount(settings.getCount()).setEphemeral(false)
                        .setTimeoutMillis(settings.getTimeout()).build());
        return observer.checkConsistency.thenCompose(ignored -> observer.acquiredFuture);
    }

    @Override
    public CompletableFuture<Boolean> releaseAsync() {
        observer.releaseFuture = new CompletableFuture<>();
        session.sendReleaseSemaphore(
                ReleaseSemaphore.newBuilder().setName(semaphoreName).setReqId(ThreadLocalRandom.current().nextInt())
                        .build());
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

    protected static class SemaphoreObserver implements Observer {
        protected final CompletableFuture<Status> createSemaphoreFuture = new CompletableFuture<>();
        private final CompletableFuture<Void> checkConsistency = CompletableFuture.completedFuture(null);
        private final String nodePath;
        private final CoordinationSession session;
        private final long limit;
        private final String semaphoreName;
        private CompletableFuture<?> cancelFuture;
        private CompletableFuture<Boolean> acquiredFuture = new CompletableFuture<>();
        private CompletableFuture<Boolean> releaseFuture = new CompletableFuture<>();

        public SemaphoreObserver(CoordinationSession session, String nodePath, String semaphoreName, long limit) {
            this.session = session;
            this.nodePath = nodePath;
            this.semaphoreName = semaphoreName;
            this.limit = limit;
        }

        @Override
        public void onAcquireSemaphoreResult(boolean acquired, Status status) {
            logger.trace("Semaphore.onAcquireSemaphoreResult: " + status.toString() + ", acquired = " + acquired);
            if (!status.isSuccess()) {
                acquiredFuture.completeExceptionally(
                        new UnexpectedResultException("Acquire query return unsuccessful", status));
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
            if (!cancelFuture.isCancelled()) {
                if (status.isSuccess()) {
                    if (semaphoreDescription.getName().equals(semaphoreName) &&
                            semaphoreDescription.getLimit() == limit) {
                        createSemaphoreFuture.complete(status);
                    } else {
                        createSemaphoreFuture.completeExceptionally(new UnexpectedResultException(
                                "The semaphore has already been created and its settings are different from " +
                                        "yours. " +
                                        semaphoreDescription, status));
                    }
                } else {
                    createSemaphoreFuture.completeExceptionally(new UnexpectedResultException(
                            "The semaphore has already been created and failed to compare settings", status));
                }
            } else {
                createSemaphoreFuture.cancel(true);
            }
        }

        @Override
        public void onDeleteSemaphoreResult(Status status) {
            logger.trace("Semaphore.onDeleteSemaphoreResult: " + status);
            checkConsistency.completeExceptionally(new UnexpectedResultException(
                    "Semaphore with name " + semaphoreName + " on node path " + nodePath + " was deleted.", status));
        }

        @Override
        public void onCreateSemaphoreResult(Status status) {
            logger.trace("Semaphore.onCreateSemaphoreResult: " + status);
            if (!cancelFuture.isCancelled()) {
                if (status.getCode() == StatusCode.ALREADY_EXISTS) {
                    session.sendDescribeSemaphore(
                            DescribeSemaphore.newBuilder().setReqId(ThreadLocalRandom.current().nextLong())
                                    .setName(semaphoreName).setIncludeOwners(false).setIncludeWaiters(false)
                                    .setWatchData(false).setWatchOwners(false).build());
                } else {
                    if (status.isSuccess()) {
                        createSemaphoreFuture.complete(status);
                    } else {
                        createSemaphoreFuture.completeExceptionally(
                                new UnexpectedResultException("Failed to create semaphore", status));
                    }
                }
            } else {
                createSemaphoreFuture.cancel(true);
            }
        }

        @Override
        public void onFailure(Status status) {
            logger.trace("Semaphore.onFailure: " + status);
            UnexpectedResultException exception =
                    new UnexpectedResultException("Connection or session failure", status);
            if (createSemaphoreFuture.isDone()) {
                checkConsistency.completeExceptionally(exception);
            } else {
                createSemaphoreFuture.completeExceptionally(exception);
            }
        }

        @Override
        public void onReleaseSemaphoreResult(boolean released, Status status) {
            logger.trace("Semaphore.onReleaseSemaphoreResult: " + status);
            if (!status.isSuccess()) {
                releaseFuture.completeExceptionally(
                        new UnexpectedResultException("Release query return unsuccessful status", status));
            }
            releaseFuture.complete(released);
        }

        @Override
        public void onSessionStarted() {
            logger.trace("Semaphore.onSessionStarted");
            if (!cancelFuture.isCancelled()) {
                session.sendCreateSemaphore(
                        CreateSemaphore.newBuilder().setName(semaphoreName)
                                .setReqId(ThreadLocalRandom.current().nextInt())
                                .setLimit(limit).build());
            } else {
                createSemaphoreFuture.cancel(true);
            }
        }

        protected <T> CompletableFuture<Status> createSemaphore(CompletableFuture<T> cancelFuture) {
            final byte[] protectionKey = new byte[16];
            ThreadLocalRandom.current().nextBytes(protectionKey);
            this.cancelFuture = cancelFuture;

            session.sendStartSession(SessionStart.newBuilder().setSessionId(session.getSessionId()).setPath(nodePath)
                    .setProtectionKey(ByteString.copyFrom(protectionKey)).build());
            return createSemaphoreFuture;
        }
    }
}
