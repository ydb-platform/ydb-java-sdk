package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.topic.YdbTopic.StreamWriteMessage.FromServer;

/**
 * @author Nikolay Perfilov
 */
public class WriteSession {
    private static final Logger logger = LoggerFactory.getLogger(WriteSession.class);

    private final GrpcReadWriteStream<FromServer, FromClient> streamConnection;

    public WriteSession(TopicRpc rpc) {
        this.streamConnection = rpc.writeSession();
    }

    public CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> streamObserver) {
        logger.debug("WriteSession start");
        return streamConnection.start(streamObserver);
    }

    public void send(FromClient request) {
        logger.debug("WriteSession request: \n{}", request);
        streamConnection.sendNext(request);
    }

    public void finish() {
        logger.debug("WriteSession finish");
        streamConnection.close();
    }

}
