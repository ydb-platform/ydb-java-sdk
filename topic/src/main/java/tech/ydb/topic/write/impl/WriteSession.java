package tech.ydb.topic.write.impl;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.impl.TopicRetryableStream;
import tech.ydb.topic.impl.TopicStream;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public final class WriteSession extends TopicRetryableStream<FromServer, FromClient> {
    interface Stream extends TopicStream<FromServer, FromClient> { }

    private static final Logger logger = LoggerFactory.getLogger(WriteSession.class);

    public interface Listener {
        void onAck(WriteAck ack);

        void onStart(long lastSeqNo, String sessionId);
        void onStop(Status status);

        void onClose(Status status);
    }

    private final Listener listener;
    private final StreamFactory streamFactory;
    private final MessageSender sender;
    private final BiConsumer<Status, Throwable> errorsHandler;

    public WriteSession(String debugId, TopicRpc rpc, WriterSettings settings, Listener controller) {
        super(logger, debugId, settings.getRetryConfig(), rpc.getScheduler());
        this.listener = controller;
        this.streamFactory = new StreamFactory(rpc, settings);
        this.sender = new MessageSender(debugId, settings.getCodec(), this::send);
        this.errorsHandler = settings.getErrorsHandler();
    }

    @Override
    protected Stream createNewStream(String id) {
        return streamFactory.createNewStream(id);
    }

    @Override
    protected FromClient getInitRequest() {
        return streamFactory.initRequest();
    }

    public void sendAll(Supplier<SentMessage> generator) {
        SentMessage next = generator.get();
        while (next != null) {
            sender.sendMessage(next);
            next = generator.get();
        }
        sender.flush();
    }

    private void onInitResponse(YdbTopic.StreamWriteMessage.InitResponse response) {
        long lastSeqNo = response.getLastSeqNo();
        String sessionId = response.getSessionId();
        resetRetries();
        logger.info("[{}] Session with id {} (partition {}) initialized for topic \"{}\", lastSeqNo {}",
                debugId, sessionId, response.getPartitionId(), streamFactory.topicPath, lastSeqNo);
        listener.onStart(lastSeqNo, sessionId);
    }

    // Shouldn't be called more than once at a time due to grpc guarantees
    private void onWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse response) {
        List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks = response.getAcksList();
        logger.debug("[{}] Received WriteResponse with {} WriteAcks", debugId, acks.size());
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
            listener.onAck(mapAck(statistics, ack));
        }
    }

    WriteAck mapAck(WriteAck.Statistics statistics, YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack) {
        logger.trace("[{}] Received WriteAck with seqNo {} and status {}", debugId, ack.getSeqNo(),
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

    @Override
    public void onRetry(Status status) {
        logger.warn("[{}] Session onRetry with status {} called", debugId, status);
        listener.onStop(status);
        if (errorsHandler != null) {
            errorsHandler.accept(status, null);
        }
    }

    @Override
    public void onClose(Status status) {
        logger.info("[{}] Session closed with status {}", debugId, status);
        listener.onClose(status);
        if (errorsHandler != null && !status.isSuccess()) {
            errorsHandler.accept(status, null);
        }
    }

    @Override
    public void onNext(YdbTopic.StreamWriteMessage.FromServer message) {
        if (message.hasInitResponse()) {
            onInitResponse(message.getInitResponse());
        } else if (message.hasWriteResponse()) {
            onWriteResponse(message.getWriteResponse());
        } else if (message.hasUpdateTokenResponse()) {
            logger.debug("[{}] got update token response", debugId);
        } else {
            logger.warn("[{}] got unknown type message", debugId);
        }
    }

    private static class StreamFactory {
        private final String topicPath;
        private final TopicRpc rpc;
        private final YdbTopic.StreamWriteMessage.InitRequest initRequest;

        StreamFactory(TopicRpc rpc, WriterSettings settings) {
            this.rpc = rpc;
            this.topicPath = settings.getTopicPath();

            YdbTopic.StreamWriteMessage.InitRequest.Builder req = YdbTopic.StreamWriteMessage.InitRequest
                    .newBuilder()
                    .setPath(topicPath);
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

            this.initRequest = req.build();
        }

        public Stream createNewStream(String id) {
            return new WriteStream(id, rpc);
        }

        public YdbTopic.StreamWriteMessage.FromClient initRequest() {
            return YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                    .setInitRequest(initRequest)
                    .build();
        }
    }
}
