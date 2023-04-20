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
                logger.trace("WriteSession is already closed. This message is NOT sent:\n{}", request);
            } else if (logger.isDebugEnabled()) {
                logger.debug("WriteSession is already closed. A message of {} bytes was not sent",
                        request.getSerializedSize());
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
        } else if (logger.isDebugEnabled()) {
            logger.debug("Sending request of {} bytes", request.getSerializedSize());
        }
        streamConnection.sendNext(request);
    }

    public boolean stop() {
        logger.info("WriteSession stop");
        return isWorking.compareAndSet(true, false);
    }

    public synchronized void shutdown() {
        logger.info("WriteSession shutdown");
        if (!stop()) {
            return;
        }
        streamConnection.close();
    }
}
