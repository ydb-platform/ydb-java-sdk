package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
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


class SessionImpl implements CoordinationSession {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSession.class);

    private final Rpc rpc;
    private final String nodePath;
    private final Duration connectTimeout;
    private final ByteString protectionKey;
    private final Executor executor;

    private final Map<Consumer<State>, Consumer<State>> listeners = new ConcurrentHashMap<>();

    private final AtomicReference<State> state = new AtomicReference<>(State.UNSTARTED);
    private final AtomicReference<Stream> stream = new AtomicReference<>();
    private final AtomicLong requestsCounter = new AtomicLong(0);
    private volatile long sessionID = -1;

    SessionImpl(Rpc rpc, String nodePath, CoordinationSessionSettings settings) {
        this.rpc = rpc;
        this.nodePath = nodePath;
        this.connectTimeout = settings.getConnectTimeout();
        this.protectionKey = createRandomKey();
        this.executor = settings.getExecutor() != null ? settings.getExecutor() : ForkJoinPool.commonPool();
    }

    @Override
    public String toString() {
        State localState = state.get();
        Long localID = sessionID;
        Stream localStream = stream.get();

        StringBuilder sb = new StringBuilder("Session{state=")
                .append(localState).append(", id=").append(localID);
        if (localStream != null) {
            sb.append(", stream=").append(localStream.hashCode());
        }

        return sb.append("}").toString();
    }

    @Override
    public CompletableFuture<Status> connect() {
        logger.trace("{} try to connect", this);
        if (!switchToConnecting()) {
            Status error = invalidStateStatus();
            logger.warn("{} cannot be connected by {}", this, error);
            return CompletableFuture.completedFuture(error);
        }

        final CompletableFuture<Status> connectFuture = new CompletableFuture<>();
        final Stream newStream = new Stream(rpc);

        newStream.startStream().whenCompleteAsync((status, th) -> {
            if (th != null) {
                logger.warn("{} stream finished with exception", this, th);
                if (connectFuture.completeExceptionally(th)) {
                    switchToDisconnected();
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
                    switchToDisconnected();
                    return;
                }
            }

            disconnect(th, status);
        }, executor);

        newStream.sendSessionStart(0, nodePath, connectTimeout, protectionKey).whenCompleteAsync((res, th) -> {
            if (th != null || res == null) {
                connectFuture.completeExceptionally(th);
                switchToDisconnected();
                return;
            }

            if (!res.isSuccess()) {
                connectFuture.complete(res.getStatus());
                switchToDisconnected();
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

    @Override
    public CompletableFuture<Status> stop() {
        if (switchToClose()) {
            logger.debug("{} closed", this);
        }

        Stream local = stream.get();
        if (local == null) { // session is unstarted
            return CompletableFuture.completedFuture(Status.SUCCESS);
        }

        return local.sendSessionStop();
    }

    @Override
    public long getId() {
        return sessionID;
    }

    @Override
    public State getState() {
        return state.get();
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

    private void disconnect(Throwable th, Status status) {
        if (switchToLost()) {
            Stream local = stream.get();
            if (local != null) {
                // disconnect grpc stream
                local.cancelStream();
            }
        }
    }

    private boolean establishNewSession(long newSessionID, Stream newStream) {
        Long previousID = sessionID;
        Long previousIdx = requestsCounter.get();

        Stream previous = stream.getAndSet(newStream);
        sessionID = newSessionID;
        requestsCounter.set(0);

        if (!switchToConnected()) {
            sessionID = previousID;
            requestsCounter.set(previousIdx);
            stream.getAndSet(previous).cancelStream();
            return false;
        }

        if (previous != null) {
            previous.cancelStream();
        }
        return true;
    }

    private boolean updateState(State previous, State newState) {
        if (!state.compareAndSet(previous, newState)) {
            return false;
        }

        for (Consumer<CoordinationSession.State> listener: listeners.values()) {
            listener.accept(newState);
        }
        return true;
    }

    private boolean updateState(State newState) {
        State old = state.getAndSet(newState);
        if (old == newState) {
            return false;
        }

        for (Consumer<CoordinationSession.State> listener: listeners.values()) {
            listener.accept(newState);
        }

        return true;
    }

    private boolean switchToClose() {
        return updateState(State.CLOSED);
    }

    private boolean switchToLost() {
        return updateState(State.LOST);
    }

    private boolean switchToConnecting() {
        return updateState(State.UNSTARTED, State.CONNECTING) || updateState(State.LOST, State.RECONNECTING);
    }

    private boolean switchToReconnecting() {
        return updateState(State.CONNECTED, State.RECONNECTING) || updateState(State.RECONNECTED, State.RECONNECTING);
    }

    private boolean switchToConnected() {
        return updateState(State.CONNECTING, State.CONNECTED) || updateState(State.RECONNECTING, State.RECONNECTED);
    }

    private boolean switchToDisconnected() {
        return updateState(State.CONNECTING, State.UNSTARTED) || updateState(State.RECONNECTING, State.LOST);
    }

    private Status invalidStateStatus() {
        Issue issue = Issue.of("Session has invalid state " + getState(), Issue.Severity.ERROR);
        return Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue);
    }

    @Override
    public CompletableFuture<Status> createSemaphore(String name, long limit, byte[] data) {
        return sendStatusMsg(StreamMsg.createSemaphore(name, limit, data));
    }

    @Override
    public CompletableFuture<Status> updateSemaphore(String name, byte[] data) {
        return sendStatusMsg(StreamMsg.updateSemaphore(name, data));
    }

    @Override
    public CompletableFuture<Status> deleteSemaphore(String name, boolean force) {
        return sendStatusMsg(StreamMsg.deleteSemaphore(name, force));
    }

    private CompletableFuture<Status> sendStatusMsg(StreamMsg<Status> msg) {
        Stream localStream = stream.get();
        if (localStream == null) {
            return CompletableFuture.completedFuture(invalidStateStatus());
        }

        localStream.sendMsg(requestsCounter.incrementAndGet(), msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String name, DescribeSemaphoreMode mode) {
        return sendResultMsg(StreamMsg.describeSemaphore(name, mode));
    }

    @Override
    public CompletableFuture<Result<SemaphoreWatcher>> watchSemaphore(String name,
            DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode) {
        return sendResultMsg(StreamMsg.watchSemaphore(name, describeMode, watchMode));
    }

    private <T> CompletableFuture<Result<T>> sendResultMsg(StreamMsg<Result<T>> msg) {
        Stream localStream = stream.get();
        if (localStream == null) {
            return CompletableFuture.completedFuture(Result.fail(invalidStateStatus()));
        }

        localStream.sendMsg(requestsCounter.incrementAndGet(), msg);
        return msg.getResult().thenApplyAsync(Function.identity(), executor);
    }

    @Override
    public CompletableFuture<Result<SemaphoreLease>> acquireSemaphore(String name, long count, byte[] data,
            Duration timeout) {
        StreamMsg<Result<Boolean>> msg = StreamMsg.acquireSemaphore(name, count, data, false, timeout.toMillis());
        return sendAcquireMsg(name, msg);
    }

    @Override
    public CompletableFuture<Result<SemaphoreLease>> acquireEphemeralSemaphore(String name, boolean exclusive,
            byte[] data, Duration timeout) {
        long count = exclusive ? -1L : 1L;
        StreamMsg<Result<Boolean>> msg = StreamMsg.acquireSemaphore(name, count, data, true, timeout.toMillis());
        return sendAcquireMsg(name, msg);
    }

    private CompletableFuture<Result<SemaphoreLease>> sendAcquireMsg(String name, StreamMsg<Result<Boolean>> msg) {
        Stream localStream = stream.get();
        if (localStream == null) {
            return CompletableFuture.completedFuture(Result.fail(invalidStateStatus()));
        }

        localStream.sendMsg(requestsCounter.incrementAndGet(), msg);
        return msg.getResult().thenApplyAsync(result -> {
            if (!result.isSuccess()) {
                return result.map(null);
            }
            if (!result.getValue()) {
                return Result.fail(Status.of(StatusCode.TIMEOUT));
            }

            return Result.success(new LeaseImpl(SessionImpl.this, name));
        }, executor);
    }

    CompletableFuture<Boolean> releaseSemaphore(String name) {
        StreamMsg<Result<Boolean>> msg = StreamMsg.releaseSemaphore(name);

        Stream localStream = stream.get();
        if (localStream == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        localStream.sendMsg(requestsCounter.incrementAndGet(), msg);
        return msg.getResult().thenApplyAsync(result -> result.isSuccess() && result.getValue(), executor);
    }

    private static ByteString createRandomKey() {
        byte[] protectionKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(protectionKey);
        return ByteString.copyFrom(protectionKey);
    }
}
