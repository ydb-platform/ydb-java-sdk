package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface SemaphoreLease {

    CoordinationSession getSession();

    String getSemaphoreName();

    CompletableFuture<Status> getStatusFuture();

    CompletableFuture<Boolean> release();

    default boolean isValid() {
        return !getStatusFuture().isDone();
    }
}
