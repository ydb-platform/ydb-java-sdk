package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;


/**
 * @author Aleksandr Gorshenin
 */
public interface SemaphoreLease {
    String getSemaphoreName();

    CoordinationSession getSession();

    CompletableFuture<Void> release();
}
