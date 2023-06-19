package tech.ydb.topic.read.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public interface DataReceivedEvent {

    List<Message> getMessages();

    PartitionSession getPartitionSession();

    CompletableFuture<Void> commit();

}
