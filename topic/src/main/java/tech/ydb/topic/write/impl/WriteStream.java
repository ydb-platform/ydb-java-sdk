package tech.ydb.topic.write.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.proto.topic.YdbTopic.UpdateTokenRequest;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.impl.TopicStream;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStream extends TopicStream<FromServer, FromClient> {
    private static final Logger logger = LoggerFactory.getLogger(WriteStream.class);

    public WriteStream(String id, TopicRpc rpc) {
        super(logger, id, rpc.writeSession(id));
    }

    @Override
    protected FromClient updateTokenMessage(String token) {
        return FromClient.newBuilder().setUpdateTokenRequest(
                UpdateTokenRequest.newBuilder().setToken(token).build()
        ).build();
    }

    @Override
    protected Status parseMessageStatus(FromServer message) {
        return Status.of(StatusCode.fromProto(message.getStatus()), Issue.fromPb(message.getIssuesList()));
    }
}
