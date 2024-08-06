package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.events.CommitOffsetAcknowledgementEventImpl;
import tech.ydb.topic.read.impl.events.PartitionSessionClosedEventImpl;
import tech.ydb.topic.read.impl.events.StartPartitionSessionEventImpl;
import tech.ydb.topic.read.impl.events.StopPartitionSessionEventImpl;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public class AsyncReaderImpl extends ReaderImpl implements AsyncReader {
    private static final Logger logger = LoggerFactory.getLogger(AsyncReaderImpl.class);
    private static final int DEFAULT_HANDLER_THREAD_COUNT = 4;

    private final Executor handlerExecutor;
    private final ExecutorService defaultHandlerExecutorService;
    private final ReadEventHandler eventHandler;

    public AsyncReaderImpl(TopicRpc topicRpc, ReaderSettings settings, ReadEventHandlersSettings handlersSettings) {
        super(topicRpc, settings);
        this.eventHandler = handlersSettings.getEventHandler();

        if (handlersSettings.getExecutor() != null) {
            logger.debug("Using handler executor provided by user");
            this.defaultHandlerExecutorService = null;
            this.handlerExecutor = handlersSettings.getExecutor();
        } else {
            logger.debug("Using default handler executor");
            this.defaultHandlerExecutorService = Executors.newFixedThreadPool(DEFAULT_HANDLER_THREAD_COUNT);
            this.handlerExecutor = defaultHandlerExecutorService;
        }
    }

    @Override
    public CompletableFuture<Void> init() {
        return initImpl();
    }

    @Override
    public CompletableFuture<Status> updateOffsetsInTransaction(YdbTransaction transaction,
                                                                Map<String, List<PartitionOffsets>> offsets,
                                                                UpdateOffsetsInTransactionSettings settings) {
        if (!transaction.isActive()) {
            throw new IllegalArgumentException("Transaction is not active. " +
                    "Can only read topic messages in already running transactions from other services");
        }
        return sendUpdateOffsetsInTransaction(transaction, offsets, settings);
    }

    @Override
    protected CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                eventHandler.onMessages(event);
            } catch (Exception exception) {
                logUserExceptionAndStopWorking(exception, "onMessages");
                throw exception;
            }
        }, handlerExecutor);
    }

    @Override
    protected void handleCommitResponse(long committedOffset, PartitionSession partitionSession) {
        handlerExecutor.execute(() -> {
            CommitOffsetAcknowledgementEvent event = new CommitOffsetAcknowledgementEventImpl(partitionSession,
                    committedOffset);
            try {
                eventHandler.onCommitResponse(event);
            } catch (Exception exception) {
                logUserExceptionAndStopWorking(exception, "onCommitResponse");
                throw exception;
            }
        });
    }

    @Override
    protected void handleStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request,
                                                      PartitionSession partitionSession,
                                                      Consumer<StartPartitionSessionSettings> confirmCallback) {
        handlerExecutor.execute(() -> {
            YdbTopic.OffsetsRange offsetsRange = request.getPartitionOffsets();
            StartPartitionSessionEvent event = new StartPartitionSessionEventImpl(
                    partitionSession,
                    request.getCommittedOffset(),
                    new OffsetsRangeImpl(offsetsRange.getStart(), offsetsRange.getEnd()),
                    confirmCallback
            );
            try {
                eventHandler.onStartPartitionSession(event);
            } catch (Exception exception) {
                logUserExceptionAndStopWorking(exception, "onStartPartitionSession");
                throw exception;
            }
        });
    }

    @Override
    protected void handleStopPartitionSession(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request,
                                              PartitionSession partitionSession, Runnable confirmCallback) {
        final long committedOffset = request.getCommittedOffset();
        final StopPartitionSessionEvent event = new StopPartitionSessionEventImpl(partitionSession, committedOffset,
                confirmCallback);
        handlerExecutor.execute(() -> {
            try {
                eventHandler.onStopPartitionSession(event);
            } catch (Exception exception) {
                logUserExceptionAndStopWorking(exception, "onStopPartitionSession");
                throw exception;
            }
        });
    }

    @Override
    protected void handleClosePartitionSession(tech.ydb.topic.read.PartitionSession partitionSession) {
        final PartitionSessionClosedEvent event = new PartitionSessionClosedEventImpl(partitionSession);
        handlerExecutor.execute(() -> {
            try {
                eventHandler.onPartitionSessionClosed(event);
            } catch (Exception exception) {
                logUserExceptionAndStopWorking(exception, "onPartitionSessionClosed");
                throw exception;
            }
        });
    }

    protected void handleReaderClosed() {
        handlerExecutor.execute(() -> {
            try {
                eventHandler.onReaderClosed(new ReaderClosedEvent());
            } catch (Exception exception) {
                logUserExceptionAndStopWorking(exception, "onReaderClosed");
                throw exception;
            }
        });
    }

    @Override
    protected void onShutdown(String reason) {
        super.onShutdown(reason);
        handleReaderClosed();
        if (defaultHandlerExecutorService != null) {
            logger.debug("Shutting down default handler executor");
            defaultHandlerExecutorService.shutdown();
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return shutdownImpl();
    }

    private void logUserExceptionAndStopWorking(Exception exception, String callbackName) {
        String errorMessage = "Unhandled exception in " + callbackName + " user callback: " + exception;
        logger.error(errorMessage);
        shutdownImpl(errorMessage).join();
    }
}
