package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.impl.GrpcStreamRetrier;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderImpl extends GrpcStreamRetrier {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private static final int DEFAULT_DECOMPRESSION_THREAD_COUNT = 4;
    private final ExecutorService defaultDecompressionExecutorService;
    private final ReadSessionFactory sessionFactory;

    private final CompletableFuture<Void> sessionReady = new CompletableFuture<>();
    private volatile ReadSession session = null;

    public ReaderImpl(TopicRpc topicRpc, ReaderSettings settings, @Nonnull CodecRegistry codecRegistry) {
        super(settings.getLogPrefix(), topicRpc.getScheduler(), settings.getErrorsHandler());

        Executor decompressionExecutor = settings.getDecompressionExecutor();
        if (decompressionExecutor != null) {
            this.defaultDecompressionExecutorService = null;
        } else {
            this.defaultDecompressionExecutorService = Executors.newFixedThreadPool(DEFAULT_DECOMPRESSION_THREAD_COUNT);
            decompressionExecutor = defaultDecompressionExecutorService;
        }
        this.sessionFactory = new ReadSessionFactory(topicRpc, settings, decompressionExecutor, codecRegistry);

        String consumerName = settings.getConsumerName();
        String readerName = settings.getReaderName();
        logger.info("Reader{} (generated id {}) created for topic(s) {} and {}",
                readerName != null ? (" '" + readerName + "'") : "",
                settings.getTopics().stream().map(t -> "\"" + t.getPath() + "\"").collect(Collectors.joining(", ")),
                consumerName != null ? (" consumer \"" + consumerName + "\"") : "without a consumer"
        );
    }

    protected abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);
    protected abstract void handleSessionStarted(String sessionId);
    protected abstract void handleCommitResponse(long committedOffset, PartitionSession partitionSession);
    protected abstract void handleStartPartitionSessionRequest(StartPartitionSessionEvent event);
    protected abstract void handleStopPartitionSession(
            YdbTopic.StreamReadMessage.StopPartitionSessionRequest request,
            PartitionSession partitionSession,
            Runnable confirmCallback);
    protected abstract void handleClosePartitionSession(PartitionSession partitionSession);

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected String getStreamName() {
        return "Reader";
    }

    @Override
    protected void onStreamReconnect() {
        session = sessionFactory.createNextSession();
        session.startAndInitialize();
    }

    protected CompletableFuture<Void> initImpl() {
        logger.info("[{}] initImpl called", id);
        if (session == null) {
            onStreamReconnect();
        } else {
            logger.warn("[{}] Init is called on this reader more than once. Nothing is done", id);
        }

        return sessionReady;
    }

    void onSessionStarted(String sessionId) {
        sessionReady.complete(null);
    }

    protected CompletableFuture<Status> updateOffsetsInTransaction(YdbTransaction transaction,
                                                                Map<String, List<PartitionOffsets>> offsets,
                                                                UpdateOffsetsInTransactionSettings settings) {
        if (!transaction.isActive()) {
            throw new IllegalArgumentException("Transaction is not active. " +
                    "Can only read topic messages in already running transactions from other services");
        }
        return session.sendUpdateOffsetsInTransaction(transaction, offsets, settings);
    }

    @Override
    protected void onShutdown(String reason) {
        session.shutdown();
        sessionReady.completeExceptionally(new RuntimeException(reason));
        if (defaultDecompressionExecutorService != null) {
            defaultDecompressionExecutorService.shutdown();
        }
    }

    private class ReadSessionFactory {
        private final TopicRpc rpc;
        private final ReaderSettings settings;
        private final Executor decompressor;
        private final CodecRegistry codecRegistry;
        private final AtomicLong sessionCounter = new AtomicLong(0);

        ReadSessionFactory(TopicRpc rpc, ReaderSettings settings, Executor decompressor, CodecRegistry codecRegistry) {
            this.rpc = rpc;
            this.settings = settings;
            this.decompressor = decompressor;
            this.codecRegistry = codecRegistry;

        }

        public ReadSession createNextSession() {
            String streamID = id + '.' + sessionCounter.incrementAndGet();
            MessageDecoder decoder = new MessageDecoder(settings.getMaxMemoryUsageBytes(), decompressor, codecRegistry);
            return new ReadSession(rpc, ReaderImpl.this, decoder, streamID, settings);
        }
    }
}
