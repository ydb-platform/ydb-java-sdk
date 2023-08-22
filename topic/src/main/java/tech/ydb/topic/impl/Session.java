package tech.ydb.topic.impl;

public interface Session {
    boolean stop();
    void shutdown();
}
