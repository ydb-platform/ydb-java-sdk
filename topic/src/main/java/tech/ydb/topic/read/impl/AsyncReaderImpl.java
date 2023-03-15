package tech.ydb.topic.read.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;

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
        return CompletableFuture.runAsync(() -> eventHandler.onMessages(event), handlerExecutor);
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
