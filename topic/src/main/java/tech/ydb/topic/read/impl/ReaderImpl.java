package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.TopicRetryableStream;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public class ReaderImpl extends TopicRetryableStream<FromServer, FromClient, ReadSession> {
    public interface Releaser {
        void releaseRange(PartitionSession partition, OffsetsRange range);
    }
    public interface Handler {
        void handleSessionStarted(String sessionId);

        void handleStartPartitionSessionRequest(StartPartitionSessionEvent event);
        void handleStopPartitionSession(StopPartitionSessionEvent event);
        void handleClosePartitionSession(PartitionSession partition);

        void handleDataReceivedEvent(Releaser releaser, DataReceivedEvent event);
        void handleCommitResponse(long committedOffset, PartitionSession partition);

        void handleReaderClosed(Status status);
    }

    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private final TopicRpc rpc;
    private final ReadConfig config;
    private final Handler handler;

    private final FromClient initRequest;

    public ReaderImpl(TopicRpc rpc, String id, ReaderSettings settings, ReadConfig config, Handler handler) {
        super(logger, id, settings.getRetryConfig(), rpc.getScheduler());
        this.rpc = rpc;
        this.initRequest = FromClient.newBuilder().setInitRequest(buildInitRequest(settings)).build();
        this.config = config;
        this.handler = handler;
    }

    @Override
    protected ReadSession createNewStream(String id) {
        return new ReadSession(id, rpc.readSession(id), initRequest, handler::handleDataReceivedEvent, config);
    }

    @Override
    protected void onRetry(ReadSession stream, Status status) {
        logger.warn("[{}] paused by status {}", debugId, status);
        stream.closeAll().forEach(ps -> handler.handleClosePartitionSession(ps));
    }

    @Override
    protected void onClose(ReadSession stream, Status status) {
        if (!status.isSuccess()) {
            logger.warn("[{}] closed by status {}", debugId, status);
        } else {
            logger.info("[{}] closed by status {}", debugId, status);
        }
        stream.closeAll().forEach(ps -> handler.handleClosePartitionSession(ps));
        handler.handleReaderClosed(status);
    }

    @Override
    protected void onNext(ReadSession stream, FromServer message) {
        logger.trace("[{}] processMessage called", debugId);

        if (message.hasInitResponse()) {
            resetRetries();
            handler.handleSessionStarted(message.getInitResponse().getSessionId());
            stream.onInit(message.getInitResponse());
        } else if (message.hasStartPartitionSessionRequest()) {
            StartPartitionSessionEvent event = stream.onStartPartition(message.getStartPartitionSessionRequest());
            handler.handleStartPartitionSessionRequest(event);
        } else if (message.hasStopPartitionSessionRequest()) {
            YdbTopic.StreamReadMessage.StopPartitionSessionRequest req = message.getStopPartitionSessionRequest();
            if (req.getGraceful()) {
                StopPartitionSessionEvent event = stream.onStopPartition(req);
                if (event != null) {
                    handler.handleStopPartitionSession(event);
                }
            } else {
                PartitionSession closed = stream.onClosePartition(req.getPartitionSessionId());
                if (closed != null) {
                    handler.handleClosePartitionSession(closed);
                }
            }
        } else if (message.hasReadResponse()) {
            stream.onRead(message.getReadResponse());
        } else if (message.hasCommitOffsetResponse()) {
            stream.onCommitOffset(message.getCommitOffsetResponse(), handler::handleCommitResponse);
        } else if (message.hasPartitionSessionStatusResponse()) {
            stream.onPartitionSessionStatus(message.getPartitionSessionStatusResponse());
        } else if (message.hasUpdateTokenResponse()) {
            logger.debug("[{}] Received UpdateTokenResponse", debugId);
        } else {
            logger.error("[{}] Unhandled message from server: {}", debugId, message);
        }
    }

    public CompletableFuture<Status> updateOffsetsInTransaction(YdbTransaction transaction,
            Map<String, List<PartitionOffsets>> offsets,
            UpdateOffsetsInTransactionSettings settings) {
        if (!transaction.isActive()) {
            throw new IllegalArgumentException("Transaction is not active. " +
                    "Can only read topic messages in already running transactions from other services");
        }
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException("Empty topic list to update in transaction");
        }
        for (List<PartitionOffsets> offset: offsets.values()) {
            if (offset.isEmpty()) {
                throw new IllegalArgumentException("Empty offsets range to update in transaction");
            }
        }

        if (logger.isDebugEnabled()) {
            StringBuilder str = new StringBuilder("Updating ");
            boolean first = true;
            for (Map.Entry<String, List<PartitionOffsets>> topicOffsets : offsets.entrySet()) {
                for (PartitionOffsets partitionOffsets : topicOffsets.getValue()) {
                    if (!first) {
                        str.append(", ");
                    } else {
                        first = false;
                    }
                    str.append("offsets [").append(partitionOffsets.getOffsets().get(0).getStart()).append("..")
                            .append(partitionOffsets.getOffsets().get(partitionOffsets.getOffsets().size() - 1)
                                    .getEnd()).append(") for partition ")
                            .append(partitionOffsets.getPartitionSession().getPartitionId())
                            .append(" [topic ").append(topicOffsets.getKey()).append("]");
                }
            }
            logger.debug(str.toString());
        }

        transaction.getStatusFuture().whenComplete((status, error) -> {
            if (status != null && !status.isSuccess()) {
                String msg = "Restarting read session due to transaction " + transaction.getId() +
                                " with partition offsets from read session " + debugId +
                                " was not committed with status: " + status;
                fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, Issue.of(msg, Issue.Severity.ERROR)));
            }
            if (error != null) {
                String msg = "Restarting read session due to transaction " + transaction.getId() +
                                " with partition offsets from read session " + debugId +
                                " was not committed with reason: " + error.getMessage();
                fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, error, Issue.of(msg, Issue.Severity.ERROR)));
            }
        });

        YdbTopic.UpdateOffsetsInTransactionRequest req = YdbTopic.UpdateOffsetsInTransactionRequest.newBuilder()
                .setTx(YdbTopic.TransactionIdentity.newBuilder()
                        .setId(transaction.getId())
                        .setSession(transaction.getSessionId())
                        .build())
                .setConsumer(config.getConsumerName())
                .addAllTopics(offsets.entrySet().stream()
                        .map(entry -> buildTopicOffsets(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList()))
                .build();

        String traceId = settings.getTraceId() == null ? UUID.randomUUID().toString() : settings.getTraceId();
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withTraceId(traceId)
                .build();

        return rpc.updateOffsetsInTransaction(req, grpcRequestSettings);
    }

    private static YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.PartitionOffsets buildPartitionOffsets(
            PartitionOffsets partitionOffsets) {
        return YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.PartitionOffsets.newBuilder()
                .setPartitionId(partitionOffsets.getPartitionSession().getPartitionId())
                .addAllPartitionOffsets(partitionOffsets.getOffsets().stream()
                        .map(ReaderImpl::buildOffsetRange)
                        .collect(Collectors.toList()))
                .build();
    }

    private static YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets buildTopicOffsets(String topicPath,
            List<PartitionOffsets> partitions) {

        return YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.newBuilder()
                .setPath(topicPath)
                .addAllPartitions(partitions.stream()
                        .map(ReaderImpl::buildPartitionOffsets)
                        .collect(Collectors.toList()))
                .build();
    }

    public static YdbTopic.OffsetsRange buildOffsetRange(OffsetsRange range) {
        return YdbTopic.OffsetsRange.newBuilder()
                .setStart(range.getStart())
                .setEnd(range.getEnd())
                .build();
    }

    private static YdbTopic.StreamReadMessage.InitRequest.TopicReadSettings buildTopicSettings(TopicReadSettings trs) {
        String topicPath = trs.getPath();
        List<Long> partitions = trs.getPartitionIds();
        Instant readFrom = trs.getReadFrom();
        Duration maxLag = trs.getMaxLag();

        YdbTopic.StreamReadMessage.InitRequest.TopicReadSettings.Builder builder = YdbTopic.StreamReadMessage
                .InitRequest.TopicReadSettings.newBuilder();

        builder.setPath(topicPath);
        if (partitions != null && !partitions.isEmpty()) {
            builder.addAllPartitionIds(partitions);
        }
        if (readFrom != null) {
            builder.setReadFrom(ProtobufUtils.instantToProto(readFrom));
        }
        if (maxLag != null) {
            builder.setMaxLag(ProtobufUtils.durationToProto(maxLag));
        }

        return builder.build();
    }

    private static YdbTopic.StreamReadMessage.InitRequest buildInitRequest(ReaderSettings settings) {
        String consumerName = settings.getConsumerName();
        String readerName = settings.getReaderName();
        List<TopicReadSettings> topics = settings.getTopics();

        YdbTopic.StreamReadMessage.InitRequest.Builder builder = YdbTopic.StreamReadMessage.InitRequest.newBuilder();

        builder.setPartitionMaxInFlightBytes(settings.getPartitionMaxInFlightBytes());
        if (consumerName != null && !consumerName.isEmpty()) {
            builder.setConsumer(consumerName);
        }
        if (readerName != null && !readerName.isEmpty()) {
            builder.setReaderName(readerName);
        }
        for (TopicReadSettings trs: topics) {
            builder.addTopicsReadSettings(buildTopicSettings(trs));
        }

        return builder.build();
    }
}
