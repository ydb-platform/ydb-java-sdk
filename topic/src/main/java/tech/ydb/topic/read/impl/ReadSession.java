package tech.ydb.topic.read.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.impl.SessionBase;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReadSession extends SessionBase<FromServer, FromClient> {
    private static final Logger logger = LoggerFactory.getLogger(ReadSession.class);

    public ReadSession(TopicRpc rpc, String streamId) {
        super(rpc.readSession(streamId), streamId);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void sendUpdateTokenRequest(String token) {
        streamConnection.sendNext(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setUpdateTokenRequest(YdbTopic.UpdateTokenRequest.newBuilder()
                        .setToken(token)
                        .build())
                .build()
        );
    }

}
