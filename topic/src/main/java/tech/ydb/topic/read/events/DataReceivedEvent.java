package tech.ydb.topic.read.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.read.Message;

/**
 * @author Nikolay Perfilov
 */
public interface DataReceivedEvent {

    List<Message> getMessages();

    CompletableFuture<Void> commit();

}
