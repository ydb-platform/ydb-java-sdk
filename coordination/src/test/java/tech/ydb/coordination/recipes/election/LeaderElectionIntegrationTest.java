package tech.ydb.coordination.recipes.election;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.AwaitAssert;
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

        AwaitAssert.await().until(leadershipTaken::get);
        Assert.assertTrue(leadershipTaken.get());
        elector.close();
    }

    @Test
    public void interruptLeadership_ThenStops() throws Exception {
        AtomicBoolean leadershipTaken = new AtomicBoolean(false);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        String testName = "interruptLeadership_ThenStops";
        LeaderElection elector = getLeaderElector(testName, new LeaderElectionListener() {
            @Override
            public void takeLeadership() throws Exception {
                try {
                    logger.debug("Leadership is taken");
                    leadershipTaken.set(true);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    interrupted.set(true);
                    logger.debug("Leadership is interrupted");
                }
            }
        });
        elector.start();
        elector.requeue();

        AwaitAssert.await().until(leadershipTaken::get);
        Assert.assertTrue(leadershipTaken.get());

        elector.interruptLeadership();
        AwaitAssert.await().until(interrupted::get);
        Assert.assertFalse(elector.isLeader());

        elector.close();
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
        AwaitAssert.await().until(() -> leadershipCount.get() == 1);

        elector.requeue();
        AwaitAssert.await().until(() -> leadershipCount.get() == 2);
        elector.close();
    }

    @Test
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

        AwaitAssert.await().until(leader1Taken::get);

        // Check participants and leader
        List<ElectionParticipant> participants1 = elector1.getParticipants();
        Optional<ElectionParticipant> leader1 = elector1.getCurrentLeader();
        logger.info("current leader 1 {}", leader1);
        logger.info("current participants 1 {}", participants1);

        Assert.assertEquals(1, participants1.size());
        Assert.assertTrue(leader1.isPresent());
        Assert.assertTrue(leader1.get().isLeader());
        Assert.assertArrayEquals(leader1.get().getData(), testName.getBytes(StandardCharsets.UTF_8));
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
        elector2.autoRequeue();
        elector2.start();

        AwaitAssert.await().until(leader2Taken::get);
        // Check participants and leader
        participants1 = elector1.getParticipants();
        leader1 = elector1.getCurrentLeader();
        logger.info("current leader 1 {}", leader1);
        logger.info("current participants 1 {}", participants1);

        List<ElectionParticipant> participants2 = elector2.getParticipants();
        Optional<ElectionParticipant> leader2 = elector2.getCurrentLeader();
        logger.info("current leader 2 {}", leader2);
        logger.info("current participants 2 {}", participants2);

        Assert.assertEquals(participants1, participants2);
        Assert.assertEquals(leader1.hashCode(), leader2.hashCode());

        Assert.assertTrue(leader2Taken.get());
        Assert.assertTrue(elector2.isLeader());
        Assert.assertEquals(elector2.getCurrentLeader().get().getSessionId(),
                elector1.getCurrentLeader().get().getSessionId());
        Assert.assertFalse(elector1.isLeader());

        elector1.close();
        elector2.close();
    }
}
