package tech.ydb.topic.read.events;

/**
 * @author Nikolay Perfilov
 */
public interface StopPartitionSessionEvent {
    long getPartitionSessionId();

    long getCommittedOffset();

    boolean isGraceful();

    void confirm();
}
