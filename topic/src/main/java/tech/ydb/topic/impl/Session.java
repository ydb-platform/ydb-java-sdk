package tech.ydb.topic.impl;

/**
 * @author Nikolay Perfilov
 */
public interface Session {
    boolean stop();
    void shutdown();
}
