package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.SessionBase;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.StartPartitionSessionEventImpl;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public final class ReadSession extends SessionBase<YdbTopic.StreamReadMessage.FromServer,
        YdbTopic.StreamReadMessage.FromClient> {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private final TopicRpc rpc;
    private final ReaderImpl reader;

    private final String consumerName;
    private final YdbTopic.StreamReadMessage.InitRequest initRequest;

    private final long maxMemoryUsageBytes;
    private final int maxBatchSize;
    private final MessageDecoder decoder;

    // Total size to request with next ReadRequest.
    // Used to group several ReadResponses in one on high rps
    private final AtomicLong sizeBytesToRequest = new AtomicLong(0);
    private final Map<Long, PartitionSessionImpl> partitionSessions = new ConcurrentHashMap<>();

    public ReadSession(TopicRpc rpc, ReaderImpl reader, MessageDecoder decoder, String id, ReaderSettings settings) {
        super(rpc.readSession(id), id);
        this.reader = reader;
        this.rpc = rpc;
        this.decoder = decoder;

        this.consumerName = settings.getConsumerName();
        this.maxMemoryUsageBytes = settings.getMaxMemoryUsageBytes();
        this.maxBatchSize = settings.getMaxBatchSize();
        this.initRequest = buildInitRequest(settings);
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

    @Override
    public void startAndInitialize() {
        logger.debug("[{}] Session startAndInitialize called", streamId);
        start(this::processMessage).whenComplete(this::closeDueToError);


        send(YdbTopic.StreamReadMessage.FromClient.newBuilder().setInitRequest(initRequest).build());
    }

    private void sendReadRequest() {
        long currentSizeBytesToRequest = sizeBytesToRequest.getAndSet(0);
        if (currentSizeBytesToRequest <= 0) {
            logger.debug("[{}] Nothing to request in DataRequest. sizeBytesToRequest == {}", streamId,
                    currentSizeBytesToRequest);
            return;
        }
        logger.debug("[{}] Sending DataRequest with {} bytes", streamId, currentSizeBytesToRequest);

        send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setReadRequest(YdbTopic.StreamReadMessage.ReadRequest.newBuilder()
                        .setBytesSize(currentSizeBytesToRequest)
                        .build())
                .build());
    }

    private void sendStopPartitionSessionResponse(long partitionSessionId) {
        if (!isWorking.get()) {
            logger.info("[{}] Need to send StopPartitionSessionResponse for partition session {}, " +
                    "but reading session is already closed", streamId, partitionSessionId);
            return;
        }
        PartitionSessionImpl partitionSession = partitionSessions.remove(partitionSessionId);
        if (partitionSession != null) {
            partitionSession.shutdown();
            logger.info("[{}] Sending StopPartitionSessionResponse", partitionSession.getFullId());
        } else {
            logger.warn("[{}] Sending StopPartitionSessionResponse for partition session {}, " +
                    "but have no such partition session active", streamId, partitionSessionId);
        }
        send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setStopPartitionSessionResponse(
                        YdbTopic.StreamReadMessage.StopPartitionSessionResponse.newBuilder()
                                .setPartitionSessionId(partitionSessionId)
                                .build())
                .build());
    }

    private void sendCommitOffsetRequest(PartitionSession session, List<OffsetsRange> rangesToCommit) {
        if (!isWorking.get()) {
            if (logger.isInfoEnabled()) {
                StringBuilder message = new StringBuilder("[").append(streamId)
                        .append("] Need to send CommitRequest for ")
                        .append(session.toString())
                        .append(" with offset ranges ");
                for (int i = 0; i < rangesToCommit.size(); i++) {
                    if (i > 0) {
                        message.append(", ");
                    }
                    OffsetsRange range = rangesToCommit.get(i);
                    message.append("[").append(range.getStart()).append(",").append(range.getEnd()).append(")");
                }
                message.append(", but reading session is already closed");
                logger.info(message.toString());
            }
            return;
        }

        YdbTopic.StreamReadMessage.FromClient req = YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setCommitOffsetRequest(YdbTopic.StreamReadMessage.CommitOffsetRequest.newBuilder()
                        .addCommitOffsets(YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset
                                .newBuilder()
                                .setPartitionSessionId(session.getId())
                                .addAllOffsets(
                                        rangesToCommit.stream().map(r -> YdbTopic.OffsetsRange.newBuilder()
                                            .setStart(r.getStart())
                                            .setEnd(r.getEnd())
                                            .build()
                                        ).collect(Collectors.toList())
                                ).build())
                        .build())
                .build();
        send(req);
    }

    private void closePartitionSessions() {
        partitionSessions.values().forEach(this::closePartitionSession);
        partitionSessions.clear();
    }

    private void closePartitionSession(PartitionSessionImpl partitionSession) {
        partitionSession.shutdown();
        reader.handleClosePartitionSession(partitionSession.getSessionId());
    }

    private void onInitResponse(YdbTopic.StreamReadMessage.InitResponse response) {
        String sessionId = response.getSessionId();

        sizeBytesToRequest.set(maxMemoryUsageBytes);
        logger.info("[{}] Session {} initialized. Requesting {} bytes...", streamId, sessionId, maxMemoryUsageBytes);
        reader.onSessionStarted(sessionId);
        sendReadRequest();
    }

    private void onStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest req) {
        PartitionSession ps = new PartitionSession(
                req.getPartitionSession().getPartitionSessionId(),
                req.getPartitionSession().getPartitionId(),
                req.getPartitionSession().getPath()
        );
        long committedOffset = req.getCommittedOffset();
        OffsetsRange offsets = new OffsetsRangeImpl(
                req.getPartitionOffsets().getStart(),
                req.getPartitionOffsets().getEnd()
        );

        String debugId = streamId + '/' + ps.getId() + "-p" + ps.getPartitionId();
        logger.info("[{}] Received StartPartitionSessionRequest for {} and consumer \"{}\" with committedOffset {}"
                + " and partitionOffsets {}", debugId, ps, consumerName, committedOffset, offsets);

        reader.handleStartPartitionSessionRequest(new StartPartitionSessionEventImpl(ps, committedOffset, offsets) {
            @Override
            public void confirm(StartPartitionSessionSettings confrimOptions) {
                if (!isWorking.get()) {
                    logger.info("[{}] Need to send StartPartitionSessionResponse, but reading session is "
                            + "already closed", debugId);
                    return;
                }

                long readFrom = committedOffset;
                long commmitFrom = committedOffset;

                YdbTopic.StreamReadMessage.StartPartitionSessionResponse.Builder response =
                        YdbTopic.StreamReadMessage.StartPartitionSessionResponse.newBuilder()
                                .setPartitionSessionId(ps.getId());
                if (confrimOptions != null) {
                    if (confrimOptions.getReadOffset() != null) {
                        readFrom = confrimOptions.getReadOffset();
                        response = response.setReadOffset(readFrom);
                    }
                    if (confrimOptions.getCommitOffset() != null) {
                        commmitFrom = confrimOptions.getCommitOffset();
                        response = response.setCommitOffset(commmitFrom);
                    }
                }

                PartitionSessionImpl impl = new PartitionSessionImpl(ps, debugId, maxBatchSize, readFrom, commmitFrom,
                        decoder) {
                    @Override
                    public void commitRanges(List<OffsetsRange> offsets) {
                        sendCommitOffsetRequest(ps, offsets);
                    }

                    @Override
                    public CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
                        return reader.handleDataReceivedEvent(event);
                    }
                };
                partitionSessions.put(ps.getId(), impl);

                logger.info("[{}] Sending StartPartitionSessionResponse for {} and consumer \"{}\" with readOffset "
                        + "{} and commitOffset {}", debugId, ps, consumerName, readFrom, commmitFrom);

                send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                        .setStartPartitionSessionResponse(response.build())
                        .build());
            }
        });


    }

    protected void onStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        if (request.getGraceful()) {
            PartitionSessionImpl partitionSession = partitionSessions.get(request.getPartitionSessionId());
            if (partitionSession != null) {
                logger.info("[{}] Received graceful StopPartitionSessionRequest", partitionSession.getFullId());
                reader.handleStopPartitionSession(request, partitionSession.getSessionId(),
                        () -> sendStopPartitionSessionResponse(request.getPartitionSessionId()));
            } else {
                logger.error("[{}] Received graceful StopPartitionSessionRequest for partition session {}, " +
                        "but have no such partition session active", streamId, request.getPartitionSessionId());
                closeDueToError(null,
                        new RuntimeException("Restarting read session due to receiving " +
                                "StopPartitionSessionRequest with PartitionSessionId " +
                                request.getPartitionSessionId() + " that SDK knows nothing about"));
            }
        } else {
            PartitionSessionImpl partitionSession = partitionSessions.remove(request.getPartitionSessionId());
            if (partitionSession != null) {
                logger.info("[{}] Received force StopPartitionSessionRequest", partitionSession.getFullId());
                closePartitionSession(partitionSession);
            } else {
                logger.info("[{}] Received force StopPartitionSessionRequest for partition session {}, " +
                        "but have no such partition session running", streamId, request.getPartitionSessionId());
            }
        }
    }

    private void onReadResponse(YdbTopic.StreamReadMessage.ReadResponse readResponse) {
        final long responseBytesSize = readResponse.getBytesSize();
        logger.trace("[{}] Received ReadResponse of {} bytes", streamId, responseBytesSize);
        List<CompletableFuture<Void>> batchReadFutures = new ArrayList<>();
        readResponse.getPartitionDataList().forEach(
                (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data) -> {
                    long partitionId = data.getPartitionSessionId();
                    PartitionSessionImpl partitionSession = partitionSessions.get(partitionId);
                    if (partitionSession != null) {
                        // Completes when all messages from a batch are read by user
                        CompletableFuture<Void> readFuture = partitionSession.addBatches(data.getBatchesList());
                        batchReadFutures.add(readFuture);
                    } else {
                        logger.info("[{}] Received PartitionData for unknown(most likely already closed) " +
                                        "PartitionSessionId={}", streamId, partitionId);
                    }
                });
        CompletableFuture.allOf(batchReadFutures.toArray(new CompletableFuture<?>[0]))
                .whenComplete((res, th) -> {
                    if (th != null) {
                        logger.error("[{}] Exception while waiting for batches to be read:", streamId, th);
                    }
                    if (isWorking.get()) {
                        logger.trace("[{}] Finished handling ReadResponse of {} bytes. Sending ReadRequest...",
                                streamId, responseBytesSize);
                        this.sizeBytesToRequest.addAndGet(responseBytesSize);
                        sendReadRequest();
                    } else {
                        logger.trace("[{}] Finished handling ReadResponse of {} bytes. Read session is already " +
                                "closed -- no need to send ReadRequest", streamId, responseBytesSize);
                    }
                });
    }

    protected void onCommitOffsetResponse(YdbTopic.StreamReadMessage.CommitOffsetResponse response) {
        logger.trace("[{}] Received CommitOffsetResponse", streamId);
        for (YdbTopic.StreamReadMessage.CommitOffsetResponse.PartitionCommittedOffset partitionCommittedOffset :
                response.getPartitionsCommittedOffsetsList()) {
            PartitionSessionImpl partitionSession =
                    partitionSessions.get(partitionCommittedOffset.getPartitionSessionId());
            if (partitionSession != null) {
                // Handling CompletableFuture completions for single commits
                partitionSession.handleCommitResponse(partitionCommittedOffset.getCommittedOffset());
                // Handling onCommitResponse callback
                reader.handleCommitResponse(partitionCommittedOffset.getCommittedOffset(),
                        partitionSession.getSessionId());
            } else {
                logger.info("[{}] Received CommitOffsetResponse for unknown (most likely already closed) " +
                                "partition session with id={}", streamId,
                        partitionCommittedOffset.getPartitionSessionId());
            }
        }
    }

    protected void onPartitionSessionStatusResponse(
            YdbTopic.StreamReadMessage.PartitionSessionStatusResponse response) {
        PartitionSessionImpl partitionSession = partitionSessions.get(response.getPartitionSessionId());
        logger.info("[{}] Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                        " Partition offsets: [{}, {}). Committed offset: {}", streamId,
                response.getPartitionSessionId(),
                partitionSession == null ? "unknown" : partitionSession.getSessionId().getPartitionId(),
                response.getPartitionOffsets().getStart(), response.getPartitionOffsets().getEnd(),
                response.getCommittedOffset());
    }

    private void processMessage(YdbTopic.StreamReadMessage.FromServer message) {
        if (!isWorking.get()) {
            logger.debug("[{}] processMessage called, but read session is already closed", streamId);
            return;
        }
        logger.debug("[{}] processMessage called", streamId);
        if (message.getStatus() != StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            Status status = Status.of(StatusCode.fromProto(message.getStatus()),
                    Issue.fromPb(message.getIssuesList()));
            logger.warn("[{}] Got non-success status in processMessage method: {}", streamId, status);
            closeDueToError(status, null);
            return;
        }

        if (message.hasInitResponse()) {
            onInitResponse(message.getInitResponse());
        } else if (message.hasStartPartitionSessionRequest()) {
            onStartPartitionSessionRequest(message.getStartPartitionSessionRequest());
        } else if (message.hasStopPartitionSessionRequest()) {
            onStopPartitionSessionRequest(message.getStopPartitionSessionRequest());
        } else if (message.hasReadResponse()) {
            onReadResponse(message.getReadResponse());
        } else if (message.hasCommitOffsetResponse()) {
            onCommitOffsetResponse(message.getCommitOffsetResponse());
        } else if (message.hasPartitionSessionStatusResponse()) {
            onPartitionSessionStatusResponse(message.getPartitionSessionStatusResponse());
        } else if (message.hasUpdateTokenResponse()) {
            logger.debug("[{}] Received UpdateTokenResponse", streamId);
        } else {
            logger.error("[{}] Unhandled message from server: {}", streamId, message);
        }
    }

    public CompletableFuture<Status> sendUpdateOffsetsInTransaction(YdbTransaction transaction,
                                                                       Map<String, List<PartitionOffsets>> offsets,
                                                                       UpdateOffsetsInTransactionSettings settings) {
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException("Empty topic list to update in transaction");
        }
        if (logger.isDebugEnabled()) {
            StringBuilder str = new StringBuilder("Updating ");
            boolean first = true;
            for (Map.Entry<String, List<PartitionOffsets>> topicOffsets : offsets.entrySet()) {
                if (topicOffsets.getValue().isEmpty()) {
                    throw new IllegalArgumentException("Empty offsets range to update in transaction");
                }
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
            if (error != null) {
                closeDueToError(null,
                         new RuntimeException("Restarting read session due to transaction " + transaction.getId() +
                                " with partition offsets from read session " + getStreamId() +
                                " was not committed with reason: " + error));
            } else if (!status.isSuccess()) {
                closeDueToError(null,
                        new RuntimeException("Restarting read session due to transaction " + transaction.getId() +
                                " with partition offsets from read session " + getStreamId() +
                                " was not committed with status: " + status));
            }
        });
        YdbTopic.UpdateOffsetsInTransactionRequest.Builder requestBuilder = YdbTopic.UpdateOffsetsInTransactionRequest
                .newBuilder()
                .setTx(YdbTopic.TransactionIdentity.newBuilder()
                        .setId(transaction.getId())
                        .setSession(transaction.getSessionId()))
                .setConsumer(consumerName);
        offsets.forEach((path, topicOffsets) -> {
            YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.Builder topicOffsetsBuilder = YdbTopic
                    .UpdateOffsetsInTransactionRequest.TopicOffsets.newBuilder()
                    .setPath(path);
            topicOffsets.forEach(partitionOffsets -> {
                YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.PartitionOffsets.Builder partitionOffsetsBuilder
                        = YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.PartitionOffsets.newBuilder()
                                .setPartitionId(partitionOffsets.getPartitionSession().getPartitionId());
                partitionOffsets.getOffsets().forEach(offsetsRange -> partitionOffsetsBuilder.addPartitionOffsets(
                        YdbTopic.OffsetsRange.newBuilder()
                                .setStart(offsetsRange.getStart())
                                .setEnd(offsetsRange.getEnd())
                                .build()));
                topicOffsetsBuilder.addPartitions(partitionOffsetsBuilder);
            });
            requestBuilder.addTopics(topicOffsetsBuilder);
        });

        String traceId = settings.getTraceId() == null ? UUID.randomUUID().toString() : settings.getTraceId();
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withTraceId(traceId)
                .build();
        return rpc.updateOffsetsInTransaction(requestBuilder.build(), grpcRequestSettings);
    }

    protected void closeDueToError(Status status, Throwable th) {
        logger.info("[{}] Session closeDueToError called", streamId);
        if (shutdown()) {
            // Signal reader to retry
            reader.onSessionClosed(status, th);
        }
    }

    @Override
    protected void onStop() {
        logger.debug("[{}] Session onStop called", streamId);
        closePartitionSessions();
    }
}
