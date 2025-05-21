package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

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
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.GrpcStreamRetrier;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderImpl extends GrpcStreamRetrier {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private static final int DEFAULT_DECOMPRESSION_THREAD_COUNT = 4;

    private volatile ReadSessionImpl session;
    private final ReaderSettings settings;
    private final TopicRpc topicRpc;
    private final Executor decompressionExecutor;
    private final ExecutorService defaultDecompressionExecutorService;
    private final AtomicReference<CompletableFuture<Void>> initResultFutureRef = new AtomicReference<>(null);
    private final CodecRegistry codecRegistry;

    // Every reading stream has a sequential number (for debug purposes)
    private final AtomicLong seqNumberCounter = new AtomicLong(0);
    private final String consumerName;

    public ReaderImpl(TopicRpc topicRpc, ReaderSettings settings, @Nonnull CodecRegistry codecRegistry) {
        super(topicRpc.getScheduler(), settings.getErrorsHandler());
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new ReadSessionImpl();
        this.codecRegistry = codecRegistry;
        if (settings.getDecompressionExecutor() != null) {
            this.defaultDecompressionExecutorService = null;
            this.decompressionExecutor = settings.getDecompressionExecutor();
        } else {
            this.defaultDecompressionExecutorService = Executors.newFixedThreadPool(DEFAULT_DECOMPRESSION_THREAD_COUNT);
            this.decompressionExecutor = defaultDecompressionExecutorService;
        }
        StringBuilder message = new StringBuilder("Reader");
        if (settings.getReaderName() != null) {
            message.append(" \"").append(settings.getReaderName()).append("\"");
        }
        message.append(" (generated id ").append(id).append(")");
        message.append(" created for Topic(s) ");
        for (TopicReadSettings topic : settings.getTopics()) {
            if (topic != settings.getTopics().get(0)) {
                message.append(", ");
            }
            message.append("\"").append(topic.getPath()).append("\"");
        }
        if (settings.getConsumerName() != null) {
            message.append(" and Consumer \"").append(settings.getConsumerName());
            consumerName = settings.getConsumerName();
        } else {
            message.append(" without a Consumer");
            consumerName = "NoConsumer";
        }
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
    protected abstract void handleSessionStarted(String sessionId);
    protected abstract void handleCommitResponse(long committedOffset, PartitionSession partitionSession);
    protected abstract void handleStartPartitionSessionRequest(
            YdbTopic.StreamReadMessage.StartPartitionSessionRequest request,
            PartitionSession partitionSession,
            Consumer<StartPartitionSessionSettings> confirmCallback);
    protected abstract void handleStopPartitionSession(
            YdbTopic.StreamReadMessage.StopPartitionSessionRequest request,
            PartitionSession partitionSession,
            Runnable confirmCallback);
    protected abstract void handleClosePartitionSession(PartitionSession partitionSession);

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

    protected CompletableFuture<Status> sendUpdateOffsetsInTransaction(YdbTransaction transaction,
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
        final ReadSessionImpl currentSession = session;
        transaction.getStatusFuture().whenComplete((status, error) -> {
            if (error != null) {
                currentSession.closeDueToError(null,
                        new RuntimeException("Restarting read session due to transaction " + transaction.getId() +
                                " with partition offsets from read session " + currentSession.streamId +
                                " was not committed with reason: " + error));
            } else if (!status.isSuccess()) {
                currentSession.closeDueToError(null,
                        new RuntimeException("Restarting read session due to transaction " + transaction.getId() +
                                " with partition offsets from read session " + currentSession.streamId +
                                " was not committed with status: " + status));
            }
        });
        YdbTopic.UpdateOffsetsInTransactionRequest.Builder requestBuilder = YdbTopic.UpdateOffsetsInTransactionRequest
                .newBuilder()
                .setTx(YdbTopic.TransactionIdentity.newBuilder()
                        .setId(transaction.getId())
                        .setSession(transaction.getSessionId()))
                .setConsumer(this.settings.getConsumerName());
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
        return topicRpc.updateOffsetsInTransaction(requestBuilder.build(), grpcRequestSettings);
    }

    protected class ReadSessionImpl extends ReadSession {
        protected String sessionId;
        // Total size to request with next ReadRequest.
        // Used to group several ReadResponses in one on high rps
        private final AtomicLong sizeBytesToRequest = new AtomicLong(0);
        private final Map<Long, PartitionSessionImpl> partitionSessions = new ConcurrentHashMap<>();
        private ReadSessionImpl() {
            super(topicRpc, id + '.' + seqNumberCounter.incrementAndGet());
        }

        @Override
        public void startAndInitialize() {
            logger.debug("[{}] Session startAndInitialize called", streamId);
            start(this::processMessage).whenComplete(this::closeDueToError);

            YdbTopic.StreamReadMessage.InitRequest.Builder initRequestBuilder = YdbTopic.StreamReadMessage.InitRequest
                    .newBuilder();
            if (settings.getConsumerName() != null) {
                initRequestBuilder.setConsumer(settings.getConsumerName());
            }
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

            String readerName = settings.getReaderName();
            if (readerName != null && !readerName.isEmpty()) {
                initRequestBuilder.setReaderName(readerName);
            }

            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setInitRequest(initRequestBuilder)
                    .build());
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

        private void sendStartPartitionSessionResponse(PartitionSessionImpl partitionSession,
                                                         StartPartitionSessionSettings startSettings) {
            if (!isWorking.get()) {
                logger.info("[{}] Need to send StartPartitionSessionResponse, but reading session is already closed",
                        partitionSession.getFullId());
                return;
            }
            if (!partitionSessions.containsKey(partitionSession.getId())) {
                logger.info("[{}] Need to send StartPartitionSessionResponse, but have no such active partition " +
                        "session anymore", partitionSession.getFullId());
                return;
            }
            YdbTopic.StreamReadMessage.StartPartitionSessionResponse.Builder responseBuilder =
                    YdbTopic.StreamReadMessage.StartPartitionSessionResponse.newBuilder()
                            .setPartitionSessionId(partitionSession.getId());
            Long userDefinedReadOffset = null;
            Long userDefinedCommitOffset = null;
            if (startSettings != null) {
                userDefinedReadOffset = startSettings.getReadOffset();
                if (userDefinedReadOffset != null) {
                    responseBuilder.setReadOffset(userDefinedReadOffset);
                    partitionSession.setLastReadOffset(userDefinedReadOffset);
                }
                userDefinedCommitOffset = startSettings.getCommitOffset();
                if (userDefinedCommitOffset != null) {
                    responseBuilder.setCommitOffset(userDefinedCommitOffset);
                    partitionSession.setLastCommittedOffset(userDefinedCommitOffset);
                }
            }
            logger.info("[{}] Sending StartPartitionSessionResponse with readOffset {} and commitOffset {}",
                    partitionSession.getFullId(), userDefinedReadOffset, userDefinedCommitOffset);
            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setStartPartitionSessionResponse(responseBuilder.build())
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

        private void sendCommitOffsetRequest(long partitionSessionId, long partitionId,
                                       List<OffsetsRange> rangesToCommit) {
            if (!isWorking.get()) {
                if (logger.isInfoEnabled()) {
                    StringBuilder message = new StringBuilder("[").append(streamId)
                            .append("] Need to send CommitRequest for partition session ").append(partitionSessionId)
                            .append(" (partition ").append(partitionId).append(") with offset ranges ");
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

            YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset.Builder builder =
            YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset.newBuilder()
                    .setPartitionSessionId(partitionSessionId);
            rangesToCommit.forEach(range -> {
                builder.addOffsets(YdbTopic.OffsetsRange.newBuilder()
                        .setStart(range.getStart())
                        .setEnd(range.getEnd()));
            });
            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setCommitOffsetRequest(YdbTopic.StreamReadMessage.CommitOffsetRequest.newBuilder()
                            .addCommitOffsets(builder))
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
            logger.info("[{}] Session {} initialized. Requesting {} bytes...", streamId, sessionId,
                    settings.getMaxMemoryUsageBytes());
            handleSessionStarted(sessionId);
            sendReadRequest();
        }

        private void onStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request) {
            long partitionSessionId = request.getPartitionSession().getPartitionSessionId();
            long partitionId = request.getPartitionSession().getPartitionId();
            String partitionSessionFullId = streamId + '/' + partitionSessionId + "-p" + partitionId;
            logger.info("[{}] Received StartPartitionSessionRequest for Topic \"{}\" and Consumer \"{}\". " +
                            "Partition session {} (partition {}) with committedOffset {} and partitionOffsets [{}-{})",
                    partitionSessionFullId, request.getPartitionSession().getPath(), consumerName,
                    partitionSessionId, partitionId, request.getCommittedOffset(),
                    request.getPartitionOffsets().getStart(), request.getPartitionOffsets().getEnd());

            PartitionSessionImpl partitionSession = PartitionSessionImpl.newBuilder()
                    .setId(partitionSessionId)
                    .setFullId(partitionSessionFullId)
                    .setTopicPath(request.getPartitionSession().getPath())
                    .setConsumerName(consumerName)
                    .setPartitionId(partitionId)
                    .setCommittedOffset(request.getCommittedOffset())
                    .setPartitionOffsets(new OffsetsRangeImpl(request.getPartitionOffsets().getStart(),
                            request.getPartitionOffsets().getEnd()))
                    .setDecompressionExecutor(decompressionExecutor)
                    .setDataEventCallback(ReaderImpl.this::handleDataReceivedEvent)
                    .setCommitFunction((offsets) -> sendCommitOffsetRequest(partitionSessionId, partitionId, offsets))
                    .setCodecRegistry(codecRegistry)
                    .build();
            partitionSessions.put(partitionSession.getId(), partitionSession);

            handleStartPartitionSessionRequest(request, partitionSession.getSessionInfo(),
                    (settings) -> sendStartPartitionSessionResponse(partitionSession, settings));
        }

        protected void onStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
            if (request.getGraceful()) {
                PartitionSessionImpl partitionSession = partitionSessions.get(request.getPartitionSessionId());
                if (partitionSession != null) {
                    logger.info("[{}] Received graceful StopPartitionSessionRequest", partitionSession.getFullId());
                    handleStopPartitionSession(request, partitionSession.getSessionInfo(),
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
                    handleCommitResponse(partitionCommittedOffset.getCommittedOffset(),
                            partitionSession.getSessionInfo());
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
                    partitionSession == null ? "unknown" : partitionSession.getPartitionId(),
                    response.getPartitionOffsets().getStart(), response.getPartitionOffsets().getEnd(),
                    response.getCommittedOffset());
        }

        private void processMessage(YdbTopic.StreamReadMessage.FromServer message) {
            if (!isWorking.get()) {
                logger.debug("[{}] processMessage called, but read session is already closed", streamId);
                return;
            }
            logger.debug("[{}] processMessage called", streamId);
            if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                reconnectCounter.set(0);
            } else {
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

        protected void closeDueToError(Status status, Throwable th) {
            logger.info("[{}] Session {} closeDueToError called", streamId, sessionId);
            if (shutdown()) {
                // Signal reader to retry
                onSessionClosed(status, th);
            }
        }

        @Override
        protected void onStop() {
            logger.debug("[{}] Session {} onStop called", streamId, sessionId);
            closePartitionSessions();
        }
    }

}
