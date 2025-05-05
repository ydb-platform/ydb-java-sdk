package tech.ydb.coordination.recipes.election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.test.junit4.GrpcTransportRule;

public class LeaderElectionIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionIntegrationTest.class);

    @ClassRule
    public static final GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static CoordinationClient client;

    @BeforeClass
    public static void init() {
        client = CoordinationClient.newClient(ydbRule);
    }

    @AfterClass
    public static void clean() {
        ydbRule.close();
    }

    private LeaderElection getLeaderElector(
            String testName,
            LeaderElectionListener leaderElectionListener
    ) {
        return getLeaderElector(
                testName,
                testName,
                testName.getBytes(StandardCharsets.UTF_8),
                leaderElectionListener
        );
    }

    private LeaderElection getLeaderElector(
            String nodePath,
            String lockName,
            byte[] data,
            LeaderElectionListener leaderElectionListener
    ) {
        client.createNode(nodePath).join().expectSuccess("cannot create coordination path");
        LeaderElection leaderElectorImpl = new LeaderElection(
                client,
                nodePath,
                lockName,
                data,
                leaderElectionListener
        );
        return leaderElectorImpl;
    }

    @Test
    public void shouldCallTakeLeadershipWhenElected() throws Exception {
        AtomicBoolean leadershipTaken = new AtomicBoolean(false);

        String testName = "shouldCallTakeLeadershipWhenElected";
        LeaderElection elector = getLeaderElector(testName, new LeaderElectionListener() {
            @Override
            public void takeLeadership() throws Exception {
                leadershipTaken.set(true);
                logger.debug("Leadership is taken");
            }
        });
        elector.start();
        elector.requeue();

        await(Duration.ofSeconds(10), Duration.ofMillis(100), leadershipTaken::get);
        Assert.assertTrue(leadershipTaken.get());
    }

    @Test
    public void shouldCallTakeLeadershipAgainAfterRequeue() throws Exception {
        AtomicInteger leadershipCount = new AtomicInteger(0);

        String testName = "shouldCallTakeLeadershipAgainAfterRequeue";
        LeaderElection elector = getLeaderElector(testName, new LeaderElectionListener() {
            @Override
            public void takeLeadership() throws Exception {
                leadershipCount.incrementAndGet();
            }
        });
        elector.start();

        elector.requeue();
        await(Duration.ofSeconds(10), Duration.ofMillis(100), () -> leadershipCount.get() > 0);

        elector.requeue();
        await(Duration.ofSeconds(10), Duration.ofMillis(100), () -> leadershipCount.get() > 1);
    }

    @Test
    @Ignore
    public void shouldTrackParticipantsAndLeader() throws Exception {
        String testName = "shouldTrackParticipantsAndLeader";

        // Create first leader
        AtomicBoolean leader1Taken = new AtomicBoolean(false);
        LeaderElection elector1 = getLeaderElector(testName, new LeaderElectionListener() {
            @Override
            public void takeLeadership() throws Exception {
                logger.info("Leadership 1 taken");
                leader1Taken.set(true);
                Thread.sleep(5000);
                logger.info("Leadership 1 ended");
            }
        });
        elector1.start();
        elector1.requeue();

        await(Duration.ofSeconds(10), Duration.ofMillis(100), leader1Taken::get);

        // Check participants and leader
        List<ElectionParticipant> participants1 = elector1.getParticipants();
        Optional<ElectionParticipant> leader1 = elector1.getCurrentLeader();
        logger.info("current leader 1 {}", leader1);
        logger.info("current participants 1 {}", participants1);

        Assert.assertEquals(1, participants1.size());
        Assert.assertTrue(leader1.isPresent());
        Assert.assertEquals(participants1.get(0).getSessionId(), leader1.get().getSessionId());

        // Add second leader
        AtomicBoolean leader2Taken = new AtomicBoolean(false);
        LeaderElection elector2 = getLeaderElector(testName, new LeaderElectionListener() {
            @Override
            public void takeLeadership() throws Exception {
                logger.info("Leadership 2 taken");
                leader2Taken.set(true);
                Thread.sleep(20000);
                logger.info("Leadership 2 ended");
            }
        });
        elector2.start();
        elector2.requeue();

        await(Duration.ofSeconds(10), Duration.ofMillis(100), leader2Taken::get);
        // Check participants and leader
        participants1 = elector1.getParticipants();
        leader1 = elector1.getCurrentLeader();
        logger.info("current leader 1 {}", leader1);
        logger.info("current participants 1 {}", participants1);

        List<ElectionParticipant> participants2 = elector2.getParticipants();
        Optional<ElectionParticipant> leader2 = elector2.getCurrentLeader();
        logger.info("current leader 2 {}", leader2);
        logger.info("current participants 2 {}", participants2);

        Assert.assertTrue(leader2Taken.get());
        Assert.assertTrue(elector2.isLeader());
        Assert.assertEquals(elector2.getCurrentLeader().get().getSessionId(),
                elector1.getCurrentLeader().get().getSessionId());
        Assert.assertFalse(elector1.isLeader());
    }

    public static void await(Duration waitDuration, Duration checkDuration, BooleanSupplier condition) {
        long timeout = System.currentTimeMillis() + waitDuration.toMillis();

        while (System.currentTimeMillis() < timeout) {
            if (condition.getAsBoolean()) {
                return;
            }

            try {
                Thread.sleep(checkDuration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Await interrupted", e);
            }
        }

        throw new RuntimeException("Condition not met within " + waitDuration);
    }
}
