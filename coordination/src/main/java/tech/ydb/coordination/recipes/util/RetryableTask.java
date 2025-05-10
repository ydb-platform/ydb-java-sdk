package tech.ydb.coordination.recipes.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;

public class RetryableTask {
    private static final Logger logger = LoggerFactory.getLogger(RetryableTask.class);

    private final String taskName;
    private final Supplier<CompletableFuture<Status>> taskSupplier;
    private final ScheduledExecutorService executor;
    private final RetryPolicy retryPolicy;
    private final long startTime;
    private int retryCount;

    public RetryableTask(
            String taskName,
            Supplier<CompletableFuture<Status>> taskSupplier,
            ScheduledExecutorService executor,
            RetryPolicy retryPolicy
    ) {
        this.taskName = taskName;
        this.taskSupplier = taskSupplier;
        this.executor = executor;
        this.retryPolicy = retryPolicy;
        this.startTime = System.currentTimeMillis();
        this.retryCount = 0;
    }

    public CompletableFuture<Status> execute() {
        CompletableFuture<Status> result = new CompletableFuture<>();
        attemptTask(result);
        return result;
    }

    void attemptTask(CompletableFuture<Status> result) {
        try {
            taskSupplier.get().whenComplete((status, throwable) -> {
                if (throwable != null) {
                    handleFailure(result, throwable);
                } else if (status.isSuccess()) {
                    logSuccess();
                    result.complete(status);
                } else {
                    handleFailure(
                            result,
                            new RuntimeException("Operation '" + taskName + "' failed with status: " + status)
                    );
                }
            });
        } catch (Exception e) {
            handleFailure(result, e);
        }
    }

    private void handleFailure(CompletableFuture<Status> result, Throwable failure) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long delayMs = retryPolicy.nextRetryMs(retryCount, elapsedTime);

        if (delayMs >= 0) {
            retryCount++;
            logRetry(delayMs, failure);

            if (delayMs == 0) {
                executor.execute(() -> attemptTask(result));
            } else {
                executor.schedule(() -> attemptTask(result), delayMs, TimeUnit.MILLISECONDS);
            }
        } else {
            logFailure(failure);
            result.completeExceptionally(failure);
        }
    }

    private void logSuccess() {
        if (retryCount > 0) {
            logger.info("Operation '{}' succeeded after {} retries", taskName, retryCount);
        } else {
            logger.info("Operation '{}' succeeded on first attempt", taskName);
        }
    }

    private void logRetry(long delayMs, Throwable failure) {
        logger.warn(
                "Attempt {} of operation '{}' failed ({}). Retrying in {}ms",
                retryCount, taskName, failure.getMessage(), delayMs
        );
    }

    private void logFailure(Throwable failure) {
        logger.error(
                "Operation '{}' failed after {} retries. Last error: {}",
                taskName, retryCount, failure.getMessage(), failure
        );
    }
}
