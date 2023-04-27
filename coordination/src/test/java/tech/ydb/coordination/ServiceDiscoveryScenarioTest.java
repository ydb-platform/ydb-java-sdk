package tech.ydb.coordination;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoveryPublisher;
import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoverySubscriber;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class ServiceDiscoveryScenarioTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final CoordinationClient client = CoordinationClient.newClient(ydbTransport);

    @Test
    public void serviceDiscoveryScenarioFullTest() {
        Set<String> hosts = Stream.of("localhost1", "localhost2")
                .collect(Collectors.toCollection(CopyOnWriteArraySet::new));

        List<ServiceDiscoveryPublisher> publishers = hosts.stream()
                .map(endpoint -> ServiceDiscoveryPublisher.newBuilder(client, endpoint)
                        .setCoordinationNodeName("service-discovery-test")
                        .setSemaphoreName("service-discovery-test")
                        .setDescription("Test discovery settings")
                        .start()
                )
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        WrapperCompletableFuture<Set<String>> future = new WrapperCompletableFuture<>();

        ServiceDiscoverySubscriber subscriber = ServiceDiscoverySubscriber.newBuilder(
                        client,
                        endpoints -> {
                            if (hosts.size() == endpoints.size()) {
                                future.complete(new HashSet<>(endpoints));
                            }
                        }
                )
                .setCoordinationNodeName("service-discovery-test")
                .setSemaphoreName("service-discovery-test")
                .setDescription("Test discovery settings")
                .start()
                .join();

        assertEndpoints(hosts, future);

        future.clear();
        hosts.add("localhost3");

        publishers.add(
                ServiceDiscoveryPublisher.newBuilder(client, "localhost3")
                        .setCoordinationNodeName("service-discovery-test")
                        .setSemaphoreName("service-discovery-test")
                        .setDescription("Test discovery settings")
                        .start()
                        .join()
        );

        assertEndpoints(hosts, future);

        publishers.forEach(ServiceDiscoveryPublisher::stop);
        subscriber.stop();
    }

    private static void assertEndpoints(Set<String> hosts, WrapperCompletableFuture<Set<String>> future) {
        Set<String> endpoints = future.join();

        Assert.assertEquals(hosts, endpoints);
    }
}
