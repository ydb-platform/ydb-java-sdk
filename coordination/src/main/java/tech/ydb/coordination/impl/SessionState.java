package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 *
 * @author Aleksandr Gorshenin
 */
abstract class SessionState {
    static final SessionState UNSTARTED = SessionState.newDisconnected(CoordinationSession.State.UNSTARTED);
    static final SessionState LOST = SessionState.newDisconnected(CoordinationSession.State.LOST);
    static final SessionState CLOSED = SessionState.newDisconnected(CoordinationSession.State.CLOSED);

    protected final long id;
    protected final CoordinationSession.State state;

    private SessionState(long id, CoordinationSession.State state) {
        this.id = id;
        this.state = state;
    }

    public long getId() {
        return id;
    }

    public CoordinationSession.State getState() {
        return state;
    }

    abstract CompletableFuture<Status> stop();
    abstract void cancel();

    abstract void sendMessage(StreamMsg<?> msg);

    static SessionState newDisconnected(CoordinationSession.State state) {
        return new DisconnectedState(-1, state);
    }

    static SessionState newConnected(CoordinationSession.State state, long id, Stream stream) {
        return new ConnectedState(stream, id, state);
    }

    private static class DisconnectedState extends SessionState {
        private final Status error;

        DisconnectedState(long id, CoordinationSession.State state) {
            super(id, state);
            Issue issue = Issue.of("Session has invalid state " + state, Issue.Severity.ERROR);
            this.error = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue);
        }

        @Override
        public String toString() {
           return new StringBuilder("Session{state=")
                   .append(getState()).append(", id=").append(getId())
                   .append("}").toString();
       }

        @Override
        void sendMessage(StreamMsg<?> msg) {
            msg.handleError(error);
        }

        @Override
        CompletableFuture<Status> stop() {
            return CompletableFuture.completedFuture(Status.SUCCESS);
        }

        @Override
        void cancel() {
        }
    }

    private static class ConnectedState extends SessionState {
        private final Stream stream;
        private final AtomicLong reqIdx = new AtomicLong(0);

        ConnectedState(Stream stream, Long id, CoordinationSession.State state) {
            super(id, state);
            this.stream = stream;
        }

        @Override
        public String toString() {
            return new StringBuilder("Session{state=")
                    .append(getState()).append(", id=").append(getId())
                    .append(", stream=").append(stream.hashCode())
                    .append("}").toString();
        }

        @Override
        void sendMessage(StreamMsg<?> msg) {
            stream.sendMsg(reqIdx.incrementAndGet(), msg);
        }

        @Override
        CompletableFuture<Status> stop() {
            return stream.stop();
        }

        @Override
        void cancel() {
            stream.cancelStream();
        }
    }
}
