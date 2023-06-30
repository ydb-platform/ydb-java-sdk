package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderImpl {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 0; // Inf
    private static final int EXP_BACKOFF_BASE_MS = 256;
    private static final int EXP_BACKOFF_CEILING_MS = 40000; // 40 sec (max delays would be 40-80 sec)
    private static final int EXP_BACKOFF_MAX_POWER = 7;
    private static final int DEFAULT_DECOMPRESSION_THREAD_COUNT = 4;

    private final ReaderSettings settings;
    private final TopicRpc topicRpc;
    private final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private final AtomicLong sizeBytesToRequest;
    protected final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final Map<Long, PartitionSession> partitionSessions = new ConcurrentHashMap<>();
    private final Executor decompressionExecutor;
    private final ExecutorService defaultDecompressionExecutorService;
    private CompletableFuture<Void> initResultFuture = new CompletableFuture<>();
    private ReadSession session;
    private String currentSessionId;

    public ReaderImpl(TopicRpc topicRpc, ReaderSettings settings) {
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new ReadSession(topicRpc);
        this.sizeBytesToRequest = new AtomicLong(settings.getMaxMemoryUsageBytes());
        if (settings.getDecompressionExecutor() != null) {
            this.defaultDecompressionExecutorService = null;
            this.decompressionExecutor = settings.getDecompressionExecutor();
        } else {
            this.defaultDecompressionExecutorService = Executors.newFixedThreadPool(DEFAULT_DECOMPRESSION_THREAD_COUNT);
            this.decompressionExecutor = defaultDecompressionExecutorService;
        }
    }

    protected CompletableFuture<Void> initImpl() {
        logger.debug("initImpl started");
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
        if (currentSizeBytesToRequest == 0) {
            return;
        }

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
        logger.info("Sending StartPartitionSessionResponse for partition session {} (partition {})",
                partitionSession.getId(), partitionSession.getPartitionId());
        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setStartPartitionSessionResponse(responseBuilder.build())
                .build());
    }

    protected void sendStopPartitionSessionResponse(long partitionSessionId) {

        PartitionSession partitionSession = partitionSessions.get(partitionSessionId);
        if (partitionSession != null) {
            logger.info("Sending StartPartitionSessionResponse for partition session {} (partition {})",
                    partitionSessionId, partitionSession.getPartitionId());
        } else {
            logger.warn("Sending StartPartitionSessionResponse for partition session {}, " +
                    "but have no such partition session running", partitionSessionId);
        }
        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setStopPartitionSessionResponse(YdbTopic.StreamReadMessage.StopPartitionSessionResponse.newBuilder()
                        .setPartitionSessionId(partitionSessionId)
                        .build())
                .build());
    }

    private void handleReadResponse(YdbTopic.StreamReadMessage.ReadResponse readResponse) {
        final long responseBytesSize = readResponse.getBytesSize();
        List<CompletableFuture<Void>> batchReadFutures = new ArrayList<>();
        readResponse.getPartitionDataList().forEach((YdbTopic.StreamReadMessage.ReadResponse.PartitionData data) -> {
            long partitionId = data.getPartitionSessionId();
            PartitionSession partitionSession = partitionSessions.get(partitionId);
            if (partitionSession != null) {
                // Completes when all messages from a batch are read by user
                CompletableFuture<Void> readFuture = partitionSession.addBatches(data.getBatchesList());
                batchReadFutures.add(readFuture);
            } else {
                logger.debug("Received PartitionData for unknown(closed?) PartitionSessionId={}", partitionId);
            }
        });
        CompletableFuture.allOf(batchReadFutures.toArray(new CompletableFuture<?>[0]))
                .whenComplete((res, th) -> {
                    if (th != null) {
                        logger.error("Exception while waiting for batches to be read:", th);
                    }
                    this.sizeBytesToRequest.addAndGet(responseBytesSize);
                    sendReadRequest();
                });
    }

    protected void handleCommitOffsetResponse(YdbTopic.StreamReadMessage.CommitOffsetResponse commitOffsetResponse) {
        for (YdbTopic.StreamReadMessage.CommitOffsetResponse.PartitionCommittedOffset partitionCommittedOffset :
                commitOffsetResponse.getPartitionsCommittedOffsetsList()) {
            PartitionSession partitionSession = partitionSessions.get(partitionCommittedOffset.getPartitionSessionId());
            if (partitionSession != null) {
                partitionSession.handleCommitResponse(partitionCommittedOffset.getCommittedOffset());
            } else {
                logger.debug("Received CommitOffsetResponse for closed partition session with id={}",
                        partitionCommittedOffset.getPartitionSessionId());
            }
        }
    }

    protected void handleStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        if (request.getGraceful()) {
            PartitionSession partitionSession = partitionSessions.get(request.getPartitionSessionId());
            if (partitionSession != null) {
                logger.info("Received graceful StopPartitionSessionRequest for partition session {} (partition {})",
                        partitionSession.getId(), partitionSession.getPartitionId());
            } else {
                logger.warn("Received graceful StopPartitionSessionRequest for partition session {}, " +
                                "but have no such partition session running", request.getPartitionSessionId());
            }
            handleStopPartitionSession(request);
        } else {
            PartitionSession partitionSession = partitionSessions.remove(request.getPartitionSessionId());
            if (partitionSession != null) {
                logger.info("Received force StopPartitionSessionRequest for partition session {} (partition {})",
                        partitionSession.getId(), partitionSession.getPartitionId());
                closePartitionSession(partitionSession);
            } else {
                logger.warn("Received force StopPartitionSessionRequest for partition session {}, " +
                        "but have no such partition session running", request.getPartitionSessionId());
            }
        }
    }

    protected abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);
    protected abstract void handleStartPartitionSessionRequest(
            YdbTopic.StreamReadMessage.StartPartitionSessionRequest request);
    protected abstract void handleStopPartitionSession(
            YdbTopic.StreamReadMessage.StopPartitionSessionRequest request);
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

    private void reconnect() {
        logger.info("Reconnect #{} started. Creating new ReadSession", reconnectCounter.get());
        this.session = new ReadSession(topicRpc);
        initImpl();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        logger.info("Shutting down Topic Reader");
        isStopped.set(true);
        return CompletableFuture.runAsync(() -> {
            closePartitionSessions();
            handleCloseReader();
            if (defaultDecompressionExecutorService != null) {
                defaultDecompressionExecutorService.shutdown();
            }
            session.shutdown();
        });
    }

    private void shutdownImpl(String reason) {
        if (!initResultFuture.isDone()) {
            initImpl().completeExceptionally(new RuntimeException(reason));
        }
        shutdownImpl();
    }

    private void closeSessionsAndScheduleReconnect(int currentReconnectCounter) {
        closePartitionSessions();
        int delayMs = currentReconnectCounter <= EXP_BACKOFF_MAX_POWER
                ? EXP_BACKOFF_BASE_MS * (1 << currentReconnectCounter)
                : EXP_BACKOFF_CEILING_MS;
        // Add jitter
        delayMs = delayMs + ThreadLocalRandom.current().nextInt(delayMs);
        logger.warn("Retry #{}. Scheduling reconnect in {}ms...", currentReconnectCounter, delayMs);
        topicRpc.getScheduler().schedule(this::reconnect, delayMs, TimeUnit.MILLISECONDS);
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
        logger.trace("processMessage called");
        if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            reconnectCounter.set(0);
        } else {
            logger.error("Got non-success status in processMessage method: {}", message);
            completeSession(Status.of(StatusCode.fromProto(message.getStatus()))
                    .withIssues(Issue.of("Got a message with non-success status: " + message,
                            Issue.Severity.ERROR)), null);
            return;
        }

        if (message.hasInitResponse()) {
            currentSessionId = message.getInitResponse().getSessionId();
            initResultFuture.complete(null);
            logger.info("Session {} initialized", currentSessionId);
            sendReadRequest();
        } else if (message.hasStartPartitionSessionRequest()) {
            YdbTopic.StreamReadMessage.StartPartitionSessionRequest request = message.getStartPartitionSessionRequest();
            logger.info("Received StartPartitionSessionRequest: partition session {} (partition {})",
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
            logger.info("Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                            " Partition offsets: [{}, {}). Committed offset: {}",
                    response.getPartitionSessionId(),
                    partitionSession == null ? "unknown" : partitionSession.getPartitionId(),
                    response.getPartitionOffsets().getStart(), response.getPartitionOffsets().getEnd(),
                    response.getCommittedOffset());
        } else if (message.hasUpdateTokenResponse()) {
            logger.debug("Received UpdateTokenResponse");
        } else {
            logger.error("Unhandled message from server: {}", message);
        }
    }

    private void completeSession(Status status, Throwable th) {
        logger.info("CompleteSession called");
        // This session is not working anymore
        this.session.stop();

        if (th != null) {
            logger.error("Exception in reading stream session {}: {}", currentSessionId, th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    logger.info("Reading stream session {} closed successfully", currentSessionId);
                } else {
                    logger.error("Reading stream session {} was closed unexpectedly. Shutting down the whole reader.",
                            currentSessionId);
                    shutdownImpl("Reading stream session " + currentSessionId
                            + " was closed unexpectedly. Shutting down reader.");
                }
                return;
            }
            logger.error("Error in reading stream session {}: {}", (currentSessionId != null ? currentSessionId : ""),
                    status);
        }

        if (isStopped.get()) {
            return;
        }
        int currentReconnectCounter = reconnectCounter.incrementAndGet();
        if (MAX_RECONNECT_COUNT > 0 && currentReconnectCounter > MAX_RECONNECT_COUNT) {
            if (isStopped.compareAndSet(false, true)) {
                logger.error("Maximum retry count ({}}) exceeded. Shutting down reader.", MAX_RECONNECT_COUNT);
                shutdownImpl("Maximum retry count (" + MAX_RECONNECT_COUNT
                        + ") exceeded. Shutting down reader with error: " + (th != null ? th : status));
            } else {
                logger.debug("Maximum retry count ({}}) exceeded. But reader is already shut down.",
                        MAX_RECONNECT_COUNT);
            }
        } else {
            closeSessionsAndScheduleReconnect(currentReconnectCounter);
        }
    }
}
