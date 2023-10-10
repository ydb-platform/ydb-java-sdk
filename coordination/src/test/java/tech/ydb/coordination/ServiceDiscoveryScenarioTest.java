//package tech.ydb.coordination;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CopyOnWriteArraySet;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.junit.Assert;
//import org.junit.ClassRule;
//import org.junit.Test;
//import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoveryPublisher;
//import tech.ydb.coordination.scenario.service_discovery.ServiceDiscoverySubscriber;
//import tech.ydb.test.junit4.GrpcTransportRule;
//
///**
// * @author Kirill Kurdyukov
// */
//public class ServiceDiscoveryScenarioTest {
//
//    @ClassRule
//    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();
//
//    private final CoordinationClient client = CoordinationClient.newClient(ydbTransport);
//    private final String semaphoreName = "service-discovery-semaphore";
//
//    @Test(timeout = Utils.TIMEOUT)
//    public void serviceDiscoveryScenarioFullTest() {
//        Set<String> hosts = Stream.of("localhost1", "localhost2")
//                .collect(Collectors.toCollection(CopyOnWriteArraySet::new));
//
//        List<ServiceDiscoveryPublisher> publishers = hosts.stream()
//                .map(endpoint -> Utils.getStart(ServiceDiscoveryPublisher.newBuilder(client, endpoint), semaphoreName))
//                .map(CompletableFuture::join)
//                .collect(Collectors.toList());
//
//        WrapperCompletableFuture<Set<String>> future = new WrapperCompletableFuture<>();
//
//        ServiceDiscoverySubscriber subscriber = Utils.getStart(ServiceDiscoverySubscriber.newBuilder(
//                        client,
//                        endpoints -> {
//                            if (hosts.size() == endpoints.size()) {
//                                future.complete(new HashSet<>(endpoints));
//                            }
//                        }
//                ), semaphoreName)
//                .join();
//
//        assertEndpoints(hosts, future);
//
//        future.clear();
//        hosts.add("localhost3");
//
//        publishers.add(Utils.getStart(
//                ServiceDiscoveryPublisher.newBuilder(client, "localhost3"),
//                semaphoreName
//        ).join());
//
//        assertEndpoints(hosts, future);
//
//        subscriber.stop();
//        publishers.forEach(ServiceDiscoveryPublisher::stop);
//    }
//
//    private static void assertEndpoints(Set<String> hosts, WrapperCompletableFuture<Set<String>> future) {
//        Set<String> endpoints = future.join();
//
//        Assert.assertEquals(hosts, endpoints);
//    }
//}
