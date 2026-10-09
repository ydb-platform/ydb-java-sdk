package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.impl.DebugTools;
import tech.ydb.topic.impl.SerialExecutor;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.PartitionSessionEndedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;
import tech.ydb.topic.read.impl.events.PartitionControl;
import tech.ydb.topic.read.impl.events.ReaderHandler;
import tech.ydb.topic.read.impl.events.SessionStartedEvent;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public class AsyncReaderImpl implements AsyncReader {
    private static final Logger logger = LoggerFactory.getLogger(AsyncReaderImpl.class);

    private final String debugId;
    private final LazyExecutor processor;
    private final LazyExecutor decompressor;
    private final SerialExecutor controlEventsExecutor;
    private final ReadEventHandler eventHandler;
    private final ReadConfig config;
    private final Impl impl;

    private final CompletableFuture<Void> initFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();

    public AsyncReaderImpl(TopicRpc topicRpc,
                           ReaderSettings settings,
                           ReadEventHandlersSettings handlersSettings,
                           @Nonnull CodecRegistry codecRegistry) {
        this.debugId = DebugTools.createDebugId(settings.getLogPrefix());
        this.eventHandler = handlersSettings.getEventHandler();
        this.processor = new LazyExecutor("reader[" + debugId + "]-handler", handlersSettings.getExecutor());
        this.decompressor = new LazyExecutor("reader[" + debugId + "]-decoder", settings.getDecompressionExecutor());
        this.controlEventsExecutor = new SerialExecutor(processor);

        this.config = new ReadConfig(codecRegistry, controlEventsExecutor, processor, decompressor, settings);

        ReadSession.Handler handler = (eventHandler instanceof ReaderHandler)
                ? new AsyncHandlerWithControl((ReaderHandler) eventHandler) : new AsyncHandler();
        this.impl = new Impl(topicRpc, debugId, settings, config, handler);

        String readerName = settings.getReaderName();
        String consumerName = settings.getConsumerName();
        logger.info("[{}] AsyncReader{} created for topic(s) {} and {}",
                debugId,
                readerName != null ? (" '" + readerName + "'") : "",
                settings.getTopics().stream().map(t -> "\"" + t.getPath() + "\"").collect(Collectors.joining(", ")),
                consumerName != null ? (" consumer \"" + consumerName + "\"") : "without a consumer"
        );
    }

    @Override
    public CompletableFuture<Void> init() {
        impl.start();
        return initFuture;
    }

    @Override
    public CompletableFuture<Status> updateOffsetsInTransaction(YdbTransaction transaction,
            Map<String, List<PartitionOffsets>> offsets, UpdateOffsetsInTransactionSettings settings) {
        return impl.updateOffsetsInTransaction(transaction, offsets, settings);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        if (!impl.close()) {
            // implicit closing because stream will never call onClose
            close(Status.SUCCESS.withIssues(Issue.of("Closed by client", Issue.Severity.INFO)));
        }
        return shutdownFuture;
    }

    private void close(Status status) {
        if (shutdownFuture.isDone()) {
            return;
        }

        controlEventsExecutor.execute(() -> {
            try {
                eventHandler.onReaderClosed(new ReaderClosedEvent());
            } catch (Throwable th) {
                logger.error("[{}] onReaderClosed finished with exception", th);
                throw th;
            }
        });

        // stop decompressong
        decompressor.close();
        // wait while processer finished all tasks
        processor.close();
        initFuture.completeExceptionally(new RuntimeException("Reader closed with " + status));
        shutdownFuture.complete(null);
    }

    private void failSession(Throwable th, String callbackName) {
        String errorMessage = "Unhandled throwable in " + callbackName + " user callback: " + th.getMessage();
        logger.error(errorMessage, th);
        impl.fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th, Issue.of(errorMessage, Issue.Severity.ERROR)));
    }

    private class Impl extends ReaderImpl {
        Impl(TopicRpc rpc, String id, ReaderSettings settings, ReadConfig config, ReadSession.Handler handler) {
            super(rpc, id, settings, config, handler);
        }

        @Override
        public void onSessionStarted(String sessionId) {
            initFuture.complete(null);
            controlEventsExecutor.execute(() -> {
                try {
                    eventHandler.onSessionStarted(new SessionStartedEvent(sessionId));
                } catch (Throwable th) {
                    failSession(th, "onSessionStarted");
                }
            });
        }

        @Override
        public void onReaderClosed(Status status) {
            AsyncReaderImpl.this.close(status);
        }
    }

    private class AsyncHandler implements ReadSession.Handler {
        @Override
        public void onData(DataReceivedEventImpl event) {
            PartitionControl control = event.getPartitionControl();
            try {
                config.getMetrics().reportDelivered(event.getMessages().size(), event.getPartitionSession().getPath());
                eventHandler.onMessages(event);
            } catch (Throwable th) {
                failSession(th, "onMessages");
            } finally {
                control.confirmProcessedRange(event.getRangeToCommit());
            }
        }

        @Override
        public void onCommitAck(CommitOffsetAcknowledgementEvent event) {
            try {
                eventHandler.onCommitResponse(event);
            } catch (Throwable th) {
                failSession(th, "onCommitResponse");
            }
        }

        @Override
        public void onPartitionStarted(StartPartitionSessionEvent event) {
            try {
                eventHandler.onStartPartitionSession(event);
            } catch (Throwable th) {
                failSession(th, "onStartPartitionSession");
            }
        }

        @Override
        public void onPartitionStopped(StopPartitionSessionEvent event) {
            try {
                eventHandler.onStopPartitionSession(event);
            } catch (Throwable th) {
                failSession(th, "onStopPartitionSession");
            }
        }

        @Override
        public void onPartitionClosed(PartitionSessionClosedEvent event) {
            try {
                eventHandler.onPartitionSessionClosed(event);
            } catch (Throwable th) {
                failSession(th, "onPartitionSessionClosed");
            }
        }

        @Override
        public void onPartitionEnded(PartitionSessionEndedEvent event) {
            try {
                eventHandler.onPartitionSessionEnded(event);
            } catch (Throwable th) {
                failSession(th, "onPartitionSessionEnded");
            }
        }
    }

    private class AsyncHandlerWithControl extends AsyncHandler {
        private final ReaderHandler readerHandler;

        AsyncHandlerWithControl(ReaderHandler readerHandler) {
            this.readerHandler = readerHandler;
        }

        @Override
        public void onData(DataReceivedEventImpl event) {
            try {
                readerHandler.onMessagesWithControl(event);
            } catch (Throwable th) {
                failSession(th, "onMessages");
                throw th;
            }
        }
    }
}
