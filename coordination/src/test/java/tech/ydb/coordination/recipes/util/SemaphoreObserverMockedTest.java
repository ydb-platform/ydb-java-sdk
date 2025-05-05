package tech.ydb.coordination.recipes.util;

import org.junit.Test;
import tech.ydb.common.retry.RetryForever;
import tech.ydb.coordination.CoordinationSessionBaseMockedTest;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;

public class SemaphoreObserverMockedTest extends CoordinationSessionBaseMockedTest {

    @Test
    public void successTest() {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        SemaphoreObserver observer = new SemaphoreObserver(
                getCoordinationSession(),
                "observable_semaphore",
                WatchSemaphoreMode.WATCH_DATA_AND_OWNERS,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                new RetryForever(100)
        );
        observer.start();
        sessionMock.connected();
        observer.getCachedData();
    }

}
