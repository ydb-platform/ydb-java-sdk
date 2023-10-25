package tech.ydb.topic.read.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.events.PartitionSessionClosedEventImpl;
import tech.ydb.topic.read.impl.events.StartPartitionSessionEventImpl;
import tech.ydb.topic.read.impl.events.StopPartitionSessionEventImpl;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

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
    protected CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                eventHandler.onMessages(event);
            } catch (Exception exception) {
                String errorMessage = "Error in user DataReceivedEvent callback: " + exception;
                logger.error(errorMessage);
                shutdownImpl(errorMessage);
            }
        }, handlerExecutor);
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
            eventHandler.onStartPartitionSession(event);
        });
    }

    @Override
    protected void handleStopPartitionSession(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request,
                                              @Nullable Long partitionId, Runnable confirmCallback) {
        final long partitionSessionId = request.getPartitionSessionId();
        final long committedOffset = request.getCommittedOffset();
        final StopPartitionSessionEvent event = new StopPartitionSessionEventImpl(partitionSessionId, partitionId,
                committedOffset, confirmCallback);
        handlerExecutor.execute(() -> {
            eventHandler.onStopPartitionSession(event);
        });
    }

    @Override
    protected void handleClosePartitionSession(tech.ydb.topic.read.PartitionSession partitionSession) {
        final PartitionSessionClosedEvent event = new PartitionSessionClosedEventImpl(partitionSession);
        handlerExecutor.execute(() -> {
            eventHandler.onPartitionSessionClosed(event);
        });
    }

    @Override
    protected void handleCloseReader() {
        handlerExecutor.execute(() -> {
            eventHandler.onReaderClosed(new ReaderClosedEvent());
        });
    }

    @Override
    protected CompletableFuture<Void> shutdownImpl() {
        return super.shutdownImpl().whenComplete((res, th) -> {
            if (defaultHandlerExecutorService != null) {
                logger.debug("Shutting down default handler executor");
                defaultHandlerExecutorService.shutdown();
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return shutdownImpl();
    }
}
