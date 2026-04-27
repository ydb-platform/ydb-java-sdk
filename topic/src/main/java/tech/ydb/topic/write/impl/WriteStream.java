package tech.ydb.topic.write.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.proto.topic.YdbTopic.UpdateTokenRequest;
import tech.ydb.topic.impl.TopicStreamBase;
import tech.ydb.topic.impl.TopicStreamFail;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStream extends TopicStreamBase<FromServer, FromClient> implements WriteSession.Stream {
    private static final Logger logger = LoggerFactory.getLogger(WriteStream.class);

    public WriteStream(String id, GrpcReadWriteStream<FromServer, FromClient> stream) {
        super(logger, id, stream);
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

    public static class Fail extends TopicStreamFail<FromServer, FromClient> implements WriteSession.Stream {
        public Fail(String id, Status status) {
            super(logger, id, status);
        }
    }
}
