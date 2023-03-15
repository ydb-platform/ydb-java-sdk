package tech.ydb.topic.read.impl;

import java.time.Duration;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;

/**
 * @author Nikolay Perfilov
 */
public class SyncReaderImpl implements SyncReader {

    @Override
    public void init() { }

    @Override
    public void initAndWait() { }

    @Override
    public Message receive(Duration timeout) {
        throw new NotImplementedException();
    }

    @Override
    public Message receive() {
        return receive(Duration.ZERO);
    }

    @Override
    public void shutdown() {

    }
}
