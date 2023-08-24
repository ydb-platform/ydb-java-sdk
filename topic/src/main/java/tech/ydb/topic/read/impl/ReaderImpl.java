package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.impl.ReaderWriterBaseImpl;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderImpl extends ReaderWriterBaseImpl<ReadSession> {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private static final int DEFAULT_DECOMPRESSION_THREAD_COUNT = 4;

    private final ReaderSettings settings;
    private final TopicRpc topicRpc;
    // Total size of all messages that are read from server and are not handled yet
    private final AtomicLong sizeBytesAcquired = new AtomicLong(0);
    // Total size to request with next ReadRequest.
    // Used to group several ReadResponses in one on high rps
    private final AtomicLong sizeBytesToRequest = new AtomicLong(0);
    private final Map<Long, PartitionSession> partitionSessions = new ConcurrentHashMap<>();
    private final Executor decompressionExecutor;
    private final ExecutorService defaultDecompressionExecutorService;
    private CompletableFuture<Void> initResultFuture = new CompletableFuture<>();

    public ReaderImpl(TopicRpc topicRpc, ReaderSettings settings) {
        super(topicRpc.getScheduler());
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new ReadSession(topicRpc);
        if (settings.getDecompressionExecutor() != null) {
            this.defaultDecompressionExecutorService = null;
            this.decompressionExecutor = settings.getDecompressionExecutor();
        } else {
            this.defaultDecompressionExecutorService = Executors.newFixedThreadPool(DEFAULT_DECOMPRESSION_THREAD_COUNT);
            this.decompressionExecutor = defaultDecompressionExecutorService;
        }
        StringBuilder message = new StringBuilder("Reader");
        if (settings.getReaderName() != null && !settings.getReaderName().isEmpty()) {
            message.append(" \"").append(settings.getReaderName()).append("\"");
        }
        message.append(" (generated id ").append(id).append(")");
        message.append(" created for topic(s): ");
        for (TopicReadSettings topic : settings.getTopics()) {
            if (topic != settings.getTopics().get(0)) {
                message.append(", ");
            }
            message.append("\"").append(topic.getPath()).append("\"");
        }
        message.append(" and Consumer: \"").append(settings.getConsumerName()).append("\"");
        logger.info(message.toString());
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected String getSessionType() {
        return "Reader";
    }

    protected CompletableFuture<Void> initImpl() {
        logger.debug("[{}] initImpl started", id);
        session.start(this::processMessage).whenComplete(this::completeSession);

        initResultFuture = new CompletableFuture<>();
        YdbTopic.StreamReadMessage.InitRequest.Builder initRequestBuilder = YdbTopic.StreamReadMessage.InitRequest
                .newBuilder()
                .setConsumer(settings.getConsumerName());
        settings.getTopics().forEach(topicReadSettings -> {
            YdbTopic.StreamReadMessage.InitRequest.TopicReadSettings.Builder settingsBuilder =
                    YdbTopic.StreamReadMessage.InitRequest.TopicReadSettings.newBuilder()
                            .setPath(topicReadSettings.getPath());
            if (topicReadSettings.getPartitionIds() != null && !topicReadSettings.getPartitionIds().isEmpty()) {
                settingsBuilder.addAllPartitionIds(topicReadSettings.getPartitionIds());
            }
            if (topicReadSettings.getMaxLag() != null) {
                settingsBuilder.setMaxLag(ProtobufUtils.durationToProto(topicReadSettings.getMaxLag()));
            }
            if (topicReadSettings.getReadFrom() != null) {
                settingsBuilder.setReadFrom(ProtobufUtils.instantToProto(topicReadSettings.getReadFrom()));
            }
            initRequestBuilder.addTopicsReadSettings(settingsBuilder);
        });

        // Not supported in API yet
        /* if (settings.getReaderName() != null && !settings.getReaderName().isEmpty()) {
            initRequestBuilder.setReaderName(settings.getReaderName());
        }*/

        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setInitRequest(initRequestBuilder)
                .build());
        return initResultFuture;
    }

    private void sendReadRequest() {
        long currentSizeBytesToRequest = sizeBytesToRequest.getAndSet(0);
        if (currentSizeBytesToRequest <= 0) {
            logger.debug("[{}] Nothing to request in DataRequest. sizeBytesToRequest == {}", id,
                    currentSizeBytesToRequest);
            return;
        }
        logger.debug("[{}] Sending DataRequest with {} bytes", id, currentSizeBytesToRequest);

        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setReadRequest(YdbTopic.StreamReadMessage.ReadRequest.newBuilder()
                        .setBytesSize(currentSizeBytesToRequest)
                        .build())
                .build());
    }

    protected void sendStartPartitionSessionResponse(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request,
                                                     StartPartitionSessionSettings startSettings) {
        PartitionSession partitionSession = PartitionSession.newBuilder()
                .setId(request.getPartitionSession().getPartitionSessionId())
                .setPath(request.getPartitionSession().getPath())
                .setPartitionId(request.getPartitionSession().getPartitionId())
                .setCommittedOffset(request.getCommittedOffset())
                .setPartitionOffsets(new OffsetsRange(request.getPartitionOffsets().getStart(),
                        request.getPartitionOffsets().getEnd()))
                .setDecompressionExecutor(decompressionExecutor)
                .setDataEventCallback(this::handleDataReceivedEvent)
                .setCommitFunction(this::commitOffset)
                .build();
        partitionSessions.put(partitionSession.getId(), partitionSession);

        YdbTopic.StreamReadMessage.StartPartitionSessionResponse.Builder responseBuilder =
                YdbTopic.StreamReadMessage.StartPartitionSessionResponse.newBuilder()
                        .setPartitionSessionId(partitionSession.getId());
        if (startSettings != null) {
            if (startSettings.getReadOffset() != null) {
                responseBuilder.setReadOffset(startSettings.getReadOffset());
            }
            if (startSettings.getCommitOffset() != null) {
                responseBuilder.setCommitOffset(startSettings.getCommitOffset());
            }
        }
        logger.info("[{}] Sending StartPartitionSessionResponse for partition session {} (partition {})", id,
                partitionSession.getId(), partitionSession.getPartitionId());
        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setStartPartitionSessionResponse(responseBuilder.build())
                .build());
    }

    protected void sendStopPartitionSessionResponse(long partitionSessionId) {

        PartitionSession partitionSession = partitionSessions.get(partitionSessionId);
        if (partitionSession != null) {
            logger.info("[{}] User confirmed graceful shutdown of partition session {} (partition {})", id,
                partitionSessionId, partitionSession.getPartitionId());
            partitionSession.shutdown();
            logger.info("[{}] Sending StopPartitionSessionResponse for partition session {} (partition {})", id,
                    partitionSessionId, partitionSession.getPartitionId());
        } else {
            logger.warn("[{}] Sending StopPartitionSessionResponse for partition session {}, " +
                    "but have no such partition session running", id, partitionSessionId);
        }
        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setStopPartitionSessionResponse(YdbTopic.StreamReadMessage.StopPartitionSessionResponse.newBuilder()
                        .setPartitionSessionId(partitionSessionId)
                        .build())
                .build());
    }

    private void handleReadResponse(YdbTopic.StreamReadMessage.ReadResponse readResponse) {
        final long responseBytesSize = readResponse.getBytesSize();
        sizeBytesAcquired.addAndGet(responseBytesSize);
        logger.trace("Received ReadResponse of {} bytes", responseBytesSize);
        List<CompletableFuture<Void>> batchReadFutures = new ArrayList<>();
        readResponse.getPartitionDataList().forEach((YdbTopic.StreamReadMessage.ReadResponse.PartitionData data) -> {
            long partitionId = data.getPartitionSessionId();
            PartitionSession partitionSession = partitionSessions.get(partitionId);
            if (partitionSession != null) {
                // Completes when all messages from a batch are read by user
                CompletableFuture<Void> readFuture = partitionSession.addBatches(data.getBatchesList());
                batchReadFutures.add(readFuture);
            } else {
                logger.debug("[{}] Received PartitionData for unknown(closed?) PartitionSessionId={}", id, partitionId);
            }
        });
        CompletableFuture.allOf(batchReadFutures.toArray(new CompletableFuture<?>[0]))
                .whenComplete((res, th) -> {
                    if (th != null) {
                        logger.error("[{}] Exception while waiting for batches to be read:", id, th);
                    }
                    boolean needToSendDataRequest = false;
                    synchronized (sizeBytesAcquired) {
                        long newAcquiredSize = sizeBytesAcquired.addAndGet(-responseBytesSize);
                        if (isReconnecting.get()) {
                            logger.trace("[{}] Finished handling ReadResponse of {} bytes. Reconnect is in progress" +
                                    " -- no need to send ReadRequest", id, responseBytesSize);
                        } else {
                            logger.trace("[{}] Finished handling ReadResponse of {} bytes. sizeBytesAcquired is now " +
                                    "{}. Sending ReadRequest...", id, responseBytesSize, newAcquiredSize);
                            this.sizeBytesToRequest.addAndGet(responseBytesSize);
                            needToSendDataRequest = true;
                        }
                    }
                    if (needToSendDataRequest) {
                        sendReadRequest();
                    }
                });
    }

    protected void handleCommitOffsetResponse(YdbTopic.StreamReadMessage.CommitOffsetResponse commitOffsetResponse) {
        for (YdbTopic.StreamReadMessage.CommitOffsetResponse.PartitionCommittedOffset partitionCommittedOffset :
                commitOffsetResponse.getPartitionsCommittedOffsetsList()) {
            PartitionSession partitionSession = partitionSessions.get(partitionCommittedOffset.getPartitionSessionId());
            if (partitionSession != null) {
                partitionSession.handleCommitResponse(partitionCommittedOffset.getCommittedOffset());
            } else {
                logger.debug("[{}] Received CommitOffsetResponse for closed partition session with id={}", id,
                        partitionCommittedOffset.getPartitionSessionId());
            }
        }
    }

    protected void handleStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        if (request.getGraceful()) {
            PartitionSession partitionSession = partitionSessions.get(request.getPartitionSessionId());
            if (partitionSession != null) {
                logger.info("[{}] Received graceful StopPartitionSessionRequest for partition session {} " +
                                "(partition {})", id, partitionSession.getId(), partitionSession.getPartitionId());
            } else {
                logger.warn("[{}] Received graceful StopPartitionSessionRequest for partition session {}, " +
                                "but have no such partition session running", id, request.getPartitionSessionId());
            }
            handleStopPartitionSession(request, partitionSession == null ? null : partitionSession.getPartitionId());
        } else {
            PartitionSession partitionSession = partitionSessions.remove(request.getPartitionSessionId());
            if (partitionSession != null) {
                logger.info("[{}] Received force StopPartitionSessionRequest for partition session {} (partition {})",
                        id, partitionSession.getId(), partitionSession.getPartitionId());
                closePartitionSession(partitionSession);
            } else {
                logger.warn("[{}] Received force StopPartitionSessionRequest for partition session {}, " +
                        "but have no such partition session running", id, request.getPartitionSessionId());
            }
        }
    }

    protected abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);
    protected abstract void handleStartPartitionSessionRequest(
            YdbTopic.StreamReadMessage.StartPartitionSessionRequest request);
    protected abstract void handleStopPartitionSession(
            YdbTopic.StreamReadMessage.StopPartitionSessionRequest request, @Nullable Long partitionId);
    protected abstract void handleClosePartitionSession(tech.ydb.topic.read.PartitionSession partitionSession);
    protected abstract void handleCloseReader();

    private void commitOffset(long partitionId, OffsetsRange offsets) {
        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setCommitOffsetRequest(YdbTopic.StreamReadMessage.CommitOffsetRequest.newBuilder()
                        .addCommitOffsets(
                                YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset.newBuilder()
                                        .setPartitionSessionId(partitionId)
                                        .addOffsets(YdbTopic.OffsetsRange.newBuilder()
                                                .setStart(offsets.getStart())
                                                .setEnd(offsets.getEnd()))
                        ))
                .build());
    }

    @Override
    protected ReadSession createNewSession() {
        return new ReadSession(topicRpc);
    }

    @Override
    protected void onReconnect() {
        initImpl();
    }

    @Override
    protected void onShutdown(String reason) {
        if (!initResultFuture.isDone()) {
            initResultFuture.completeExceptionally(new RuntimeException(reason));
        }
        closePartitionSessions();
        handleCloseReader();
        if (defaultDecompressionExecutorService != null) {
            defaultDecompressionExecutorService.shutdown();
        }
    }

    @Override
    protected void onSessionStop() {
        logger.info("[{}] Read session is stopped. Closing all current Partition sessions...", id);
        closePartitionSessions();
    }

    private void closePartitionSessions() {
        partitionSessions.values().forEach(this::closePartitionSession);
        partitionSessions.clear();
    }

    private void closePartitionSession(PartitionSession partitionSession) {
        partitionSession.shutdown();
        handleClosePartitionSession(partitionSession.getSessionInfo());
    }

    private void processMessage(YdbTopic.StreamReadMessage.FromServer message) {
        logger.trace("[{}] processMessage called", id);
        if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            reconnectCounter.set(0);
        } else {
            logger.warn("[{}] Got non-success status in processMessage method: {}", id, message);
            completeSession(Status.of(StatusCode.fromProto(message.getStatus()))
                    .withIssues(Issue.of("Got a message with non-success status: " + message,
                            Issue.Severity.ERROR)), null);
            return;
        }

        if (message.hasInitResponse()) {
            currentSessionId = message.getInitResponse().getSessionId();
            initResultFuture.complete(null);
            synchronized (sizeBytesAcquired) {
                long bytesAvailable = settings.getMaxMemoryUsageBytes() - sizeBytesAcquired.get();
                sizeBytesToRequest.addAndGet(bytesAvailable);
                logger.info("[{}] Session {} initialized. Requesting available {} bytes...", id, currentSessionId,
                        bytesAvailable);
            }
            sendReadRequest();
        } else if (message.hasStartPartitionSessionRequest()) {
            YdbTopic.StreamReadMessage.StartPartitionSessionRequest request = message.getStartPartitionSessionRequest();
            logger.info("[{}] Received StartPartitionSessionRequest: partition session {} (partition {})", id,
                    request.getPartitionSession().getPartitionSessionId(),
                    request.getPartitionSession().getPartitionId());
            handleStartPartitionSessionRequest(request);
        } else if (message.hasStopPartitionSessionRequest()) {
            handleStopPartitionSessionRequest(message.getStopPartitionSessionRequest());
        } else if (message.hasReadResponse()) {
            handleReadResponse(message.getReadResponse());
        } else if (message.hasCommitOffsetResponse()) {
            handleCommitOffsetResponse(message.getCommitOffsetResponse());
        } else if (message.hasPartitionSessionStatusResponse()) {
            YdbTopic.StreamReadMessage.PartitionSessionStatusResponse response =
                    message.getPartitionSessionStatusResponse();
            PartitionSession partitionSession = partitionSessions.get(response.getPartitionSessionId());
            logger.info("[{}] Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                            " Partition offsets: [{}, {}). Committed offset: {}", id,
                    response.getPartitionSessionId(),
                    partitionSession == null ? "unknown" : partitionSession.getPartitionId(),
                    response.getPartitionOffsets().getStart(), response.getPartitionOffsets().getEnd(),
                    response.getCommittedOffset());
        } else if (message.hasUpdateTokenResponse()) {
            logger.debug("[{}] Received UpdateTokenResponse", id);
        } else {
            logger.error("[{}] Unhandled message from server: {}", id, message);
        }
    }
}
