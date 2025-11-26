package tech.ydb.topic.read.impl.events;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SessionStartedEvent {
    private final String sessionId;

    public SessionStartedEvent(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return this.sessionId;
    }
}
