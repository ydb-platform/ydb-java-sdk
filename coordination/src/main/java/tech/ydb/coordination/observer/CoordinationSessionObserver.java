package tech.ydb.coordination.observer;

import tech.ydb.coordination.SemaphoreDescription;
import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public interface CoordinationSessionObserver {

    default void onAcquireSemaphoreResult(boolean acquired, Status status) {
    }

    default void onAcquireSemaphorePending() {
    }

    default void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription, Status status) {
    }

    default void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
    }

    default void onDeleteSemaphoreResult(Status status) {
    }

    default void onCreateSemaphoreResult(Status status) {
    }

    default void onFailure(Status status) {
    }

    default void onReleaseSemaphoreResult(boolean released, Status status) {
    }

    default void onUpdateSemaphoreResult(long reqId, Status status) {
    }

    default void onSessionStarted() {
    }

    default void onPong(long pingValue) {
    }
}
