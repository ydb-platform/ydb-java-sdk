package tech.ydb.topic.read.events;

/**
 * @author Nikolay Perfilov
 */
public interface ReadEventHandler {

    void onMessages(DataReceivedEvent event);

    void onCommitOffsetAcknowledgement(CommitOffsetAcknowledgementEvent event);

    void onStartPartitionSession(StartPartitionSessionEvent event);

    void onStopPartitionSession(StopPartitionSessionEvent event);

    void onPartitionSessionStatus(PartitionSessionStatusEvent event);

    void onPartitionSessionClosed(PartitionSessionClosedEvent event);

    void onReaderClosed(ReaderClosedEvent event);

    void onError(Throwable throwable);
}
