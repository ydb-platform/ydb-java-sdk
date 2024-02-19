package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;


class Session implements CoordinationSession {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSession.class);

    private final Rpc rpc;
    private final String nodePath;
    private final Duration connectTimeout;
    private final ByteString protectionKey;
    private final Executor executor;

    private final Map<Consumer<State>, Consumer<State>> listeners = new ConcurrentHashMap<>();

    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.UNSTARTED);

    Session(Rpc rpc, String nodePath, CoordinationSessionSettings settings) {
        this.rpc = rpc;
        this.nodePath = nodePath;
        this.connectTimeout = settings.getConnectTimeout();
        this.protectionKey = createRandomKey();
        this.executor = settings.getExecutor() != null ? settings.getExecutor() : ForkJoinPool.commonPool();
    }


    @Override
    public long getId() {
        return state.get().getId();
    }

    @Override
    public State getState() {
        return state.get().getState();
    }

    @Override
    public String toString() {
        return state.get().toString();
    }

    @Override
    public void addStateListener(Consumer<State> listener) {
        if (listener != null) {
            listeners.put(listener, listener);
        }
    }

    @Override
    public void removeStateListener(Consumer<State> listener) {
        listeners.remove(listener);
    }

    @Override
    public CompletableFuture<Status> stop() {
        SessionState local = state.get();
        while (!updateState(local, SessionState.CLOSED)) {
            local = state.get();
        }

        logger.debug("{} stopped", this);
        return local.stop();
    }

    @Override
    public CompletableFuture<Status> connect() {
        logger.trace("{} try to connect", this);
        SessionState local = state.get();
        if (!switchToConnectiong(local)) {
            Status error = invalidStateStatus(local);
            logger.warn("{} cannot be connected by {}", this, error);
            return CompletableFuture.completedFuture(error);
        }

        final CompletableFuture<Status> connectFuture = new CompletableFuture<>();
        final Stream newStream = new Stream(rpc);

        newStream.startStream().whenCompleteAsync((status, th) -> {
            if (th != null) {
                logger.warn("{} stream finished with exception", this, th);
                if (connectFuture.completeExceptionally(th)) {
                    switchToDisconnected(local);
                    return;
                }
            }
            if (status != null) {
                if (status.isSuccess()) {
                    logger.debug("{} stream finished with status {}", this, status);
                } else {
                    logger.warn("{} stream finished with status {}", this, status);
                }
                if (connectFuture.complete(status)) {
                    switchToDisconnected(local);
                    return;
                }
            }

            disconnect(th, status);
        }, executor);

        newStream.sendSessionStart(0, nodePath, connectTimeout, protectionKey).whenCompleteAsync((res, th) -> {
            if (th != null || res == null) {
                connectFuture.completeExceptionally(th);
                switchToDisconnected(local);
                return;
            }

            if (!res.isSuccess()) {
                connectFuture.complete(res.getStatus());
                switchToDisconnected(local);
                return;
            }

            if (!establishNewSession(res.getValue(), newStream)) {
                Issue issue = Issue.of("Cannot establish new session", Issue.Severity.ERROR);
                Status error = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue);
                connectFuture.complete(error);
                return;
            }

            connectFuture.complete(Status.SUCCESS);

        }, executor);


        return connectFuture;
    }

    private boolean establishNewSession(long id, Stream stream) {
        SessionState local = state.get();
        if (!switchToConnected(local, id, stream)) {
            stream.cancelStream();
            return false;
        }

        local.cancel();
        return true;
    }

    private void disconnect(Throwable th, Status status) {
        SessionState local = state.get();
        if (switchToDisconnected(local)) {
            // disconnect grpc stream
            local.cancel();
        }
    }

    private boolean switchToConnectiong(SessionState local) {
        if (local.getState() == State.UNSTARTED) {
            return updateState(local, SessionState.newDisconnected(State.CONNECTING));
        }
        if (local.getState() == State.LOST) {
            return updateState(local, SessionState.newDisconnected(State.RECONNECTING));
        }

        return false;
    }

    private boolean switchToConnected(SessionState local, long id, Stream stream) {
        if (local.getState() == State.CONNECTING) {
            return updateState(local, SessionState.newConnected(State.CONNECTED, id, stream));
        }
        if (local.getState() == State.RECONNECTING) {
            return updateState(local, SessionState.newConnected(State.RECONNECTED, id, stream));
        }

        return false;
    }

    private boolean switchToDisconnected(SessionState local) {
        return updateState(local, SessionState.LOST);
    }

    private boolean updateState(SessionState previous, SessionState next) {
        if (next == null || !state.compareAndSet(previous, next)) {
            return false;
        }

        if (next.getState() != previous.getState()) {
            for (Consumer<CoordinationSession.State> listener: listeners.values()) {
                listener.accept(next.getState());
            }
        }
        return true;
    }

    @Override
    public CompletableFuture<Status> createSemaphore(String name, long limit, byte[] data) {
        StreamMsg<Status> msg = StreamMsg.createSemaphore(name, limit, data);
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Status> updateSemaphore(String name, byte[] data) {
        StreamMsg<Status> msg = StreamMsg.updateSemaphore(name, data);
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Status> deleteSemaphore(String name, boolean force) {
        StreamMsg<Status> msg = StreamMsg.deleteSemaphore(name, force);
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String name, DescribeSemaphoreMode mode) {
        StreamMsg<Result<SemaphoreDescription>> msg = StreamMsg.describeSemaphore(name, mode);
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Result<SemaphoreWatcher>> watchSemaphore(String name,
            DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode) {
        StreamMsg<Result<SemaphoreWatcher>> msg = StreamMsg.watchSemaphore(name, describeMode, watchMode);
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Result<SemaphoreLease>> acquireSemaphore(String name, long count, byte[] data,
            Duration timeout) {
        StreamMsg<Result<Boolean>> msg = StreamMsg.acquireSemaphore(name, count, data, false, timeout.toMillis());
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(new LeaseCreator(name), executor);
    }

    @Override
    public CompletableFuture<Result<SemaphoreLease>> acquireEphemeralSemaphore(String name, boolean exclusive,
            byte[] data, Duration timeout) {
        long count = exclusive ? -1L : 1L;
        StreamMsg<Result<Boolean>> msg = StreamMsg.acquireSemaphore(name, count, data, true, timeout.toMillis());
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(new LeaseCreator(name), executor);
    }

    CompletableFuture<Boolean> releaseSemaphore(String name) {
        StreamMsg<Result<Boolean>> msg = StreamMsg.releaseSemaphore(name);
        state.get().sendMessage(msg);
        return msg.getResult().thenApplyAsync(result -> result.isSuccess() && result.getValue(), executor);
    }

    private static ByteString createRandomKey() {
        byte[] protectionKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(protectionKey);
        return ByteString.copyFrom(protectionKey);
    }

    private static Status invalidStateStatus(SessionState state) {
        Issue issue = Issue.of("Session has invalid state " + state.getState(), Issue.Severity.ERROR);
        return Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue);
    }

    private class LeaseCreator implements Function<Result<Boolean>, Result<SemaphoreLease>> {
        private final String name;

        LeaseCreator(String name) {
            this.name = name;
        }

        @Override
        public Result<SemaphoreLease> apply(Result<Boolean> acquireResult) {
            if (!acquireResult.isSuccess()) {
                return acquireResult.map(null);
            }
            if (!acquireResult.getValue()) {
                return Result.fail(Status.of(StatusCode.TIMEOUT));
            }

            return Result.success(new Lease(Session.this, name));
        }
    }
}
