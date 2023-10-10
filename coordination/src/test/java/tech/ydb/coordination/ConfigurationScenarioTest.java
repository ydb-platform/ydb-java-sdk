//package tech.ydb.coordination;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.junit.Assert;
//import org.junit.ClassRule;
//import org.junit.Test;
//import tech.ydb.coordination.scenario.configuration.ConfigurationPublisher;
//import tech.ydb.coordination.scenario.configuration.ConfigurationSubscriber;
//import tech.ydb.test.junit4.GrpcTransportRule;
//
///**
// * @author Kirill Kurdyukov
// */
//public class ConfigurationScenarioTest {
//
//    @ClassRule
//    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();
//
//    private final CoordinationClient client = CoordinationClient.newClient(ydbTransport);
//    private final String semaphoreName = "configuration-semaphore";
//
//    @Test(timeout = Utils.TIMEOUT)
//    public void configurationScenarioFullTest() {
//        ConfigurationPublisher publisher = Utils.getStart(
//                ConfigurationPublisher.newBuilder(client),
//                semaphoreName
//        ).join();
//
//        List<WrapperCompletableFuture<byte[]>> wrappers = new ArrayList<>();
//
//        List<ConfigurationSubscriber> subscribers = Stream.generate(
//                        () -> {
//                            WrapperCompletableFuture<byte[]> wrapper = new WrapperCompletableFuture<>();
//
//                            wrappers.add(wrapper);
//                            return Utils.getStart(
//                                    ConfigurationSubscriber.newBuilder(
//                                            client,
//                                            configurationData -> {
//                                                if (configurationData.length > 0) {
//                                                    wrapper.complete(configurationData);
//                                                }
//                                            }
//                                    ),
//                                    semaphoreName
//                            );
//                        }
//                )
//                .map(CompletableFuture::join)
//                .limit(3)
//                .collect(Collectors.toList());
//
//        publish(publisher, wrappers, "test1".getBytes());
//
//        wrappers.forEach(WrapperCompletableFuture::clear);
//
//        publish(publisher, wrappers, "test2".getBytes());
//
//        publisher.stop();
//
//        ConfigurationPublisher newPublisher = Utils.getStart(
//                ConfigurationPublisher.newBuilder(client),
//                semaphoreName
//        ).join();
//
//        publish(newPublisher, wrappers, "test3".getBytes());
//
//        newPublisher.stop();
//        subscribers.forEach(ConfigurationSubscriber::stop);
//    }
//
//    private static void publish(
//            ConfigurationPublisher publisher,
//            List<WrapperCompletableFuture<byte[]>> wrappers,
//            byte[] test1
//    ) {
//        Assert.assertTrue(publisher.publishData(test1).join().isSuccess());
//
//        wrappers.forEach(wrapperCompletableFuture ->
//                Assert.assertArrayEquals(test1, wrapperCompletableFuture.join()));
//
//        wrappers.forEach(WrapperCompletableFuture::clear);
//    }
//}
