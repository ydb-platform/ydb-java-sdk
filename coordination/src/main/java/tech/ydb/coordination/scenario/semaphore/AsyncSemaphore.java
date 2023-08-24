package tech.ydb.coordination.scenario.semaphore;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.scenario.semaphore.impl.AsyncSemaphoreImpl;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.core.Status;

/**
 * An asynchronous semaphore that allows multiple instances to acquire and release tokens.
 * Tokens can be acquired and released asynchronously using CompletableFuture.
 *
 * @see CompletableFuture
 * @see SemaphoreSettings
 * @see CoordinationClient
 * @see Status
 */
public interface AsyncSemaphore {
    /**
     * Creates a new Semaphore using the specified coordination client, path, semaphore name, and limit.
     *
     * @param client the coordination client for communication with nodes.
     * @param path the path for the Coordination Node. There might be no node on this path, AsyncSemaphore will try to
     *             create a new node.
     * @param semaphoreName the name of the semaphore on this Coordination Node (different nodes could have different
     *                      semaphores with the same name).
     * @param limit the maximum number of tokens for the semaphore.
     * @param createNode create node before semaphore creation or not
     * @return a CompletableFuture that completes with a new AsyncSemaphore instance or an exception in case of the bad
     * connection and other coordination problems.
     */
    static CompletableFuture<AsyncSemaphore> newAsyncSemaphore(
            CoordinationClient client, String path, String semaphoreName, long limit, boolean createNode) {
        return AsyncSemaphoreImpl.newAsyncSemaphore(client, path, semaphoreName, limit, createNode);
    }

    /**
     * Asynchronously deletes a semaphore with the specified coordination client, path, and semaphore name.
     *
     * @param client the coordination client for communication with nodes.
     * @param path the path for the Coordination Node.
     * @param semaphoreName the name of the semaphore.
     * @param force without this parameter, semaphore wouldn't be deleted in case of a non-empty query.
     * @return a CompletableFuture that completes with the status of the delete operation or an exception.
     */

    static CompletableFuture<Status> deleteSemaphoreAsync(CoordinationClient client, String path,
                                                          String semaphoreName, boolean force) {
        return AsyncSemaphoreImpl.deleteSemaphoreAsync(client, path, semaphoreName, force);
    }

    /**
     * Asynchronously acquires a permit with the specified settings (number of tokens, etc.). You can call
     * acquireAsync several times, but only if each new call will demand fewer tokens than you obey now. It means
     * that you could release part of tokens (what you cannot do with {@link AsyncSemaphore#releaseAsync()}).
     *
     * @param settings the settings for acquiring the permit.
     * @return a CompletableFuture that completes with a boolean value indicating success or failure. Also, a
     * CompletableFuture could complete with exception if there is a connection problem or inconsistency state.
     */
    CompletableFuture<Boolean> acquireAsync(SemaphoreSettings settings);

    /**
     * Asynchronously releases a previously acquired tokens.
     *
     * @return a CompletableFuture that completes with a boolean value indicating success or failure. Also, a
     * CompletableFuture could complete with exception if there is a connection problem or inconsistency state.
     */
    CompletableFuture<Boolean> releaseAsync();

    /**
     * Checks if a permit is currently acquired.
     *
     * @return true if a permit is acquired, false otherwise.
     */
    boolean isAcquired();
}
