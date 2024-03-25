package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private String token;

    public SessionBase(GrpcReadWriteStream<R, W> streamConnection) {
        this.streamConnection = streamConnection;
        this.token = streamConnection.authToken();
    }

    protected abstract Logger getLogger();

    protected abstract void sendUpdateTokenRequest(String token);

    protected abstract void onStop();

    protected synchronized CompletableFuture<Status> start(GrpcReadStream.Observer<R> streamObserver) {
        getLogger().info("Session start");
        return streamConnection.start(message -> {
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Message received:\n{}", message);
            } else {
                getLogger().debug("Message received");
            }

            if (isWorking.get()) {
                streamObserver.onNext(message);
            }
        });
    }

    public synchronized void send(W request) {
        if (!isWorking.get()) {
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Session is already closed. This message is NOT sent:\n{}", request);
            }
            return;
        }
        String currentToken = streamConnection.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            getLogger().info("Sending new token");
            sendUpdateTokenRequest(token);
        }

        if (getLogger().isTraceEnabled()) {
            getLogger().trace("Sending request:\n{}", request);
        } else {
            getLogger().debug("Sending request");
        }
        streamConnection.sendNext(request);
    }

    private boolean stop() {
        getLogger().info("Session stop");
        return isWorking.compareAndSet(true, false);
    }


    @Override
    public synchronized boolean shutdown() {
        getLogger().info("Session shutdown");
        if (stop()) {
            onStop();
            streamConnection.close();
            return true;
        }
        return false;
    }
}
