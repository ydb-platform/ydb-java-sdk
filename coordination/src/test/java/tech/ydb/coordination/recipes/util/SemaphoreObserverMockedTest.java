package tech.ydb.coordination.recipes.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.mockito.Mockito;
import tech.ydb.common.retry.RetryForever;
import tech.ydb.coordination.CoordinationSessionBaseMockedTest;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public class SemaphoreObserverMockedTest extends CoordinationSessionBaseMockedTest {

    @Test
    public void successTest() {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());
        sessionMock.describeSemaphore()
                .thenReturn(CompletableFuture.completedFuture(Mockito.mock(Result.class)));

        Result<SemaphoreWatcher> watchResultMock = Mockito.mock(Result.class);
        Mockito.when(watchResultMock.isSuccess()).thenReturn(true);
        Mockito.when(watchResultMock.getStatus()).thenReturn(Status.SUCCESS);
        SemaphoreWatcher semaphoreWatcher = Mockito.mock(SemaphoreWatcher.class);
        Mockito.when(semaphoreWatcher.getDescription()).thenReturn(Mockito.mock(SemaphoreDescription.class));
        Result<SemaphoreChangedEvent> watchResultEventMock = Mockito.mock(Result.class);
        CompletableFuture<Result<SemaphoreChangedEvent>> watchedEvent =
                CompletableFuture.completedFuture(watchResultEventMock);
        Mockito.when(semaphoreWatcher.getChangedFuture()).thenReturn(watchedEvent);
        Mockito.when(watchResultMock.getValue()).thenReturn(semaphoreWatcher);

        sessionMock.watchSemaphore()
                .thenReturn(CompletableFuture.completedFuture(watchResultMock));

        SemaphoreObserver observer = new SemaphoreObserver(
                getCoordinationSession(),
                "observable_semaphore",
                WatchSemaphoreMode.WATCH_DATA_AND_OWNERS,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                new RetryForever(100),
                Executors.newSingleThreadScheduledExecutor()
        );
        observer.start();
        sessionMock.connected();
        observer.getCachedData();
        observer.close();
    }

    @Test
    public void start_alreadyStarted_Error() {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());
        sessionMock.describeSemaphore()
                .thenReturn(CompletableFuture.completedFuture(Mockito.mock(Result.class)));

        Result<SemaphoreWatcher> watchResultMock = Mockito.mock(Result.class);
        Mockito.when(watchResultMock.isSuccess()).thenReturn(true);
        Mockito.when(watchResultMock.getStatus()).thenReturn(Status.SUCCESS);
        SemaphoreWatcher semaphoreWatcher = Mockito.mock(SemaphoreWatcher.class);
        Mockito.when(semaphoreWatcher.getDescription()).thenReturn(Mockito.mock(SemaphoreDescription.class));
        Result<SemaphoreChangedEvent> watchResultEventMock = Mockito.mock(Result.class);
        CompletableFuture<Result<SemaphoreChangedEvent>> watchedEvent =
                CompletableFuture.completedFuture(watchResultEventMock);
        Mockito.when(semaphoreWatcher.getChangedFuture()).thenReturn(watchedEvent);
        Mockito.when(watchResultMock.getValue()).thenReturn(semaphoreWatcher);

        sessionMock.watchSemaphore()
                .thenReturn(CompletableFuture.completedFuture(watchResultMock));

        SemaphoreObserver observer = new SemaphoreObserver(
                getCoordinationSession(),
                "observable_semaphore",
                WatchSemaphoreMode.WATCH_DATA_AND_OWNERS,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                new RetryForever(100),
                Executors.newSingleThreadScheduledExecutor()
        );
        observer.start();
        sessionMock.connected();
        observer.getCachedData();
        observer.close();
    }

}
