package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;

/**
 * @author Nikolay Perfilov
 * @param <R> type of message received from the server
 * @param <W> type of message to be sent to the server
 */
public abstract class SessionBase<R, W> implements Session {

    protected final GrpcReadWriteStream<R, W> streamConnection;
    protected final AtomicBoolean isWorking = new AtomicBoolean(true);
    protected final String streamId;
    private final ReentrantLock lock = new ReentrantLock();
    private String token;

    public SessionBase(GrpcReadWriteStream<R, W> streamConnection, String streamId) {
        this.streamConnection = streamConnection;
        this.streamId = streamId;
        this.token = streamConnection.authToken();
    }

    public String getStreamId() {
        return streamId;
    }

    protected abstract Logger getLogger();

    protected abstract void sendUpdateTokenRequest(String token);

    protected abstract void onStop();

    protected CompletableFuture<Status> start(GrpcReadStream.Observer<R> streamObserver) {
        lock.lock();

        try {
            getLogger().info("[{}] Session start", streamId);
            return streamConnection.start(message -> {
                if (getLogger().isTraceEnabled()) {
                    getLogger().trace("[{}] Message received:\n{}", streamId, message);
                } else {
                    getLogger().debug("[{}] Message received", streamId);
                }

                if (isWorking.get()) {
                    streamObserver.onNext(message);
                }
            });
        } finally {
            lock.unlock();
        }
    }

    public void send(W request) {
        lock.lock();

        try {
            if (!isWorking.get()) {
                if (getLogger().isTraceEnabled()) {
                    getLogger().trace(
                            "[{}] Session is already closed. This message is NOT sent:\n{}",
                            streamId,
                            request
                    );
                }
                return;
            }
            String currentToken = streamConnection.authToken();
            if (!Objects.equals(token, currentToken)) {
                token = currentToken;
                getLogger().info("[{}] Sending new token", streamId);
                sendUpdateTokenRequest(token);
            }

            if (getLogger().isTraceEnabled()) {
                getLogger().trace("[{}] Sending request:\n{}", streamId, request);
            } else {
                getLogger().debug("[{}] Sending request", streamId);
            }
            streamConnection.sendNext(request);
        } finally {
            lock.unlock();
        }
    }

    private boolean stop() {
        getLogger().info("[{}] Session stop", streamId);
        return isWorking.compareAndSet(true, false);
    }

    @Override
    public boolean shutdown() {
        lock.lock();

        try {
            getLogger().info("[{}] Session shutdown", streamId);
            if (stop()) {
                onStop();
                streamConnection.close();
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
