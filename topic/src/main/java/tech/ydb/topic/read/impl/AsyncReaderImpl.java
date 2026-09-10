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
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.ReaderImpl.Releaser;
import tech.ydb.topic.read.impl.events.CommitOffsetAcknowledgementEventImpl;
import tech.ydb.topic.read.impl.events.PartitionSessionClosedEventImpl;
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
    private final ReadEventHandler eventHandler;
    private final SerialExecutor controlEventsExecutor;
    private final ReadConfig config;
    private final ReaderImpl impl;

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

        this.config = new ReadConfig(codecRegistry, processor, decompressor, settings);
        this.impl = new ReaderImpl(topicRpc, debugId, settings, config, new AsyncHandler());

        String readerName = settings.getReaderName();
        String consumerName = settings.getConsumerName();
        logger.info("Reader{} (generated id {}) created for topic(s) {} and {}",
                readerName != null ? (" '" + readerName + "'") : "",
                debugId,
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

    protected CompletableFuture<Void> handleReaderClosed() {
        return CompletableFuture.runAsync(() -> {
            try {
                eventHandler.onReaderClosed(new ReaderClosedEvent());
            } catch (Throwable th) {
                failSession(th, "onReaderClosed");
                throw th;
            }
        }, controlEventsExecutor);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        impl.close();
        return shutdownFuture;
    }

    private void close() {
        decompressor.close();
        processor.close();
        shutdownFuture.complete(null);
    }

    private void failSession(Throwable th, String callbackName) {
        String errorMessage = "Unhandled throwable in " + callbackName + " user callback: " + th.getMessage();
        logger.error(errorMessage, th);
        impl.fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th, Issue.of(errorMessage, Issue.Severity.ERROR)));
    }

    private class AsyncHandler implements ReaderImpl.Handler {
        @Override
        public  void handleSessionStarted(String sessionId) {
            initFuture.complete(null);
            try {
                eventHandler.onSessionStarted(new SessionStartedEvent(sessionId));
            } catch (Throwable th) {
                failSession(th, "onSessionStarted");
            }
        }

        @Override
        public void handleReaderClosed(Status status) {
            try {
                eventHandler.onReaderClosed(new ReaderClosedEvent());
            } catch (Throwable th) {
                failSession(th, "onReaderClosed");
            } finally {
                close();
            }
        }

        @Override
        public void handleDataReceivedEvent(Releaser releaser, DataReceivedEvent event) {
            try {
                int messagesCount = event.getMessages().size();
                long offsetStart = event.getMessages().get(0).getOffset();
                long offsetEnd = event.getMessages().get(event.getMessages().size() - 1).getOffset();
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) is about "
                        + "to be called...", debugId, messagesCount, offsetStart, offsetEnd);
                eventHandler.onMessages(event);
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) "
                        + "successfully finished", debugId, messagesCount, offsetStart, offsetEnd);
            } catch (Throwable th) {
                failSession(th, "onMessages");
                throw th;
            } finally {
                releaser.releaseRange(event.getPartitionSession(), event.getRangeToCommit());
            }
        }

        @Override
        public void handleCommitResponse(long committedOffset, PartitionSession partition) {
            processor.execute(() -> {
                try {
                    eventHandler.onCommitResponse(new CommitOffsetAcknowledgementEventImpl(partition, committedOffset));
                } catch (Throwable th) {
                    failSession(th, "onCommitResponse");
                    throw th;
                }
            });
        }

        @Override
        public void handleStartPartitionSessionRequest(StartPartitionSessionEvent event) {
            controlEventsExecutor.execute(() -> {
                try {
                    eventHandler.onStartPartitionSession(event);
                } catch (Throwable th) {
                    failSession(th, "onStartPartitionSession");
                    throw th;
                }
            });
        }

        @Override
        public void handleStopPartitionSession(StopPartitionSessionEvent event) {
            controlEventsExecutor.execute(() -> {
                try {
                    eventHandler.onStopPartitionSession(event);
                } catch (Throwable th) {
                    failSession(th, "onStopPartitionSession");
                    throw th;
                }
            });
        }

        @Override
        public void handleClosePartitionSession(PartitionSession partition) {
            controlEventsExecutor.execute(() -> {
                try {
                    eventHandler.onPartitionSessionClosed(new PartitionSessionClosedEventImpl(partition));
                } catch (Throwable th) {
                    failSession(th, "onPartitionSessionClosed");
                    throw th;
                }
            });
        }
    }
}
