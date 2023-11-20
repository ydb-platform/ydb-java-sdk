package tech.ydb.coordination;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

import tech.ydb.coordination.scenario.leader_election.LeaderElection;
import tech.ydb.coordination.scenario.leader_election.LeaderElection.LeadershipPolicy;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.test.junit4.GrpcTransportRule;

public class LeaderElectionScenarioTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionScenarioTest.class);
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

    @Test(timeout = 40_000)
    public void leaderElectionBaseTest() {
        final String semaphoreName = "leader-election-base-test";

        final AtomicReference<String> leader = new AtomicReference<>();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        try (LeaderElection participant1 = LeaderElection
                .joinElection(client, path, "endpoint-1", semaphoreName)
                .withLeadershipPolicy(LeadershipPolicy.TAKE_LEADERSHIP)
                .withTakeLeadershipObserver(leaderElection -> {
                    leader.set("endpoint-1");
                    logger.info("Endpoint-1 is the leader now!");
                    awaitBarrier(barrier);
                }).build();

             LeaderElection participant2 = LeaderElection
                 .joinElection(client, path, "endpoint-2", semaphoreName)
                 .withLeadershipPolicy(LeadershipPolicy.TAKE_LEADERSHIP)
                 .withTakeLeadershipObserver(leaderElection -> {
                     leader.set("endpoint-2");
                     logger.info("Endpoint-2 is the leader now!");
                     awaitBarrier(barrier);
                 }).build()
        ) {
            awaitBarrier(barrier);
            String leaderFromParticipant1 = participant1.forceUpdateLeader().orElse("none");
            String leaderFromParticipant2 = participant2.forceUpdateLeader().orElse("none");
            Assert.assertEquals(leader.get(), leaderFromParticipant1);
            Assert.assertEquals(leader.get(), leaderFromParticipant2);

            logger.info("The first leader: " + leader.get());
            barrier.reset();

            /* Leader change observer will be call 3 times:
                first - after asking election who is a leader now
                second - after leader call interruptLeadership()
                third - after call forceUpdateLeader() on participant3
             */
            CountDownLatch counter = new CountDownLatch(3);
            try (LeaderElection participant3 = LeaderElection
                    .joinElection(client, path, "endpoint-3", semaphoreName)
                    .withLeadershipPolicy(LeadershipPolicy.TAKE_LEADERSHIP)
                    .withTakeLeadershipObserver(leaderElection -> {
                        leader.set("endpoint-3");
                        logger.info("Endpoint-3 is the leader now!");
                        awaitBarrier(barrier);
                    })
                    .withChangeLeaderObserver(leaderElection -> counter.countDown()).build()
            ) {
                final String previousLeader = leader.get();
                switch (leader.get()) {
                    case "endpoint-1":
                        participant1.interruptLeadership();
                        break;
                    case "endpoint-2":
                        participant2.interruptLeadership();
                        break;
                    case "endpoint3":
                        participant3.interruptLeadership();
                    default:
                        throw new RuntimeException("No leader was elected.");
                }

                awaitBarrier(barrier);
                Assert.assertNotEquals(previousLeader, leader.get());
                Assert.assertEquals(leader.get(), participant1.forceUpdateLeader().orElse("none"));
                Assert.assertEquals(leader.get(), participant2.forceUpdateLeader().orElse("none"));
                Assert.assertEquals(leader.get(), participant3.forceUpdateLeader().orElse("none"));
                Assert.assertTrue(counter.await(40, TimeUnit.SECONDS));
            } catch (Exception e) {
                Assert.fail("Exception in leader election test.");
            }
        } catch (Exception e) {
            Assert.fail("Exception in leader election test.");
        }
    }

    @Test(timeout = 20_000)
    public void leaderElectionOneLeaderSeveralFollowerTest() {
        final String name = "leader-election-one-leader-several-followers";
        final AtomicBoolean isFollowerALeader = new AtomicBoolean(false);
        /* Check that after leader.interruptLeadership() this leader will be chosen again */
        final CountDownLatch latch = new CountDownLatch(1);

        try (LeaderElection follower1 = LeaderElection.joinElection(client, path, "endpoint-1", name)
                .withTakeLeadershipObserver(leaderElection -> isFollowerALeader.set(true)).build();

             LeaderElection follower2 = LeaderElection.joinElection(client, path, "endpoint-2", name)
                     .withTakeLeadershipObserver(leaderElection -> isFollowerALeader.set(true)).build();

             LeaderElection leader = LeaderElection.joinElection(client, path, "endpoint-3", name)
                     .withTakeLeadershipObserver(leaderElection -> {
                         latch.countDown();
                         leaderElection.interruptLeadership();
                     }).build()
        ) {

            leader.proposeLeadershipAsync();

            Assert.assertTrue(latch.await(20_000, TimeUnit.MILLISECONDS));

            Assert.assertFalse(follower1.isLeader());
            Assert.assertFalse(follower2.isLeader());
            Assert.assertFalse(isFollowerALeader.get());
        } catch (Exception e) {
            Assert.fail("Exception in leader election test: " + e.getMessage());
        }
    }

    @Test(timeout = 20_000)
    public void leaderElectionBaseTest2() {
        final String name = "leader-election-base-test-2";
        CyclicBarrier barrier = new CyclicBarrier(2);

        try (LeaderElection participant1 = LeaderElection.joinElection(client, path, "endpoint-1", name)
                .withLeadershipPolicy(LeadershipPolicy.TAKE_LEADERSHIP)
                .withTakeLeadershipObserver(leaderElection -> awaitBarrier(barrier))
                .build();

             LeaderElection participant2 = LeaderElection.joinElection(client, path, "endpoint-2", name).build()
        ) {
            awaitBarrier(barrier);
            String firstLeader = participant1.forceUpdateLeader().orElse("none");
            Assert.assertEquals("endpoint-1", firstLeader);

            barrier.reset();

            participant1.interruptLeadership();
            awaitBarrier(barrier);

            Assert.assertEquals(firstLeader, participant1.forceUpdateLeader().orElse("none"));
            Assert.assertEquals(firstLeader, participant2.forceUpdateLeader().orElse("none"));
        } catch (Exception e) {
            Assert.fail("Exception while testing leader election scenario.");
        }
    }

    @Test(timeout = 20_000)
    public void leaderElectionBaseTest3() {
        final String name = "leader-election-base-test-3";
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (LeaderElection participant1 = LeaderElection.joinElection(client, path, "endpoint-1", name)
                .withTakeLeadershipObserver(leaderElection -> {
                    logger.info("Endpoint-1 is a leader now!");
                    awaitBarrier(barrier);
                }).build();
             LeaderElection participant2 = LeaderElection.joinElection(client, path, "endpoint-2", name)
                     .withTakeLeadershipObserver(leaderElection -> {
                         logger.info("Endpoint-2 is a leader now!");
                         awaitBarrier(barrier);
                     }).build()
        ) {
            participant1.proposeLeadershipAsync();
            participant2.proposeLeadershipAsync();
            awaitBarrier(barrier);
            barrier.reset();
            if (participant1.isLeader()) {
                participant1.interruptLeadership();
            } else {
                participant2.interruptLeadership();
            }
            awaitBarrier(barrier);
            barrier.reset();
            if (participant1.isLeader()) {
                participant1.interruptLeadership();
            } else {
                participant2.interruptLeadership();
            }
            awaitBarrier(barrier);
        } catch (Exception e) {
            Assert.fail("Exception while testing leader election scenario.");
        }
    }

    @Test(timeout = 60_000)
    public void leaderElectionStressTest1() {
        final int sessionCount = 20;
        final String semaphoreName = "leader-election-stress-test-1";
        CyclicBarrier barrier = new CyclicBarrier(2);

        List<LeaderElection> participants = IntStream.range(0, sessionCount).mapToObj(id -> LeaderElection
                        .joinElection(client, path, "endpoint-" + id, semaphoreName)
                        .withTakeLeadershipObserver(leaderElection -> awaitBarrier(barrier))
                        .withLeadershipPolicy(LeadershipPolicy.TAKE_LEADERSHIP)
                        .build())
                .collect(Collectors.toList());

        awaitBarrier(barrier);
        final AtomicReference<String> leader = new AtomicReference<>();
        for (LeaderElection participant : participants) {
            String localLeader = participant.forceUpdateLeader().orElse("none");
            leader.updateAndGet(currLeader -> currLeader == null ? localLeader : currLeader);
            Assert.assertEquals(leader.get(), localLeader);
        }

        barrier.reset();

        /* The leader is not a leader anymore */
        for (int i = 0; i < sessionCount; i++) {
            if (participants.get(i).isLeader()) {
                participants.remove(i).close();
                break;
            }
        }

        awaitBarrier(barrier);
        final AtomicReference<String> newLeader = new AtomicReference<>();
        for (LeaderElection participant : participants) {
            participant.forceUpdateLeader();
            String localLeader = participant.forceUpdateLeader().orElse("none");
            newLeader.updateAndGet(currLeader -> currLeader == null ? localLeader : currLeader);
            Assert.assertEquals(newLeader.get(), localLeader);
        }

        Assert.assertNotEquals(leader.get(), newLeader.get());

        participants.forEach(LeaderElection::close);
    }

    @Test(timeout = 60_000)
    public void leaderElectionOnPureSessionsTest() throws InterruptedException {
        final String semaphoreName = "leader-election-semaphore";
        final AtomicBoolean assertChecker = new AtomicBoolean(true);
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
                                    threadWorkAssert(assertChecker, createSemaphoreTh == null);
                                    threadWorkAssert(assertChecker, status == Status.SUCCESS ||
                                            status.getCode() == StatusCode.ALREADY_EXISTS);
                                }
                        )
        );

        latch1.await();
        final CountDownLatch latch2 = new CountDownLatch(sessionCount);

        sessions.forEach(session -> session
                .acquireSemaphore(semaphoreName, 1, String.valueOf(session.getId()).getBytes(), Duration.ZERO)
                .whenComplete((lease, acquireSemaphoreTh) -> {
                            threadWorkAssert(assertChecker, acquireSemaphoreTh == null);
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
                    threadWorkAssert(assertChecker, result.isSuccess());
                    threadWorkAssert(assertChecker, th == null);
                    threadWorkAssert(assertChecker, Arrays.equals(String.valueOf(leaderSession.getId()).getBytes(),
                            result.getValue().getOwnersList().get(0).getData()));
                    latch3.countDown();
                }));

        latch3.await();
        Assert.assertTrue(assertChecker.get());
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


    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void threadWorkAssert(AtomicBoolean atomicBoolean, boolean condition) {
        if (!condition) {
            atomicBoolean.set(false);
            logger.warn("Thread work assert is fail.");
            Arrays.stream(Thread.currentThread().getStackTrace())
                    .map(StackTraceElement::toString)
                    .forEach(logger::warn);
        }
    }
}
