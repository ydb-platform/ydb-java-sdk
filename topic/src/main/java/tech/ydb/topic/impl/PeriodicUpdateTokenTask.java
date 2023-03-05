package tech.ydb.topic.impl;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.grpc.netty.shaded.io.netty.util.Timeout;
import io.grpc.netty.shaded.io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.utils.Async;
import tech.ydb.topic.TopicRpc;

/**
 * @author Nikolay Perfilov
 */
public class PeriodicUpdateTokenTask implements TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(PeriodicUpdateTokenTask.class);
    private static final long UPDATE_TOKEN_PERIOD_SECONDS = 3600;

    private final TopicRpc topicRpc;
    private final Consumer<String> sendTokenCallback;
    private Timeout currentSchedule = null;
    private String previousToken = null;

    public PeriodicUpdateTokenTask(TopicRpc topicRpc, Consumer<String> sendTokenCallback) {
        this.topicRpc = topicRpc;
        this.sendTokenCallback = sendTokenCallback;
    }

    public void stop() {
        logger.info("stopping PeriodicUpdateTokenTask");
        if (currentSchedule != null) {
            currentSchedule.cancel();
            currentSchedule = null;
        }
    }

    public void start() {
        logger.info("starting PeriodicUpdateTokenTask");
        previousToken = topicRpc.getCallOptions().getAuthority();
        // do not check token at the start, just schedule next
        scheduleNextTokenCheck();
    }

    @Override
    public void run(Timeout timeout) {
        if (timeout.isCancelled()) {
            return;
        }

        updateTokenIfNeeded();
        scheduleNextTokenCheck();
    }

    private void updateTokenIfNeeded() {
        String token = topicRpc.getCallOptions().getAuthority();
        if (token == null || token.equals(previousToken)) {
            return;
        }
        previousToken = token;
        sendTokenCallback.accept(token);
    }

    private void scheduleNextTokenCheck() {
        currentSchedule = Async.runAfter(this, UPDATE_TOKEN_PERIOD_SECONDS, TimeUnit.SECONDS);
    }
}
