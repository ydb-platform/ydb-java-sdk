package tech.ydb.coordination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.rpc.grpc.GrpcCoordinationRpc;
import tech.ydb.coordination.session.LeaderElectionSession;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class LeaderElectionSessionTest {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionSessionTest.class);

    @ClassRule
    public static final GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final CoordinationClient client = CoordinationClient.newClient(
            GrpcCoordinationRpc.useTransport(ydbTransport)
    );

    @Test
    public void leaderElectionAndStoppingAllLeadersTest() {
        int sessionSize = 1;
        logger.info("Leader election with sessionsSize = {}", sessionSize);

        final List<LeaderElectionSession> leaderElectionSessionList = getLeaderElectionSessionList(sessionSize);

        int ticket = 0;

        final Set<String> tickets = new HashSet<>();
        final Map<String, WrapperCompletableFuture> endpointToFuture = new HashMap<>();
        final Map<String, LeaderElectionSession> ticketToSession = new HashMap<>();

        for (LeaderElectionSession leaderElectionSession : leaderElectionSessionList) {
            String ticketStr = String.valueOf(ticket++);
            WrapperCompletableFuture endpoint = new WrapperCompletableFuture();

            ticketToSession.put(ticketStr, leaderElectionSession);
            endpointToFuture.put(ticketStr, endpoint);
            tickets.add(ticketStr);

            leaderElectionSession.start(
                    ticketStr,
                    endpoint::complete
            );
        }

        while (!endpointToFuture.isEmpty()) {
            List<String> leaders = endpointToFuture
                    .values()
                    .stream()
                    .map(WrapperCompletableFuture::join)
                    .collect(Collectors.toList());

            for (WrapperCompletableFuture future : endpointToFuture.values()) {
                future.clear();
            }

            String leaderTicket = leaders.get(0);

            Assert.assertTrue(tickets.contains(leaderTicket));
            for (String ticketStr : leaders) {
                Assert.assertEquals(leaderTicket, ticketStr);
            }

            LeaderElectionSession leaderElectionSession = ticketToSession.get(leaderTicket);
            endpointToFuture.remove(leaderTicket);

            try {
                leaderElectionSession.stop();
            } catch (IllegalStateException e) {
                logger.error("Failed stopping", e);
            }
        }
    }

    @Test
    public void stopRandomLeaderThenStart() {
        int sessionSize = 3;

        final List<LeaderElectionSession> leaderElectionSessionList = getLeaderElectionSessionList(sessionSize);

        int ticket = 0;

        final Set<String> tickets = new HashSet<>();
        final List<WrapperCompletableFuture> futures = new ArrayList<>();

        for (LeaderElectionSession leaderElectionSession : leaderElectionSessionList) {
            String ticketStr = String.valueOf(ticket++);
            WrapperCompletableFuture endpoint = new WrapperCompletableFuture();

            leaderElectionSession.start(
                    ticketStr,
                    endpoint::complete
            );

            tickets.add(ticketStr);
            futures.add(endpoint);
        }

        for (int i = 0; i < 10; i++) {
            List<String> leaders = futures
                    .stream()
                    .map(WrapperCompletableFuture::join)
                    .collect(Collectors.toList());

            for (WrapperCompletableFuture future : futures) {
                future.clear();
            }

            String leaderTicket = leaders.get(0);
            Assert.assertTrue(tickets.contains(leaderTicket));

            for (String ticketStr : leaders) {
                Assert.assertEquals(leaderTicket, ticketStr);
            }

            int leaderIndex = Integer.parseInt(leaderTicket);

            logger.info("Stopping session with index {}", leaderIndex);
            leaderElectionSessionList.get(leaderIndex).stop();

            leaderElectionSessionList.get(leaderIndex).start(
                    String.valueOf(leaderIndex),
                    end -> futures.get(leaderIndex).complete(end)
            );
        }

        leaderElectionSessionList.forEach(LeaderElectionSession::stop);
    }

    private List<LeaderElectionSession> getLeaderElectionSessionList(int sessionSize) {
        return Stream
                .generate(
                        () -> client.createLeaderElectionSession(
                                SessionSettings.newBuilder()
                                        .setSessionNum(1)
                                        .setDescription("Test leader election!")
                                        .build()
                        )
                )
                .limit(sessionSize)
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
}
