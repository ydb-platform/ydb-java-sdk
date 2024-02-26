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
class SessionState {
    private final CoordinationSession.State state;
    private final Stream stream;
    private final long sessionID;
    private final AtomicLong reqIdx;

    private SessionState(CoordinationSession.State state) {
        this(state, null, -1, new AtomicLong(0));
    }

    private SessionState(CoordinationSession.State state, Stream stream) {
        this(state, stream, -1, new AtomicLong(0));
    }

    private SessionState(CoordinationSession.State state, Stream stream, long sessionID, AtomicLong reqIdx) {
        this.state = state;
        this.stream = stream;
        this.sessionID = sessionID;
        this.reqIdx = reqIdx;
    }

    public long getSessionId() {
        return sessionID;
    }

    public boolean hasStream(Stream stream) {
        return this.stream == stream;
    }

    public CoordinationSession.State getState() {
        return state;
    }

    public void sendMessage(StreamMsg<?> msg) {
        if (stream != null) {
            stream.sendMsg(reqIdx.incrementAndGet(), msg);
        } else {
            Issue issue = Issue.of("Session has invalid state " + state, Issue.Severity.ERROR);
            msg.handleError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
        }
    }

    public CompletableFuture<Status> stop() {
        if (stream != null) {
            return stream.stop();
        } else {
            return CompletableFuture.completedFuture(Status.SUCCESS);
        }
    }

    public void cancel() {
        if (stream != null) {
            stream.cancelStream();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Session{state=").append(getState()).append(", id=").append(sessionID);
        if (stream != null) {
            sb.append(", stream=").append(stream.hashCode());
        }
        return sb.append("}").toString();
    }

    static SessionState unstarted() {
        return new SessionState(CoordinationSession.State.INITIAL);
    }

    static SessionState lost() {
        return new SessionState(CoordinationSession.State.LOST);
    }

    static SessionState closed() {
        return new SessionState(CoordinationSession.State.CLOSED);
    }

    static SessionState connecting(Stream stream) {
        return new SessionState(CoordinationSession.State.CONNECTING, stream);
    }

    static SessionState reconnecting(Stream stream) {
        return new SessionState(CoordinationSession.State.RECONNECTING, stream);
    }

    static SessionState connected(SessionState prev, long sessionID) {
        return new SessionState(CoordinationSession.State.CONNECTED, prev.stream, sessionID, prev.reqIdx);
    }

    static SessionState reconnected(SessionState prev) {
        return new SessionState(CoordinationSession.State.RECONNECTED, prev.stream, prev.sessionID, prev.reqIdx);
    }

    static SessionState disconnected(SessionState prev, Stream stream) {
        return new SessionState(CoordinationSession.State.RECONNECTING, stream, prev.sessionID, prev.reqIdx);
    }
}
