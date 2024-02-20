package tech.ydb.coordination.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
import tech.ydb.core.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;


class Session implements CoordinationSession {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSession.class);

    private final Rpc rpc;
    private final Clock clock;
    private final Executor executor;
    private final RetryPolicy retryPolicy;

    private final String nodePath;
    private final Duration connectTimeout;
    private final ByteString protectionKey;

    private final Map<Consumer<State>, Consumer<State>> listeners = new ConcurrentHashMap<>();
    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.unstarted());

    Session(Rpc rpc, Clock clock, String nodePath, CoordinationSessionSettings settings) {
        this.rpc = rpc;
        this.clock = clock;
        this.executor = settings.getExecutor() != null ? settings.getExecutor() : ForkJoinPool.commonPool();
        this.retryPolicy = settings.getRetryPolicy();

        this.nodePath = nodePath;
        this.connectTimeout = settings.getConnectTimeout();
        this.protectionKey = createRandomKey();
    }

    @Override
    public long getId() {
        return state.get().getSessionId();
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
        while (!updateState(local, SessionState.closed())) {
            local = state.get();
        }

        logger.debug("{} stopped", this);
        return local.stop();
    }

    @Override
    public CompletableFuture<Status> connect() {
        SessionState local = state.get();
        final Stream stream = new Stream(rpc);
        if (!switchToConnectiong(local, stream)) {
            logger.warn("{} cannot be connected with state {}", this, local.getState());
            return CompletableFuture.completedFuture(Status.of(StatusCode.BAD_REQUEST));
        }

        return connectToSession(stream, 0)
                .thenApplyAsync(res -> establishNewSession(res, stream, Collections.emptyList()), executor);
    }

    private CompletableFuture<Result<Long>> connectToSession(Stream stream, long sessionID) {
        // start new stream
        stream.startStream().whenCompleteAsync((status, th) -> {
            if (th != null) {
                logger.warn("{} stream finished with exception", Session.this, th);
            }

            if (status != null) {
                if (status.isSuccess()) {
                    logger.debug("{} stream finished with status {}", Session.this, status);
                } else {
                    logger.warn("{} stream finished with status {}", Session.this, status);
                }
            }

            List<StreamMsg<?>> messagesToRetry = new ArrayList<>();
            for (StreamMsg<?> msg: stream.getMessages()) {
                if (msg.isIdempotent()) {
                    messagesToRetry.add(msg);
                } else {
                    msg.handleError(Status.of(StatusCode.BAD_SESSION));
                }
            }

            SessionState local = state.get();
            boolean restorable = local.getState() == State.CONNECTED || local.getState() == State.RECONNECTING;
            if (restorable && local.hasStream(stream)) {
                long disconnectedAt = clock.millis();
                restoreSession(disconnectedAt, 0, messagesToRetry);
            } else {
                for (StreamMsg<?> msg: stream.getMessages()) {
                    msg.handleError(Status.of(StatusCode.BAD_SESSION));
                }
            }
        }, executor);

        // and send session start message with id of previos session
        return stream.sendSessionStart(sessionID, nodePath, connectTimeout, protectionKey);
    }


    private void reconnect(Stream stream, long disconnectedAt, int retryCount, List<StreamMsg<?>> messagesToRetry) {
        SessionState local = state.get();
        if (local.getState() != State.RECONNECTING || !local.hasStream(stream)) {
            for (StreamMsg<?> msg: messagesToRetry) {
                msg.handleError(Status.of(StatusCode.BAD_SESSION));
            }
            return;
        }

        connectToSession(stream, local.getSessionId()).whenCompleteAsync((res, th) -> {
            if (res != null && res.isSuccess()) {
                establishNewSession(res, stream, messagesToRetry);
                return;
            }

            if (th != null) {
                logger.warn("{} stream retry {} finished with exception", Session.this, retryCount, th);
            }

            if (res != null) {
                logger.debug("{} stream retry {} finished with status {}", Session.this, retryCount, res.getStatus());
            }

            SessionState localState = state.get();
            boolean restorable = localState.getState() == State.RECONNECTING;
            if (restorable && local.hasStream(stream)) {
                restoreSession(disconnectedAt, retryCount + 1, messagesToRetry);
            } else {
                for (StreamMsg<?> msg: messagesToRetry) {
                    msg.handleError(Status.of(StatusCode.BAD_SESSION));
                }
            }
        }, executor);
    }

    private void restoreSession(long disconnectedAt, int retryCount, List<StreamMsg<?>> messagesToRetry) {
        SessionState local = state.get();
        if (local.getState() != State.CONNECTED && local.getState() != State.RECONNECTING) {
            return;
        }

        long elapsedTimeMs = clock.millis() - disconnectedAt;
        long retryInMs = retryPolicy.nextRetryMs(retryCount, elapsedTimeMs);
        if (retryInMs < 0) {
            logger.debug("stream {} close connection by retry policy");
            switchToLost(local);
            for (StreamMsg<?> msg: messagesToRetry) {
                msg.handleError(Status.of(StatusCode.BAD_SESSION));
            }
            return;
        }

        Stream stream = new Stream(rpc);
        if (!switchToConnectiong(local, stream)) {
            logger.warn("{} cannot be reconnected with state {}", this, state.get().getState());
            for (StreamMsg<?> msg: messagesToRetry) {
                msg.handleError(Status.of(StatusCode.BAD_SESSION));
            }
            return;
        }

        if (retryInMs > 0) {
            logger.debug("stream {} shedule next retry {} in {} ms", this, retryCount, retryInMs);
            rpc.getScheduler().schedule(
                    () -> reconnect(stream, disconnectedAt, retryCount, messagesToRetry),
                    retryInMs,
                    TimeUnit.MILLISECONDS
            );
        } else {
            logger.debug("stream {} immediatelly retry {}", this, retryCount);
            reconnect(stream, disconnectedAt, retryCount, messagesToRetry);
        }
    }

    private Status establishNewSession(Result<Long> result, Stream stream, List<StreamMsg<?>> messagesToRetry) {
        if (!result.isSuccess()) {
            return result.getStatus();
        }

        SessionState local = state.get();
        SessionState connected = switchToConnected(local, result.getValue(), stream);
        if (connected == null || !updateState(local, connected)) {
            stream.stop();
            return Status.of(
                    StatusCode.CANCELLED, null, Issue.of("{} cannot handle successful session", Issue.Severity.ERROR)
            );
        }

        for (StreamMsg<?> msg: messagesToRetry) {
            connected.sendMessage(msg);
        }

        return Status.SUCCESS;
    }

    private boolean switchToConnectiong(SessionState local, Stream stream) {
        if (local.getState() == State.UNSTARTED) {
            return updateState(local, SessionState.connecting(stream));
        }
        if (local.getState() == State.LOST) {
            return updateState(local, SessionState.reconnecting(stream));
        }
        if (local.getState() == State.CONNECTED || local.getState() == State.RECONNECTING) {
            return updateState(local, SessionState.disconnected(local, stream));
        }
        return false;
    }

    private SessionState switchToConnected(SessionState local, long id, Stream stream) {
        if (local.getState() == State.CONNECTING && local.hasStream(stream)) {
            return SessionState.connected(local, id);
        }
        if (local.getState() == State.RECONNECTING && local.hasStream(stream)) {
            return SessionState.reconnected(local);
        }

        return null;
    }

    private boolean switchToLost(SessionState local) {
        if (local.getState() == State.CONNECTING) {
            return updateState(local, SessionState.unstarted());
        }
        if (local.getState() == State.RECONNECTING) {
            return updateState(local, SessionState.lost());
        }

        return false;
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
