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
import tech.ydb.topic.read.impl.events.StopPartitionSessionEventImpl;
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

    private final Map<Long, PartitionSession> partitions = new ConcurrentHashMap<>();
    private final Map<Long, ReadPartitionSession> partSessions = new ConcurrentHashMap<>();

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

    int getMaxBatchSize() {
        return maxBatchSize;
    }

    MessageDecoder getMessageDecoder() {
        return decoder;
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

    @Override
    public void startAndInitialize() {
        logger.debug("[{}] Session startAndInitialize called", streamId);
        start(this::processMessage).whenComplete(this::closeDueToError);

        send(YdbTopic.StreamReadMessage.FromClient.newBuilder().setInitRequest(initRequest).build());
    }

    @Override
    protected void onStop() {
        logger.debug("[{}] Session onStop called", streamId);

        partSessions.values().forEach(ReadPartitionSession::stop);
        partSessions.clear();

        partitions.values().forEach(reader::handleClosePartitionSession);
        partitions.clear();
    }

    protected void closeDueToError(Status status, Throwable th) {
        logger.info("[{}] Session closeDueToError called", streamId);
        if (shutdown()) {
            // Signal reader to retry
            reader.onSessionClosed(status, th);
        }
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

    void sendCommitOffsetRequest(PartitionSession session, List<OffsetsRange> rangesToCommit) {
        if (isStopped()) {
            logger.atInfo()
                    .setMessage("[{}] Need to send CommitRequest for {} with offset ranges {}, "
                            + "but reading session is already closed")
                    .addArgument(streamId)
                    .addArgument(session)
                    .addArgument(() -> rangesToCommit.stream().map(Object::toString).collect(Collectors.joining(", ")))
                    .log();
            return;
        }

        send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setCommitOffsetRequest(YdbTopic.StreamReadMessage.CommitOffsetRequest.newBuilder()
                        .addCommitOffsets(YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset
                                .newBuilder()
                                .setPartitionSessionId(session.getId())
                                .addAllOffsets(rangesToCommit.stream()
                                        .map(ReadSession::buildOffsetRange)
                                        .collect(Collectors.toList()))
                                .build())
                        .build())
                .build());
    }

    private void onInitResponse(YdbTopic.StreamReadMessage.InitResponse response) {
        String sessionId = response.getSessionId();

        sizeBytesToRequest.set(maxMemoryUsageBytes);
        logger.info("[{}] Session {} initialized. Requesting {} bytes...", streamId, sessionId, maxMemoryUsageBytes);
        reader.onSessionStarted(sessionId);
        sendReadRequest();
    }

    private void onStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest req) {
        long psid = req.getPartitionSession().getPartitionSessionId();
        long pid = req.getPartitionSession().getPartitionId();
        long committed = req.getCommittedOffset();

        PartitionSession partition = new PartitionSession(psid, pid, req.getPartitionSession().getPath());
        OffsetsRange offsets = new OffsetsRangeImpl(
                req.getPartitionOffsets().getStart(),
                req.getPartitionOffsets().getEnd()
        );

        String traceID = streamId + '/' + psid + "-p" + pid;
        logger.info("[{}] Received StartPartitionSessionRequest for {} and consumer \"{}\" with committedOffset {}"
                + " and partitionOffsets {}", traceID, partition, consumerName, committed, offsets);

        partitions.put(psid, partition);

        reader.handleStartPartitionSessionRequest(new StartPartitionSessionEventImpl(partition, committed, offsets) {
            @Override
            public void confirm(StartPartitionSessionSettings options) {
                if (isStopped()) {
                    logger.info("[{}] Need to send StartPartitionSessionResponse, but reading session is "
                            + "already closed", traceID);
                    return;
                }

                PartitionSession partition = partitions.get(psid);
                if (partition == null) {
                    logger.info("[{}] Need to send StartPartitionSessionResponse, but have no such active partition "
                            + "session anymore", traceID);
                    return;
                }

                long readFrom = committed;
                long commitTo = committed;
                if (options != null) {
                    if (options.getReadOffset() != null) {
                        readFrom = options.getReadOffset();
                    }
                    if (options.getCommitOffset() != null) {
                        commitTo = options.getCommitOffset();
                    }
                }

                partSessions.put(psid, new ReadPartitionSession(traceID, ReadSession.this, partition, commitTo) {
                    @Override
                    public CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
                        return reader.handleDataReceivedEvent(event);
                    }
                });

                logger.info("[{}] Sending StartPartitionSessionResponse for {} and consumer \"{}\" with readOffset "
                        + "{} and commitOffset {}", traceID, partition, consumerName, readFrom, commitTo);
                send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                        .setStartPartitionSessionResponse(YdbTopic.StreamReadMessage.StartPartitionSessionResponse
                                .newBuilder()
                                .setPartitionSessionId(psid)
                                .setReadOffset(readFrom)
                                .setCommitOffset(commitTo)
                                .build()
                        ).build());
            }
        });
    }

    protected void onStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        if (!request.getGraceful()) {
            long psid = request.getPartitionSessionId();
            PartitionSession partition = partitions.remove(psid);
            if (partition == null) {
                logger.warn("[{}] Received force StopPartitionSessionRequest for partition session {}, " +
                        "but have no such partition session running", streamId, request.getPartitionSessionId());
                return;
            }

            ReadPartitionSession rps = partSessions.remove(psid);
            if (rps != null) {
                logger.info("[{}] Received force StopPartitionSessionRequest for {} ", streamId, rps.getPartition());
                rps.stop();
            }
            return;
        }

        long committedOffset = request.getCommittedOffset();
        long psid = request.getPartitionSessionId();
        PartitionSession partition = partitions.get(psid);
        if (partition == null) {
            logger.error("[{}] Received graceful StopPartitionSessionRequest for partition session {}, " +
                    "but have no such partition session active", streamId, psid);
            closeDueToError(null, new RuntimeException("Restarting read session due to receiving "
                    + "StopPartitionSessionRequest with PartitionSessionId " + psid + " that SDK knows nothing about"));
            return;
        }

        logger.info("[{}] Received graceful StopPartitionSessionRequest for {}", streamId, partition);
        reader.handleStopPartitionSession(new StopPartitionSessionEventImpl(partition, committedOffset) {
            @Override
            public void confirm() {
                if (isStopped()) {
                    logger.info("[{}] Need to send StopPartitionSessionResponse for {}, " +
                            "but reading session is already closed", streamId, partition);
                    return;
                }

                if (partitions.remove(psid, partition)) {
                    logger.info("[{}] Sending StopPartitionSessionResponse for {}", streamId, partition);
                    send(YdbTopic.StreamReadMessage.FromClient.newBuilder().setStopPartitionSessionResponse(
                                    YdbTopic.StreamReadMessage.StopPartitionSessionResponse.newBuilder()
                                            .setPartitionSessionId(psid)
                                            .build())
                            .build());

                    ReadPartitionSession session = partSessions.remove(psid);
                    if (session != null) {
                        session.stop();
                    }
                }
            }
        });
    }

    private void onReadResponse(YdbTopic.StreamReadMessage.ReadResponse response) {
        final long responseBytesSize = response.getBytesSize();
        logger.trace("[{}] Received ReadResponse of {} bytes", streamId, responseBytesSize);
        List<CompletableFuture<Void>> batchReadFutures = new ArrayList<>();

        for (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data: response.getPartitionDataList()) {
            long psid = data.getPartitionSessionId();
            ReadPartitionSession session = partSessions.get(data.getPartitionSessionId());
            if (session == null) {
                logger.warn("[{}] Received PartitionData for unknown(most likely already closed) PartitionSessionId={}",
                        streamId, psid);
                // TODO: release memory buffer
                continue;
            }

            // Completes when all messages from a batch are read by user
            batchReadFutures.add(session.addBatches(data.getBatchesList()));
        }

        CompletableFuture.allOf(batchReadFutures.toArray(new CompletableFuture<?>[0]))
                .whenComplete((res, th) -> {
                    if (th != null) {
                        logger.error("[{}] Exception while waiting for batches to be read:", streamId, th);
                        return;
                    }
                    if (isStopped()) {
                        logger.trace("[{}] Finished handling ReadResponse of {} bytes. Read session is already " +
                                "closed -- no need to send ReadRequest", streamId, responseBytesSize);
                        return;
                    }

                    logger.trace("[{}] Finished handling ReadResponse of {} bytes. Sending ReadRequest...",
                            streamId, responseBytesSize);
                    this.sizeBytesToRequest.addAndGet(responseBytesSize);
                    sendReadRequest();
                });
    }

    protected void onCommitOffsetResponse(YdbTopic.StreamReadMessage.CommitOffsetResponse response) {
        logger.trace("[{}] Received CommitOffsetResponse", streamId);
        response.getPartitionsCommittedOffsetsList().forEach(offset -> {
            ReadPartitionSession session = partSessions.get(offset.getPartitionSessionId());
            if (session == null) {
                logger.info("[{}] Received CommitOffsetResponse for unknown (most likely already closed) " +
                                "e session with id={}", streamId, offset.getPartitionSessionId());
                return;
            }

            // Handling CompletableFuture completions for single commits
            session.confirmCommit(offset.getCommittedOffset());
            // Handling onCommitResponse callback
            reader.handleCommitResponse(offset.getCommittedOffset(), session.getPartition());
        });
    }

    protected void onPartitionSessionStatusResponse(YdbTopic.StreamReadMessage.PartitionSessionStatusResponse resp) {
        PartitionSession partition = partitions.get(resp.getPartitionSessionId());
        logger.info("[{}] Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                        " Partition offsets: [{}, {}). Committed offset: {}", streamId,
                resp.getPartitionSessionId(),
                partition == null ? "unknown" : partition.getPartitionId(),
                resp.getPartitionOffsets().getStart(),
                resp.getPartitionOffsets().getEnd(),
                resp.getCommittedOffset());
    }

    private void processMessage(YdbTopic.StreamReadMessage.FromServer message) {
        if (isStopped()) {
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

        YdbTopic.UpdateOffsetsInTransactionRequest req = YdbTopic.UpdateOffsetsInTransactionRequest.newBuilder()
                .setTx(YdbTopic.TransactionIdentity.newBuilder()
                        .setId(transaction.getId())
                        .setSession(transaction.getSessionId())
                        .build())
                .setConsumer(consumerName)
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
                        .map(ReadSession::buildOffsetRange)
                        .collect(Collectors.toList()))
                .build();
    }

    private static YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets buildTopicOffsets(String topicPath,
            List<PartitionOffsets> partitions) {

        return YdbTopic.UpdateOffsetsInTransactionRequest.TopicOffsets.newBuilder()
                .setPath(topicPath)
                .addAllPartitions(partitions.stream()
                        .map(ReadSession::buildPartitionOffsets)
                        .collect(Collectors.toList()))
                .build();
    }

    private static YdbTopic.OffsetsRange buildOffsetRange(OffsetsRange range) {
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
