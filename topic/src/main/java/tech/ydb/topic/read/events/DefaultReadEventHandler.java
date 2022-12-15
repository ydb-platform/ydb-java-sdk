package tech.ydb.topic.read.events;

/**
 * @author Nikolay Perfilov
 */
public abstract class DefaultReadEventHandler implements ReadEventHandler {

    // onMessages method should be defined in user's implementation

    @Override
    public void onCommitOffsetAcknowledgement(CommitOffsetAcknowledgementEvent event) {

    }

    @Override
    public void onStartPartitionSession(StartPartitionSessionEvent event) {
        event.confirm();
    }

    @Override
    public void onStopPartitionSession(StopPartitionSessionEvent event) {
        event.confirm();
    }

    @Override
    public void onPartitionSessionStatus(PartitionSessionStatusEvent event) {

    }

    @Override
    public void onPartitionSessionClosed(PartitionSessionClosedEvent event) {

    }

    @Override
    public void onSessionClosed(SessionClosedEvent event) {

    }

    @Override
    public void onError(Throwable throwable) {

    }
}
