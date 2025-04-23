package tech.ydb.coordination.recipes;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSessionBaseMockedTest;
import tech.ydb.core.StatusCode;

public class ReadWriteLockTest extends CoordinationSessionBaseMockedTest {

    @Test
    public void simpleTest() {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(defaultStreamMockAnswer()); // and repeat;

        CoordinationSession session = client.createSession("/coordination/node/path");
        session.connect();

        Assert.assertNull(currentStream());
        getScheduler().hasTasks(1).executeNextTasks(1);
    }

}
