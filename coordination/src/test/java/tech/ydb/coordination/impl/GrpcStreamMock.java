package tech.ydb.coordination.impl;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.junit.Assert;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcStreamMock implements GrpcReadWriteStream<SessionResponse, SessionRequest> {
    private final Queue<SessionRequest> requests = new LinkedList<>();
    private final CompletableFuture<Status> finish = new CompletableFuture<>();

    private final Executor executor;

    private boolean isClosed = false;
    private boolean isCanceled = false;
    private Observer<SessionResponse> observer;

    public GrpcStreamMock(Executor executor) {
        this.executor = executor;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public String authToken() {
        return null;
    }

    @Override
    public CompletableFuture<Status> start(Observer<SessionResponse> observer) {
        Assert.assertNull(this.observer);
        this.observer = observer;
        return finish;
    }

    @Override
    public void sendNext(SessionRequest message) {
        requests.offer(message);
    }

    @Override
    public void close() {
        isClosed = true;
    }

    @Override
    public void cancel() {
        isCanceled = true;
    }

    public void closeConnectionOK() {
        executor.execute(() -> finish.complete(Status.SUCCESS));
    }

    public void closeConnecttionUnavailable() {
        executor.execute(() -> finish.complete(Status.of(StatusCode.TRANSPORT_UNAVAILABLE)));
    }

    public boolean hasNextRequest() {
        return !requests.isEmpty();
    }

    public SessionRequest pollNextRequest() {
        return requests.poll();
    }

    public void responseSessionStarted(long id, long timeout) {
        SessionResponse response = SessionResponse.newBuilder().setSessionStarted(
                SessionResponse.SessionStarted.newBuilder().setSessionId(id).setTimeoutMillis(timeout).build()
        ).build();
        executor.execute(() -> observer.onNext(response));

    }

    public void responseSessionStopped(long id) {
        SessionResponse response = SessionResponse.newBuilder().setSessionStopped(
                SessionResponse.SessionStopped.newBuilder().setSessionId(id).build()
        ).build();
        executor.execute(() -> observer.onNext(response));
    }
}
