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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
import tech.ydb.topic.impl.GrpcStreamRetrier;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderImpl extends GrpcStreamRetrier {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private static final int DEFAULT_DECOMPRESSION_THREAD_COUNT = 4;

    private ReadSessionImpl session;
    private final ReaderSettings settings;
    private final TopicRpc topicRpc;
    private final Executor decompressionExecutor;
    private final ExecutorService defaultDecompressionExecutorService;
    private final AtomicReference<CompletableFuture<Void>> initResultFutureRef = new AtomicReference<>(null);

    // Every reading stream has a sequential number (for debug purposes)
    private final AtomicLong seqNumberCounter = new AtomicLong(0);

    public ReaderImpl(TopicRpc topicRpc, ReaderSettings settings) {
        super(topicRpc.getScheduler());
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new ReadSessionImpl();
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
    protected String getStreamName() {
        return "Reader";
    }

    protected CompletableFuture<Void> initImpl() {
        logger.info("[{}] initImpl called", id);
        if (initResultFutureRef.compareAndSet(null, new CompletableFuture<>())) {
            session.startAndInitialize();
        } else {
            logger.warn("[{}] Init is called on this reader more than once. Nothing is done", id);
        }
        return initResultFutureRef.get();
    }

    protected abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);
    protected abstract void handleStartPartitionSessionRequest(
            YdbTopic.StreamReadMessage.StartPartitionSessionRequest request,
            PartitionSession partitionSession,
            Consumer<StartPartitionSessionSettings> confirmCallback);
    protected abstract void handleStopPartitionSession(
            YdbTopic.StreamReadMessage.StopPartitionSessionRequest request, @Nullable Long partitionId,
            Runnable confirmCallback);
    protected abstract void handleClosePartitionSession(PartitionSession partitionSession);
    protected abstract void handleCloseReader();

    @Override
    protected void onStreamReconnect() {
        session = new ReadSessionImpl();
        session.startAndInitialize();
    }

    @Override
    protected void onShutdown(String reason) {
        session.shutdown();
        if (initResultFutureRef.get() != null && !initResultFutureRef.get().isDone()) {
            initResultFutureRef.get().completeExceptionally(new RuntimeException(reason));
        }
        if (defaultDecompressionExecutorService != null) {
            defaultDecompressionExecutorService.shutdown();
        }
    }

    private class ReadSessionImpl extends ReadSession {
        protected String sessionId = "";
        private final String fullId;
        // Total size to request with next ReadRequest.
        // Used to group several ReadResponses in one on high rps
        private final AtomicLong sizeBytesToRequest = new AtomicLong(0);
        private final Map<Long, PartitionSessionImpl> partitionSessions = new ConcurrentHashMap<>();
        private ReadSessionImpl() {
            super(topicRpc);
            this.fullId = id + '.' + seqNumberCounter.incrementAndGet();
        }

        public void startAndInitialize() {
            logger.debug("[{}] Session {} startAndInitialize called", fullId, sessionId);
            start(this::processMessage).whenComplete(this::onSessionClosing);

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

            if (settings.getReaderName() != null && !settings.getReaderName().isEmpty()) {
                initRequestBuilder.setReaderName(settings.getReaderName());
            }

            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setInitRequest(initRequestBuilder)
                    .build());
        }

        private void sendReadRequest() {
            long currentSizeBytesToRequest = sizeBytesToRequest.getAndSet(0);
            if (currentSizeBytesToRequest <= 0) {
                logger.debug("[{}] Nothing to request in DataRequest. sizeBytesToRequest == {}", fullId,
                        currentSizeBytesToRequest);
                return;
            }
            logger.debug("[{}] Sending DataRequest with {} bytes", fullId, currentSizeBytesToRequest);

            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setReadRequest(YdbTopic.StreamReadMessage.ReadRequest.newBuilder()
                            .setBytesSize(currentSizeBytesToRequest)
                            .build())
                    .build());
        }

        private void sendStartPartitionSessionResponse(PartitionSession partitionSession,
                                                         StartPartitionSessionSettings startSettings) {
            if (!isWorking.get()) {
                logger.info("[{}] Need to send StartPartitionSessionResponse for partition session {} (partition {})," +
                        " but reading session is already closed", fullId, partitionSession.getId(),
                        partitionSession.getPartitionId());
                return;
            }
            if (!partitionSessions.containsKey(partitionSession.getId())) {
                logger.info("[{}] Need to send StartPartitionSessionResponse for partition session {} (partition {})," +
                                " but have no such partition session active", fullId, partitionSession.getId(),
                        partitionSession.getPartitionId());
                return;
            }
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
            logger.info("[{}] Sending StartPartitionSessionResponse for partition session {} (partition {})", fullId,
                    partitionSession.getId(), partitionSession.getPartitionId());
            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setStartPartitionSessionResponse(responseBuilder.build())
                    .build());
        }

        private void sendStopPartitionSessionResponse(long partitionSessionId) {
            if (!isWorking.get()) {
                logger.info("[{}] Need to send StopPartitionSessionResponse for partition session {}, " +
                        "but reading session is already closed", fullId, partitionSessionId);
                return;
            }
            PartitionSessionImpl partitionSession = partitionSessions.remove(partitionSessionId);
            if (partitionSession != null) {
                partitionSession.shutdown();
                logger.info("[{}] Sending StopPartitionSessionResponse for partition session {} (partition {})", fullId,
                        partitionSessionId, partitionSession.getPartitionId());
            } else {
                logger.warn("[{}] Sending StopPartitionSessionResponse for partition session {}, " +
                        "but have no such partition session active", fullId, partitionSessionId);
            }
            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setStopPartitionSessionResponse(
                            YdbTopic.StreamReadMessage.StopPartitionSessionResponse.newBuilder()
                                    .setPartitionSessionId(partitionSessionId)
                                    .build())
                    .build());
        }

        private void commitOffset(long partitionSessionId, long partitionId, OffsetsRange offsets) {
            if (!isWorking.get()) {
                logger.info("[{}] Need to send CommitRequest for partition session {} (partition {})," +
                                " but reading session is already closed", fullId, partitionSessionId, partitionId);
                return;
            }
            logger.info("[{}] Sending CommitRequest for partition session {} (partition {}) with offset range [{},{})",
                    fullId, partitionSessionId, partitionId, offsets.getStart(), offsets.getEnd());
            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setCommitOffsetRequest(YdbTopic.StreamReadMessage.CommitOffsetRequest.newBuilder()
                            .addCommitOffsets(
                                    YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset.newBuilder()
                                            .setPartitionSessionId(partitionSessionId)
                                            .addOffsets(YdbTopic.OffsetsRange.newBuilder()
                                                    .setStart(offsets.getStart())
                                                    .setEnd(offsets.getEnd()))
                            ))
                    .build());
        }

        private void closePartitionSessions() {
            partitionSessions.values().forEach(this::closePartitionSession);
            partitionSessions.clear();
        }

        private void closePartitionSession(PartitionSessionImpl partitionSession) {
            partitionSession.shutdown();
            handleClosePartitionSession(partitionSession.getSessionInfo());
        }

        private void onInitResponse(YdbTopic.StreamReadMessage.InitResponse response) {
            sessionId = response.getSessionId();

            if (initResultFutureRef.get() != null) {
                initResultFutureRef.get().complete(null);
            }
            sizeBytesToRequest.set(settings.getMaxMemoryUsageBytes());
            logger.info("[{}] Session {} initialized. Requesting {} bytes...", fullId, sessionId,
                    settings.getMaxMemoryUsageBytes());
            sendReadRequest();
        }

        private void onStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request) {
            long partitionSessionId = request.getPartitionSession().getPartitionSessionId();
            long partitionId = request.getPartitionSession().getPartitionId();
            logger.info("[{}] Received StartPartitionSessionRequest: partition session {} (partition {})", fullId,
                    partitionSessionId, partitionId);

            PartitionSessionImpl partitionSession = PartitionSessionImpl.newBuilder()
                    .setId(partitionSessionId)
                    .setPath(request.getPartitionSession().getPath())
                    .setPartitionId(partitionId)
                    .setCommittedOffset(request.getCommittedOffset())
                    .setPartitionOffsets(new OffsetsRange(request.getPartitionOffsets().getStart(),
                            request.getPartitionOffsets().getEnd()))
                    .setDecompressionExecutor(decompressionExecutor)
                    .setDataEventCallback(ReaderImpl.this::handleDataReceivedEvent)
                    .setCommitFunction((offsets) -> commitOffset(partitionSessionId, partitionId, offsets))
                    .build();
            partitionSessions.put(partitionSession.getId(), partitionSession);

            handleStartPartitionSessionRequest(request, partitionSession.getSessionInfo(),
                    (settings) -> sendStartPartitionSessionResponse(partitionSession.getSessionInfo(), settings));
        }

        protected void onStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
            if (request.getGraceful()) {
                PartitionSessionImpl partitionSession = partitionSessions.get(request.getPartitionSessionId());
                if (partitionSession != null) {
                    logger.info("[{}] Received graceful StopPartitionSessionRequest for partition session {} " +
                            "(partition {})", fullId, partitionSession.getId(), partitionSession.getPartitionId());
                } else {
                    logger.warn("[{}] Received graceful StopPartitionSessionRequest for partition session {}, " +
                            "but have no such partition session active", fullId, request.getPartitionSessionId());
                }
                handleStopPartitionSession(request, partitionSession == null ? null : partitionSession.getPartitionId(),
                        () -> sendStopPartitionSessionResponse(request.getPartitionSessionId()));
            } else {
                PartitionSessionImpl partitionSession = partitionSessions.remove(request.getPartitionSessionId());
                if (partitionSession != null) {
                    logger.info("[{}] Received force StopPartitionSessionRequest for partition session {} (partition " +
                                    "{})", fullId, partitionSession.getId(), partitionSession.getPartitionId());
                    closePartitionSession(partitionSession);
                } else {
                    logger.warn("[{}] Received force StopPartitionSessionRequest for partition session {}, " +
                            "but have no such partition session running", fullId, request.getPartitionSessionId());
                }
            }
        }

        private void onReadResponse(YdbTopic.StreamReadMessage.ReadResponse readResponse) {
            final long responseBytesSize = readResponse.getBytesSize();
            logger.trace("[{}] Received ReadResponse of {} bytes", fullId, responseBytesSize);
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
                            logger.warn("[{}] Received PartitionData for unknown(closed?) PartitionSessionId={}",
                                    fullId, partitionId);
                        }
                    });
            CompletableFuture.allOf(batchReadFutures.toArray(new CompletableFuture<?>[0]))
                    .whenComplete((res, th) -> {
                        if (th != null) {
                            logger.error("[{}] Exception while waiting for batches to be read:", fullId, th);
                        }
                        if (isWorking.get()) {
                            logger.trace("[{}] Finished handling ReadResponse of {} bytes. Sending ReadRequest...",
                                    fullId, responseBytesSize);
                            this.sizeBytesToRequest.addAndGet(responseBytesSize);
                            sendReadRequest();
                        } else {
                            logger.trace("[{}] Finished handling ReadResponse of {} bytes. Read session is already " +
                                    "closed -- no need to send ReadRequest", fullId, responseBytesSize);
                        }
                    });
        }

        protected void onCommitOffsetResponse(YdbTopic.StreamReadMessage.CommitOffsetResponse response) {
            logger.trace("[{}] Received CommitOffsetResponse", fullId);
            for (YdbTopic.StreamReadMessage.CommitOffsetResponse.PartitionCommittedOffset partitionCommittedOffset :
                    response.getPartitionsCommittedOffsetsList()) {
                PartitionSessionImpl partitionSession =
                        partitionSessions.get(partitionCommittedOffset.getPartitionSessionId());
                if (partitionSession != null) {
                    partitionSession.handleCommitResponse(partitionCommittedOffset.getCommittedOffset());
                } else {
                    logger.debug("[{}] Received CommitOffsetResponse for closed partition session with id={}", fullId,
                            partitionCommittedOffset.getPartitionSessionId());
                }
            }
        }

        protected void onPartitionSessionStatusResponse(
                YdbTopic.StreamReadMessage.PartitionSessionStatusResponse response) {
            PartitionSessionImpl partitionSession = partitionSessions.get(response.getPartitionSessionId());
            logger.info("[{}] Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                            " Partition offsets: [{}, {}). Committed offset: {}", fullId,
                    response.getPartitionSessionId(),
                    partitionSession == null ? "unknown" : partitionSession.getPartitionId(),
                    response.getPartitionOffsets().getStart(), response.getPartitionOffsets().getEnd(),
                    response.getCommittedOffset());
        }

        private void processMessage(YdbTopic.StreamReadMessage.FromServer message) {
            if (!isWorking.get()) {
                logger.debug("[{}] processMessage called, but read session is already closed", fullId);
                return;
            }
            logger.debug("[{}] processMessage called", fullId);
            if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                reconnectCounter.set(0);
            } else {
                logger.warn("[{}] Got non-success status in processMessage method: {}", fullId, message);
                onSessionClosed(Status.of(StatusCode.fromProto(message.getStatus()))
                        .withIssues(Issue.of("Got a message with non-success status: " + message,
                                Issue.Severity.ERROR)), null);
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
                logger.debug("[{}] Received UpdateTokenResponse", fullId);
            } else {
                logger.error("[{}] Unhandled message from server: {}", fullId, message);
            }
        }

        private void onSessionClosing(Status status, Throwable th) {
            logger.info("[{}] Session {} onSessionClosing called", fullId, sessionId);
            if (isWorking.get()) {
                shutdown();
                // Signal reader to retry
                onSessionClosed(status, th);
            }
        }

        @Override
        protected void onStop() {
            logger.debug("[{}] Session {} onStop called", fullId, sessionId);
            closePartitionSessions();
        }
    }

}
