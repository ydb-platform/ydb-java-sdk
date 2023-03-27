package tech.ydb.topic.read.events;

/**
 * @author Nikolay Perfilov
 */
public abstract class AbstractReadEventHandler implements ReadEventHandler {

    // onMessages(DataReceivedEvent event) method should be defined in user's implementation

    @Override
    public void onStartPartitionSession(StartPartitionSessionEvent event) {
        event.confirm();
    }

    @Override
    public void onStopPartitionSession(StopPartitionSessionEvent event) {
        event.confirm();
    }

    @Override
    public void onPartitionSessionClosed(PartitionSessionClosedEvent event) {

    }

    @Override
    public void onReaderClosed(ReaderClosedEvent event) {

    }
}
