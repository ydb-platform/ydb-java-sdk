package tech.ydb.topic.read.events;

/**
 * @author Nikolay Perfilov
 */
public interface ReadEventHandler {

    void onMessages(DataReceivedEvent event);
    void onCommitResponse(CommitOffsetAcknowledgementEvent event);

    void onStartPartitionSession(StartPartitionSessionEvent event);

    void onStopPartitionSession(StopPartitionSessionEvent event);

    void onPartitionSessionClosed(PartitionSessionClosedEvent event);

    void onReaderClosed(ReaderClosedEvent event);
}
