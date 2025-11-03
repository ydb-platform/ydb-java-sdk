package tech.ydb.topic.write.impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.impl.SessionBase;

/**
 * @author Nikolay Perfilov
 */
public abstract class WriteSession extends SessionBase<FromServer, FromClient> {
    private static final Logger logger = LoggerFactory.getLogger(WriteSession.class);

    public WriteSession(TopicRpc rpc, String streamId) {
        super(rpc.writeSession(streamId), streamId);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void sendUpdateTokenRequest(String token) {
        streamConnection.sendNext(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setUpdateTokenRequest(YdbTopic.UpdateTokenRequest.newBuilder()
                        .setToken(token)
                        .build())
                .build()
        );
    }
}
