package tech.ydb.topic.read.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;
import tech.ydb.topic.TopicRpc;

/**
 * @author Nikolay Perfilov
 */
public class ReadSession {
    private static final Logger logger = LoggerFactory.getLogger(ReadSession.class);

    private final GrpcReadWriteStream<FromServer, FromClient> streamConnection;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private String token;

    public ReadSession(TopicRpc rpc) {
        this.streamConnection = rpc.readSession();
        this.token = streamConnection.authToken();
    }

    public synchronized CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> streamObserver) {
        logger.info("ReadSession start");
        return streamConnection.start(message -> {
            if (logger.isTraceEnabled()) {
                logger.trace("Message received:\n{}", message);
            } else {
                logger.debug("Message received");
            }

            if (isWorking.get()) {
                streamObserver.onNext(message);
            }
        });
    }

    public synchronized void send(FromClient request) {
        if (!isWorking.get()) {
            if (logger.isTraceEnabled()) {
                logger.trace("ReadSession is already closed. This message is NOT sent:\n{}", request);
            }
            return;
        }
        String currentToken = streamConnection.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.info("Sending new token");
            streamConnection.sendNext(FromClient.newBuilder()
                    .setUpdateTokenRequest(YdbTopic.UpdateTokenRequest.newBuilder()
                            .setToken(token)
                            .build())
                    .build()
            );
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Sending request:\n{}", request);
        } else {
            logger.debug("Sending request");
        }
        streamConnection.sendNext(request);
    }

    public boolean stop() {
        logger.info("ReadSession stop");
        return isWorking.compareAndSet(true, false);
    }

    public synchronized void shutdown() {
        logger.info("ReadSession shutdown");
        if (!stop()) {
            return;
        }
        streamConnection.close();
    }

}
