package tech.ydb.coordination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import tech.ydb.coordination.scenario.leader_election.LeaderElection;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class LeaderElectionScenarioTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final CoordinationClient client = CoordinationClient.newClient(ydbTransport);

    @Test
    public void leaderElectionScenarioFullTest() {
        int sessionsSize = 50;

        Map<String, WrapperCompletableFuture<String>> futures = new HashMap<>();

        Map<String, LeaderElection> electionMap = IntStream.range(0, sessionsSize)
                .mapToObj(Integer::toString)
                .collect(Collectors
                        .toMap(
                                Function.identity(),
                                endpoint -> {
                                    WrapperCompletableFuture<String> future = new WrapperCompletableFuture<>();

                                    futures.put(endpoint, future);
                                    return Utils.getStart(
                                                    LeaderElection.newBuilder(client, endpoint, future::complete)
                                            )
                                            .join();
                                }
                        )
                );


        for (int i = 0; i < sessionsSize; i++) {
            List<String> endpoints = futures.values()
                    .stream()
                    .map(WrapperCompletableFuture::join)
                    .collect(Collectors.toList());

            futures.values().forEach(WrapperCompletableFuture::clear);

            String endpointLeader = endpoints.get(0);

            for (String e : endpoints) {
                Assert.assertEquals(endpointLeader, e);
            }

            LeaderElection election = electionMap.get(endpointLeader);
            futures.remove(endpointLeader);
            election.stop();
        }
    }
}
