package tech.ydb.coordination.scenario.semaphore;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.scenario.semaphore.impl.SemaphoreImpl;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.core.Status;

/**
 * A synchronous semaphore that allows multiple instances to acquire and release tokens.
 * This Semaphore is a synchronous extension of {@link AsyncSemaphore}.
 *
 * @see CompletableFuture
 * @see SemaphoreSettings
 * @see CoordinationClient
 * @see Status
 */
public interface Semaphore extends AsyncSemaphore {
    /**
     * Blocking equivalent of {@link AsyncSemaphore#newAsyncSemaphore(CoordinationClient, String, String, long)}
     *
     * @return New instance of Semaphore or an exception.
     */
    static CompletableFuture<Semaphore> newSemaphore(
            CoordinationClient client, String path, String semaphoreName, long limit) {
        return SemaphoreImpl.newSemaphore(client, path, semaphoreName, limit);
    }

    /**
     * Blocking equivalent of {@link AsyncSemaphore#deleteSemaphoreAsync(CoordinationClient, String, String, boolean)}
     *
     * @return Status of delete semaphore operation or an exception.
     */
    static Status deleteSemaphore(CoordinationClient client, String path,
                                  String semaphoreName, boolean force) {
        return SemaphoreImpl.deleteSemaphore(client, path, semaphoreName, force);
    }

    /**
     * Blocking equivalent of {@link AsyncSemaphore#acquireAsync(SemaphoreSettings)}
     *
     * @return true if tokens were acquired and false or an exception in another way.
     */
    boolean acquire(SemaphoreSettings settings);

    /**
     * Blocking equivalent of {@link AsyncSemaphore#releaseAsync()}
     *
     * @return true if tokens were released and false or an exception in another way.
     */
    boolean release();
}
