package tech.ydb.topic.write.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.topic.YdbTopic.StreamWriteMessage.FromServer;

/**
 * @author Nikolay Perfilov
 */
public class WriteSession {
    private static final Logger logger = LoggerFactory.getLogger(WriteSession.class);

    private final GrpcReadWriteStream<FromServer, FromClient> streamConnection;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private String token;

    public WriteSession(TopicRpc rpc) {
        this.streamConnection = rpc.writeSession();
        this.token = streamConnection.authToken();
    }

    public synchronized CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> streamObserver) {
        logger.debug("WriteSession start");
        return streamConnection.start(message -> {
            if (logger.isTraceEnabled()) {
                logger.debug("ServerResponseObserver - onNext: {}", message);
            }

            if (isWorking.get()) {
                streamObserver.onNext(message);
            }
        });
    }

    public synchronized void send(FromClient request) {
        String currentToken = streamConnection.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.debug("WriteSession send new token: \n{}", request);
            streamConnection.sendNext(FromClient.newBuilder()
                .setUpdateTokenRequest(YdbTopic.UpdateTokenRequest.newBuilder()
                        .setToken(token)
                        .build())
                .build()
            );
        }

        logger.debug("WriteSession request: \n{}", request);
        streamConnection.sendNext(request);
    }

    public synchronized void finish() {
        if (isWorking.compareAndSet(true, false)) {
            return;
        }

        logger.debug("WriteSession finish");
        streamConnection.close();
    }
}
