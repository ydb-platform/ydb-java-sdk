package tech.ydb.coordination;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.coordination.scenario.configuration.Publisher;
import tech.ydb.coordination.scenario.configuration.Subscriber;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;

public class ConfigurationScenarioTest {
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
    public void configurationScenarioTest() throws BrokenBarrierException, InterruptedException {
        final long token = 1_000_001;
        final int n = 5;
        Publisher publisher = Publisher.newPublisher(client, path, token).join();

        final String[] dataNow = new String[1];
        final CountDownLatch[] updateCounter = new CountDownLatch[1];

        updateCounter[0] = new CountDownLatch(n);
        dataNow[0] = "Nothing";
        publisher.publish(dataNow[0].getBytes(StandardCharsets.UTF_8)).join();

        final List<Subscriber> subscribers = Stream.generate(() ->
                Subscriber.newSubscriber(client, path, token, data -> {
                    if (Arrays.equals(data, dataNow[0].getBytes(StandardCharsets.UTF_8))) {
                        updateCounter[0].countDown();
                    }
                }).join()
        ).limit(n).collect(Collectors.toList());

        updateCounter[0].await();
        dataNow[0] = "Second message";
        updateCounter[0] = new CountDownLatch(n);
        publisher.publish(dataNow[0].getBytes(StandardCharsets.UTF_8)).join();

        updateCounter[0].await();

        subscribers.forEach(Subscriber::close);
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
