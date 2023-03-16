package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderImpl {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 0; // Inf
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int DEFAULT_DECOMPRESSION_THREAD_COUNT = 4;

    private final ReaderSettings settings;
    private final TopicRpc topicRpc;
    private final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private final AtomicLong sizeBytesToRequest;
    protected final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1);
    private final Map<Long, PartitionSession> partitionSessions = new HashMap<>();
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

    private void handleStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request) {
        final long partitionId = request.getPartitionSession().getPartitionSessionId();
        PartitionSession partitionSession = PartitionSession.newBuilder()
                .setId(partitionId)
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
        sendStartPartitionSessionResponse(partitionSession);
    }

    private void sendStartPartitionSessionResponse(PartitionSession partitionSession) {
        session.send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                .setStartPartitionSessionResponse(YdbTopic.StreamReadMessage.StartPartitionSessionResponse.newBuilder()
                        .setPartitionSessionId(partitionSession.getId())
                        // TODO: set offsets?
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

    private void handleCommitOffsetResponse(YdbTopic.StreamReadMessage.CommitOffsetResponse commitOffsetResponse) {
        for (YdbTopic.StreamReadMessage.CommitOffsetResponse.PartitionCommittedOffset partitionCommittedOffset :
                commitOffsetResponse.getPartitionsCommittedOffsetsList()) {
            PartitionSession partitionSession = partitionSessions.get(partitionCommittedOffset.getPartitionSessionId());
            if (partitionSession != null) {
                partitionSession.handleCommitResponse(partitionCommittedOffset.getCommittedOffset());
            } else {
                logger.debug("Received CommitOffsetResponse for closed session with id={}",
                        partitionCommittedOffset.getPartitionSessionId());
            }
        }
    }

    protected abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);

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
        partitionSessions.values().forEach(PartitionSession::shutdown);
        partitionSessions.clear();
        this.session = new ReadSession(topicRpc);
        initImpl();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        logger.info("Shutting down Topic Reader");
        isStopped.set(true);
        return CompletableFuture.runAsync(() -> {
            reconnectExecutor.shutdown();
            if (defaultDecompressionExecutorService != null) {
                defaultDecompressionExecutorService.shutdown();
            }
            session.finish();
        });
    }

    private void processMessage(YdbTopic.StreamReadMessage.FromServer message) {
        if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            reconnectCounter.set(0);
        } else {
            logger.error("Got non-success status in processMessage method: {}", message);
            completeSession(Status.of(StatusCode.fromProto(message.getStatus())), null);
            return;
        }

        if (message.hasInitResponse()) {
            currentSessionId = message.getInitResponse().getSessionId();
            initResultFuture.complete(null);
            logger.info("Session {} initialized", currentSessionId);
            sendReadRequest();
        } else if (message.hasStartPartitionSessionRequest()) {
            handleStartPartitionSessionRequest(message.getStartPartitionSessionRequest());
        } else if (message.hasReadResponse()) {
            handleReadResponse(message.getReadResponse());
        } else if (message.hasCommitOffsetResponse()) {
            handleCommitOffsetResponse(message.getCommitOffsetResponse());
        } else {
            logger.error("Unhandled message from server: {}", message);
        }
    }

    private void completeSession(Status status, Throwable th) {
        // This session is not working anymore
        this.session.finish();

        if (th != null) {
            logger.error("Exception in reading stream session {}: {}", currentSessionId, th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    logger.info("Reading stream session {} closed successfully", currentSessionId);
                } else {
                    logger.error("Reading stream session {} was closed unexpectedly. Shutting down the whole reader.",
                            currentSessionId);
                    shutdownImpl();
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
                shutdownImpl();
            } else {
                logger.debug("Maximum retry count ({}}) exceeded. But reader is already shut down.",
                        MAX_RECONNECT_COUNT);
            }
        } else {
            logger.warn("Retry #" + currentReconnectCounter + ". Scheduling reconnect...");
            reconnectExecutor.schedule(this::reconnect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }
}
