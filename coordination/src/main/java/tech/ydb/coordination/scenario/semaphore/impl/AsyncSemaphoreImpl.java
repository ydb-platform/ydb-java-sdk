package tech.ydb.coordination.scenario.semaphore.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSession.Observer;
import tech.ydb.coordination.scenario.semaphore.AsyncSemaphore;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
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
                                                              String semaphoreName, long limit, boolean createNode) {
        return newSemaphoreDetail(client, path, semaphoreName, limit, createNode, AsyncSemaphoreImpl::new);
    }

    protected static <T> CompletableFuture<T> newSemaphoreDetail(CoordinationClient client, String path,
                                          String semaphoreName, long limit, boolean createNode,
                                          BiFunction<CoordinationSession, SemaphoreObserver, T> semaphoreCreator) {
        final CompletableFuture<T> creationFuture = new CompletableFuture<>();
        final CoordinationSession session = client.createSession();
        final SemaphoreObserver observer = new SemaphoreObserver(session, path, semaphoreName, limit);
        if (createNode) {
            tryCreateCoordinationNode(client, session, observer, creationFuture, semaphoreCreator);
        } else {
            tryCreateCoordinationSemaphore(session, observer, creationFuture, semaphoreCreator);
        }
        return creationFuture;
    }

    public static CompletableFuture<Status> deleteSemaphoreAsync(CoordinationClient client, String path,
                                                                 String semaphoreName, boolean force) {
        final CompletableFuture<Status> deleteFuture = new CompletableFuture<>();
        final CoordinationSession session = client.createSession();

        session.start(new Observer() {
            @Override
            public void onDeleteSemaphoreResult(Status status) {
                logger.trace("DeletingSemaphore.onDeleteSemaphoreResult, " + status);
                deleteFuture.complete(status);
            }

            @Override
            public void onFailure(Status status) {
                logger.trace("DeletingSemaphore.onFailure, " + status);
                deleteFuture.completeExceptionally(
                        new UnexpectedResultException("Connection or session failure", status));
            }

            @Override
            public void onSessionStarted() {
                logger.trace("DeletingSemaphore.onSessionStarted");
                session.sendDeleteSemaphore(
                        DeleteSemaphore.newBuilder().setName(semaphoreName).setForce(force)
                                .setReqId(ThreadLocalRandom.current().nextLong()).build());
            }
        });

        final byte[] protectionKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(protectionKey);
        session.sendStartSession(SessionStart.newBuilder().setSessionId(session.getSessionId()).setPath(path)
                .setProtectionKey(ByteString.copyFrom(protectionKey)).build());
        return deleteFuture;
    }

    protected static <T> void tryCreateCoordinationNode(CoordinationClient client, CoordinationSession session,
                                            SemaphoreObserver observer, CompletableFuture<T> creationFuture,
                                            BiFunction<CoordinationSession, SemaphoreObserver, T> semaphoreCreator) {
        client.createNode(observer.nodePath, CoordinationNodeSettings.newBuilder().build())
                .whenComplete((status, th) -> {
            if (th != null) {
                creationFuture.completeExceptionally(th);
                return;
            }
            if (status != null) {
                if (status.isSuccess() && !creationFuture.isDone()) {
                    tryCreateCoordinationSemaphore(session, observer, creationFuture, semaphoreCreator);
                    return;
                }
            }
            creationFuture.completeExceptionally(
                    new UnexpectedResultException("Node creation wasn't success", status != null ? status :
                            Status.of(StatusCode.UNUSED_STATUS)));
        });
    }


    protected static <T> void tryCreateCoordinationSemaphore(CoordinationSession session, SemaphoreObserver observer,
         CompletableFuture<T> creationFuture, BiFunction<CoordinationSession, SemaphoreObserver, T> semaphoreCreator) {
        session.start(observer);
        observer.createSemaphore(creationFuture).whenComplete((status, th) -> {
            if (th != null) {
                creationFuture.completeExceptionally(th);
            }
            if (status != null) {
                if (status.isSuccess() && !creationFuture.isDone()) {
                    creationFuture.complete(semaphoreCreator.apply(session, observer));
                } else {
                    session.stop();
                }
            }
            creationFuture.completeExceptionally(
                    new UnexpectedResultException("Couldn't create semaphore", status != null ? status :
                            Status.of(StatusCode.UNUSED_STATUS)));
        });
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
            if (!cancelFuture.isDone()) {
                if (status.isSuccess()) {
                    if (semaphoreDescription.getName().equals(semaphoreName) &&
                            semaphoreDescription.getLimit() == limit) {
                        createSemaphoreFuture.complete(status);
                    } else {
                        createSemaphoreFuture.completeExceptionally(new UnexpectedResultException(
                                "The semaphore has already been created and its settings are different from " +
                                        "yours. " + semaphoreDescription, status));
                    }
                } else {
                    createSemaphoreFuture.completeExceptionally(new UnexpectedResultException(
                            "The semaphore has already been created and failed to compare settings", status));
                }
            } else {
                createSemaphoreFuture.completeExceptionally(
                        new RuntimeException("Future with Semaphore was done before adjusted."));
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
            if (!cancelFuture.isDone()) {
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
                createSemaphoreFuture.completeExceptionally(
                        new RuntimeException("Future with Semaphore was done before adjusted."));
            }
        }

        @Override
        public void onFailure(Status status) {
            logger.trace("Semaphore.onFailure: " + status);
            final UnexpectedResultException exception =
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
            if (!cancelFuture.isDone()) {
                session.sendCreateSemaphore(
                        CreateSemaphore.newBuilder().setName(semaphoreName)
                                .setReqId(ThreadLocalRandom.current().nextInt())
                                .setLimit(limit).build());
            } else {
                createSemaphoreFuture.completeExceptionally(
                        new RuntimeException("Future with Semaphore was done before adjusted."));
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
