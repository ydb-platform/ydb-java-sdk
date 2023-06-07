package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;

/**
 * @author Nikolay Perfilov
 */
public class SyncReaderImpl extends ReaderImpl implements SyncReader {
    private static final Logger logger = LoggerFactory.getLogger(SyncReaderImpl.class);

    private static final int DEFAULT_HANDLER_THREAD_COUNT = 1;
    private static final int POLL_INTERVAL_SECONDS = 5;
    private static final int MAX_MESSAGES_IN_BUFFER = 20;

    BlockingQueue<MessageWrapper> messageBuffer = new ArrayBlockingQueue<>(MAX_MESSAGES_IN_BUFFER);
    private final ExecutorService handlerExecutor;

    public SyncReaderImpl(TopicRpc topicRpc, ReaderSettings settings) {
        super(topicRpc, settings);

        this.handlerExecutor = Executors.newFixedThreadPool(DEFAULT_HANDLER_THREAD_COUNT);
    }

    private static class MessageWrapper {
        private final Message message;
        private final CompletableFuture<Void> future;

        private MessageWrapper(Message message, CompletableFuture<Void> future) {
            this.message = message;
            this.future = future;
        }
    }

    @Override
    public void init() {
        initImpl();
    }

    @Override
    public void initAndWait() {
        initImpl().join();
    }

    @Override
    @Nullable
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        if (isStopped.get()) {
            throw new RuntimeException("Reader was stopped");
        }
        MessageWrapper wrapper = messageBuffer.poll(timeout, unit);
        if (wrapper != null) {
            wrapper.future.complete(null);
            return wrapper.message;
        } else {
            return null;
        }
    }

    @Override
    public Message receive() throws InterruptedException {
        Message result;
        // Poll to prevent infinite wait in case if reader was stopped
        do {
            result = receive(POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } while (result == null);
        return result;
    }

    @Override
    protected CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
        // Completes when all messages from this event are read by user
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        handlerExecutor.execute(() -> {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            event.getMessages().forEach(message -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                futures.add(future);
                try {
                    messageBuffer.put(new MessageWrapper(message, future));
                } catch (InterruptedException e) {
                    logger.warn("Putting message to message buffer was interrupted: ", e);
                    future.completeExceptionally(e);
                }
            });
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                    .thenRun(() -> resultFuture.complete(null))
                    .exceptionally(throwable -> {
                        resultFuture.completeExceptionally(throwable);
                        return null;
                    });
        });
        return resultFuture;
    }

    @Override
    protected void handleStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request) {
        sendStartPartitionSessionResponse(request, null);
    }

    @Override
    protected void handleStopPartitionSession(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        sendStopPartitionSessionResponse(request.getPartitionSessionId());
    }

    @Override
    protected void handleClosePartitionSession(PartitionSession partitionSession) {
        logger.debug("ClosePartitionSession event received. Ignoring.");
    }

    @Override
    protected void handleCloseReader() {
        logger.debug("CloseReader event received. Ignoring.");
    }

    @Override
    protected CompletableFuture<Void> shutdownImpl() {
        return super.shutdownImpl().whenComplete((res, th) -> {
            logger.debug("Shutting down default handler executor");
            handlerExecutor.shutdown();
        });
    }

    @Override
    public void shutdown() {
        shutdownImpl().join();
    }
}
