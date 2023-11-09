package tech.ydb.coordination;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.description.SemaphoreDescription.Session;
import tech.ydb.coordination.scenario.leader_election.LeaderElection;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.test.junit4.GrpcTransportRule;

public class LeaderElectionScenarioTest {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionScenarioTest.class);
    private static final String SEMAPHORE_PREFIX = "leader-election-";
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private final String path = YDB_TRANSPORT.getDatabase() + "/coordination-node";
    private final CoordinationClient client = CoordinationClient.newClient(YDB_TRANSPORT);

    @Before
    public void createNode() {
        CompletableFuture<Status> result = client.createNode(
                path,
                CoordinationNodeSettings.newBuilder()
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }

    @Test(timeout = 20_000)
    public void leaderElectionBaseTest() {
        /* ID for definition Leader Election. Every session has to point the same token. */
        final String semaphoreName = SEMAPHORE_PREFIX + 1_000_001;

        try (LeaderElection participant1 = LeaderElection
                .joinElection(client, path, "endpoint-1", semaphoreName);
             LeaderElection participant2 = LeaderElection
                 .joinElection(client, path, "endpoint-2", semaphoreName)) {
            final Session leader = participant1.getLeader();
            Assert.assertEquals(leader, participant2.getLeader());
            logger.info("The first leader: " + new String(leader.getData(), StandardCharsets.UTF_8));

            try (LeaderElection participant3 = LeaderElection
                    .joinElection(client, path, "endpoint-3", semaphoreName)) {
                Assert.assertEquals(leader, participant3.getLeader());

                /* The leader is not a leader anymore */
                final Session newLeader;
                if (participant1.isLeader()) {
                    participant1.leaveElection();
                    newLeader = participant2.forceUpdateLeader();
                    Assert.assertEquals(newLeader, participant3.forceUpdateLeader());
                    Assert.assertNotEquals(newLeader, leader);
                } else if (participant2.isLeader()) {
                    participant2.leaveElection();
                    newLeader = participant1.forceUpdateLeader();
                    Assert.assertEquals(newLeader, participant3.forceUpdateLeader());
                    Assert.assertNotEquals(newLeader, leader);
                } else if (participant3.isLeader()) {
                    participant3.leaveElection();
                    newLeader = participant1.forceUpdateLeader();
                    Assert.assertEquals(newLeader, participant2.forceUpdateLeader());
                    Assert.assertNotEquals(newLeader, leader);
                } else {
                    newLeader = null;
                    Assert.fail("None of participants is a leader.");
                }
                logger.info("The second leader: " + new String(newLeader.getData(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                Assert.fail("Exception in leader election test.");
            }
        } catch (Exception e) {
            Assert.fail("Exception in leader election test.");
        }
    }

    @Test(timeout = 60_000)
    public void leaderElectionStressTest1() {
        final int sessionCount = 20;
        /* ID for definition Leader Election. Every session has to point the same token. */
        final String semaphoreName = SEMAPHORE_PREFIX + 1_000_000;

        List<LeaderElection> participants = IntStream.range(0, sessionCount).mapToObj(id -> LeaderElection
                        .joinElection(client, path, "endpoint-" + id, semaphoreName))
                .collect(Collectors.toList());

        final AtomicReference<Session> leader = new AtomicReference<>();
        for (LeaderElection participant : participants) {
            Session localLeader = participant.getLeader();
            leader.updateAndGet(currLeader -> currLeader == null ? localLeader : currLeader);
            Assert.assertEquals(leader.get(), localLeader);
        }

        /* The leader is not a leader anymore */
        for (int i = 0; i < sessionCount; i++) {
            if (participants.get(i).isLeader()) {
                participants.remove(i).leaveElection();
                break;
            }
        }

        final AtomicReference<Session> newLeader = new AtomicReference<>();
        for (LeaderElection participant : participants) {
            participant.forceUpdateLeader();
            Session localLeader = participant.getLeader();
            newLeader.updateAndGet(currLeader -> currLeader == null ? localLeader : currLeader);
            Assert.assertEquals(newLeader.get(), localLeader);
        }

        Assert.assertNotEquals(leader.get(), newLeader.get());

        participants.stream().map(LeaderElection::leaveElectionAsync).forEach(CompletableFuture::join);
    }

    @Test(timeout = 60_000)
    public void leaderElectionOnPureSessionsTest() throws InterruptedException {
        final String semaphoreName = "leader-election-semaphore";
        final int sessionCount = 10;
        final CountDownLatch latch1 = new CountDownLatch(sessionCount);
        List<CoordinationSession> sessions = Stream.generate(() -> client.createSession(path).join())
                .limit(sessionCount)
                .collect(Collectors.toList());

        CompletableFuture<SemaphoreLease> semaphore = new CompletableFuture<>();
        CompletableFuture<CoordinationSession> leader = new CompletableFuture<>();

        sessions.forEach(session ->
                session.createSemaphore(semaphoreName, 1)
                        .whenComplete((status, createSemaphoreTh) -> {
                                    latch1.countDown();
                                    Assert.assertNull(createSemaphoreTh);
                                    Assert.assertTrue(status == Status.SUCCESS &&
                                            status.getCode() == StatusCode.ALREADY_EXISTS);
                                }
                        )
        );

        latch1.await();
        final CountDownLatch latch2 = new CountDownLatch(sessionCount);

        sessions.forEach(session -> session
                .acquireSemaphore(semaphoreName, 1, String.valueOf(session.getId()).getBytes(), Duration.ZERO)
                .whenComplete((lease, acquireSemaphoreTh) -> {
                            Assert.assertNull(acquireSemaphoreTh);
                            if (lease.isValid()) {
                                semaphore.complete(lease);
                                leader.complete(session);
                            }
                            latch2.countDown();
                        }
                ));

        latch2.await();
        final CoordinationSession leaderSession = leader.join();
        final CountDownLatch latch3 = new CountDownLatch(sessionCount);

        sessions.forEach(session -> session.describeSemaphore(semaphoreName, DescribeSemaphoreMode.WITH_OWNERS)
                .whenComplete((result, th) -> {
                    Assert.assertTrue(result.isSuccess());
                    Assert.assertNull(th);
                    Assert.assertArrayEquals(String.valueOf(leaderSession.getId()).getBytes(),
                            result.getValue().getOwnersList().get(0).getData());
                    latch3.countDown();
                }));

        latch3.await();
    }

    @After
    public void deleteNode() {
        CompletableFuture<Status> result = client.dropNode(
                path,
                DropCoordinationNodeSettings.newBuilder()
                        .build()
        );
        Assert.assertTrue(result.join().isSuccess());
    }
}