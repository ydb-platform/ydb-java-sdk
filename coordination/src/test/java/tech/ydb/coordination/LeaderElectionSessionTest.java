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

import tech.ydb.coordination.rpc.grpc.GrpcCoordinationRpc;
import tech.ydb.coordination.session.LeaderElectionSession;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.test.junit4.GrpcTransportRule;

public class LeaderElectionSessionTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final CoordinationClient client = CoordinationClient.newClient(
            GrpcCoordinationRpc.useTransport(ydbTransport)
    );

    @Test
    public void leaderElectionTest() {
        List<LeaderElectionSession> leaderElectionSessionList = Stream.generate(
                () -> client.createLeaderElectionSession(
                SessionSettings.newBuilder()
                        .setSessionNum(1)
                        .setDescription("Test leader election!")
                        .build()
                )
        ).limit(5)
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        int ticket = 0;
        final Set<String> tickets = new HashSet<>();

        List<CompletableFuture<String>> endpoints = new ArrayList<>();
        //Map<String, LeaderElectionSession> ticketToSession = new HashMap<>();

        for (LeaderElectionSession leaderElectionSession : leaderElectionSessionList) {
            String ticketStr = String.valueOf(ticket++);
            CompletableFuture<String> endpoint = new CompletableFuture<>();
           // ticketToSession.put(ticketStr, leaderElectionSession);

            tickets.add(ticketStr);
            leaderElectionSession.start(
                    ticketStr,
                    endpoint::complete
            );

            endpoints.add(endpoint);
        }

        List<String> leaders = endpoints
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        String leaderTicket = leaders.get(0);

        Assert.assertTrue(tickets.contains(leaderTicket));
        for (String ticketStr: leaders) {
            Assert.assertEquals(leaderTicket, ticketStr);
        }
    }
}
