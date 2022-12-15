package tech.ydb.topic.read.events;

import java.util.List;

import tech.ydb.topic.read.Message;

/**
 * @author Nikolay Perfilov
 */
public interface DataReceivedEvent {

    List<Message> getMessages();

    void commit();

}
