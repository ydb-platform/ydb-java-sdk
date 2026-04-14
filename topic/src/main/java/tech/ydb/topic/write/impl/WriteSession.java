package tech.ydb.topic.write.impl;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.impl.SessionBase;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public final class WriteSession extends SessionBase<FromServer, FromClient> {
    private static final Logger logger = LoggerFactory.getLogger(WriteSession.class);

    private final WriterImpl writer;
    private final MessageSender sender;
    private final YdbTopic.StreamWriteMessage.InitRequest initRequest;

    private volatile String sessionId = null;
    private volatile Status finishStatus = null;

    public WriteSession(WriterImpl writer, TopicRpc rpc, String streamId, WriterSettings settings) {
        super(rpc.writeSession(streamId), streamId);
        this.writer = writer;
        this.initRequest = buildInitRequest(settings);
        this.sender = new MessageSender(settings.getCodec(), this::safeSend);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public boolean isStarted() {
        return sessionId != null;
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

    private void safeSend(YdbTopic.StreamWriteMessage.FromClient msg) {
        if (finishStatus == null) {
            send(msg);
        }
    }

    public void sendAll(Supplier<SentMessage> generator) {
        SentMessage next = generator.get();
        while (next != null) {
            sender.sendMessage(next);
            next = generator.get();
        }
        sender.flush();
    }

    @Override
    public void startAndInitialize() {
        logger.debug("[{}] Session startAndInitialize called", streamId);
        start(this::processMessage).whenComplete(this::closeDueToError);
        safeSend(YdbTopic.StreamWriteMessage.FromClient.newBuilder().setInitRequest(initRequest).build());
    }

    private void onInitResponse(YdbTopic.StreamWriteMessage.InitResponse response) {
        long lastSeqNo = response.getLastSeqNo();
        writer.onInit(lastSeqNo);
        sessionId = response.getSessionId();
        logger.info("[{}] Session with id {} (partition {}) initialized for topic \"{}\", lastSeqNo {}",
                streamId, sessionId, response.getPartitionId(), initRequest.getPath(), lastSeqNo);
        writer.onStart(lastSeqNo);
    }

    // Shouldn't be called more than once at a time due to grpc guarantees
    private void onWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse response) {
        List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks = response.getAcksList();
        logger.debug("[{}] Received WriteResponse with {} WriteAcks", streamId, acks.size());
        WriteAck.Statistics statistics = null;
        if (response.getWriteStatistics() != null) {
            YdbTopic.StreamWriteMessage.WriteResponse.WriteStatistics src = response.getWriteStatistics();
            statistics = new WriteAck.Statistics(
                ProtobufUtils.protoToDuration(src.getPersistingTime()),
                ProtobufUtils.protoToDuration(src.getPartitionQuotaWaitTime()),
                ProtobufUtils.protoToDuration(src.getTopicQuotaWaitTime()),
                ProtobufUtils.protoToDuration(src.getMaxQueueWaitTime()),
                ProtobufUtils.protoToDuration(src.getMinQueueWaitTime())
            );
        }

        for (YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack : acks) {
            writer.onAck(mapAck(statistics, ack));
        }
    }

    private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
        if (message.getStatus() != StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            Status status = Status.of(StatusCode.fromProto(message.getStatus()),
                    Issue.fromPb(message.getIssuesList()));
            logger.warn("[{}] Got non-success status in processMessage method: {}", streamId, status);
            closeDueToError(status, null);
            return;
        }
        if (message.hasInitResponse()) {
            onInitResponse(message.getInitResponse());
        } else if (message.hasWriteResponse()) {
            onWriteResponse(message.getWriteResponse());
        } else if (message.hasUpdateTokenResponse()) {
            logger.debug("[{}] got update token response", streamId);
        } else {
            logger.warn("[{}] got unknown type message", streamId);
        }
    }

    WriteAck mapAck(WriteAck.Statistics statistics, YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack) {
        logger.debug("[{}] Received WriteAck with seqNo {} and status {}", streamId, ack.getSeqNo(),
                ack.getMessageWriteStatusCase());
        if (ack.hasSkipped()) {
            return new WriteAck(ack.getSeqNo(), WriteAck.State.ALREADY_WRITTEN, null, statistics);
        }
        if (ack.hasWrittenInTx()) {
            return new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN_IN_TX, null, statistics);
        }
        if (ack.hasWritten()) {
            WriteAck.Details details = new WriteAck.Details(ack.getWritten().getOffset());
            return new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN, details, statistics);
        }

        // Unknown type of write ack
        return new WriteAck(ack.getSeqNo(), null, null, statistics);
    }

    private void closeDueToError(Status status, Throwable th) {
        finishStatus = status != null ? status : Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
        logger.info("[{}] Session {} closeDueToError called", streamId, sessionId);
        if (shutdown()) {
            // Signal writer to retry
            writer.onSessionClosed(status, th);
        }
    }

    @Override
    protected void onStop() {
        logger.debug("[{}] Session {} onStop called", streamId, sessionId);
    }

    private static YdbTopic.StreamWriteMessage.InitRequest buildInitRequest(WriterSettings settings) {
        YdbTopic.StreamWriteMessage.InitRequest.Builder req = YdbTopic.StreamWriteMessage.InitRequest
                .newBuilder()
                .setPath(settings.getTopicPath());
        String producerId = settings.getProducerId();
        if (producerId != null) {
            req.setProducerId(producerId);
        }
        String messageGroupId = settings.getMessageGroupId();
        Long partitionId = settings.getPartitionId();
        if (messageGroupId != null) {
            if (partitionId != null) {
                throw new IllegalArgumentException("Both MessageGroupId and PartitionId are set in WriterSettings");
            }
            req.setMessageGroupId(messageGroupId);
        } else if (partitionId != null) {
            req.setPartitionId(partitionId);
        }

        return req.build();
    }
}
