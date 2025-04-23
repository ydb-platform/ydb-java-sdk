package tech.ydb.coordination.recipes;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSessionBaseMockedTest;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ReadWriteLockTest extends CoordinationSessionBaseMockedTest {

    @Test
    public void startTest() throws Exception {
        StreamMock streamMock = mockStream();
        streamMock.complete(StatusCode.TRANSPORT_UNAVAILABLE);

        CoordinationSession session = client.createSession("/coordination/node/path");
        session.connect();
        streamMock.nextMsg().isSessionStart().hasPath("/coordination/node/path");

        Assert.assertEquals(CoordinationSession.State.INITIAL, session.getState());
    }

    @Test
    public void successAcquireTest() throws Exception {
        StreamMock streamMock = mockStream();

        CoordinationSession session = client.createSession(
                "/coordination/node/path",
                    CoordinationSessionSettings.newBuilder()
                            .withExecutor(getScheduler())
                            .build()
        );
        session.connect();
        streamMock.nextMsg().isSessionStart().hasPath("/coordination/node/path");
        streamMock.responseSessionStarted(123);

        getScheduler().hasTasks(1).executeNextTasks(1);
        Assert.assertEquals(CoordinationSession.State.CONNECTED, session.getState());

        CompletableFuture<Result<SemaphoreLease>> leaseResult = session.acquireEphemeralSemaphore("lock", false, Duration.ofSeconds(1));
        long requestId = streamMock.nextMsg().isAcquireSemaphore()
                .isEphemeralSemaphore()
                .hasSemaphoreName("lock")
                .get()
                .getAcquireSemaphore()
                .getReqId();
        streamMock.responseAcquiredSuccessfully(requestId);
        getScheduler().hasTasks(1).executeNextTasks(1);
        Result<SemaphoreLease> lease = leaseResult.join();

        Assert.assertTrue(lease.isSuccess());
        Assert.assertEquals("lock", lease.getValue().getSemaphoreName());
    }

}
