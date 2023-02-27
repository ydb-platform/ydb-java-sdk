package tech.ydb.topic.write.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.rpc.OutStreamObserver;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;

/**
 * @author Nikolay Perfilov
 */
public class WriteSession {
    private static final Logger logger = LoggerFactory.getLogger(WriteSession.class);

    private final OutStreamObserver<YdbTopic.StreamWriteMessage.FromClient> streamConnection;

    public WriteSession(TopicRpc rpc, StreamObserver<YdbTopic.StreamWriteMessage.FromServer> streamObserver) {
        this.streamConnection = rpc.writeSession(streamObserver);
    }

    public void send(YdbTopic.StreamWriteMessage.FromClient request) {
        logger.debug("WriteSession request: \n{}", request);
        streamConnection.onNext(request);
    }

    public void finish() {
        logger.debug("WriteSession finish");
        streamConnection.onCompleted();
    }

}
