package tech.ydb.topic.read.impl.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;

/**
 * @author Nikolay Perfilov
 */
public class DataReceivedEventImpl implements DataReceivedEvent {
    private static final Logger logger = LoggerFactory.getLogger(DataReceivedEventImpl.class);
    private final List<Message> messages;
    private final PartitionSession partitionSession;
    private final Supplier<CompletableFuture<Void>> commitCallback;

    public DataReceivedEventImpl(List<Message> messages, PartitionSession partitionSession,
                                 Supplier<CompletableFuture<Void>> commitCallback) {
        this.messages = messages;
        this.partitionSession = partitionSession;
        this.commitCallback = commitCallback;
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    @Override
    public CompletableFuture<Void> commit() {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] partition session {} (partition {}): committing batch with {} message(s) and offsets" +
                            " {}-{}", partitionSession.getPath(), partitionSession.getId(),
                    partitionSession.getPartitionId(), messages.size(), messages.get(0).getOffset(),
                    messages.get(messages.size() - 1).getOffset());
        }
        return commitCallback.get();
    }
}
