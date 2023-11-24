package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;


/**
 *
 * @author Aleksandr Gorshenin
 */
public interface SemaphoreLease extends AutoCloseable {

    String getSemaphoreName();

    CoordinationSession getSession();

    boolean isActive();

    CompletableFuture<Void> release();

    @Override
    default void close() {
        release().join();
    }
}
