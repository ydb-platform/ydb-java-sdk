package tech.ydb.coordination;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import tech.ydb.coordination.rpc.grpc.GrpcCoordinationRpc;
import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoveryPublisher;
import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoveryScenarioFactory;
import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoverySubscriber;
import tech.ydb.coordination.settings.ScenarioSettings;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class ServiceDiscoveryScenarioTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final CoordinationClient client = CoordinationClient.newClient(
            GrpcCoordinationRpc.useTransport(ydbTransport)
    );

    @Test
    public void serviceDiscoveryScenarioFullTest() {
        ServiceDiscoveryScenarioFactory factory = new ServiceDiscoveryScenarioFactory(client);
        ScenarioSettings settings = ScenarioSettings.newBuilder()
                .setCoordinationNodeName("service-discovery-test")
                .setSemaphoreName("service-discovery-test")
                .setDescription("Test discovery settings")
                .build();

        Set<String> hosts = Stream.of("localhost1", "localhost2")
                .collect(Collectors.toCollection(CopyOnWriteArraySet::new));

        List<ServiceDiscoveryPublisher> publishers = hosts.stream()
                .map(endpoint -> factory.serviceDiscoveryPublisher(settings, endpoint))
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        WrapperCompletableFuture<Set<String>> future = new WrapperCompletableFuture<>();

        ServiceDiscoverySubscriber subscriber = factory.serviceDiscoverySubscriber(
                settings,
                endpoints -> {
                    if (hosts.size() == endpoints.size()) {
                        future.complete(new HashSet<>(endpoints));
                    }
                }
        ).join();

        assertEndpoints(hosts, future);

        future.clear();
        hosts.add("localhost3");

        publishers.add(factory.serviceDiscoveryPublisher(settings, "localhost3").join());

        assertEndpoints(hosts, future);

        publishers.forEach(ServiceDiscoveryPublisher::stop);
        subscriber.stop();
    }

    private static void assertEndpoints(Set<String> hosts, WrapperCompletableFuture<Set<String>> future) {
        Set<String> endpoints = future.join();

        Assert.assertEquals(hosts, endpoints);
    }
}
