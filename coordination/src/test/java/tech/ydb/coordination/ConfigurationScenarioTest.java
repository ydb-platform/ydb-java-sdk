package tech.ydb.coordination;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
    static final String SEMAPHORE_PREFIX = "configuration-";
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
    public void configurationScenarioBaseTest() {
        final String semaphoreName = SEMAPHORE_PREFIX + 1_000_000;

        try (Publisher publisher1 = Publisher.newPublisher(client, path, semaphoreName)) {
            final AtomicReference<CountDownLatch> subscriberApproveCounter =
                    new AtomicReference<>(new CountDownLatch(2));
            final AtomicReference<String> dataNow = new AtomicReference<>();
            dataNow.set("First message.");
            /* Public first data */
            publisher1.publish(dataNow.get().getBytes(StandardCharsets.UTF_8));
            /* Create 2 subscribers */
            try (Subscriber subscriber1 = Subscriber.newSubscriber(client, path, semaphoreName, data -> {
                if (Arrays.equals(data, dataNow.get().getBytes())) {
                    subscriberApproveCounter.get().countDown();
                }
            });
             Subscriber subscriber2 = Subscriber.newSubscriber(client, path, semaphoreName, data -> {
                 if (Arrays.equals(data, dataNow.get().getBytes())) {
                     subscriberApproveCounter.get().countDown();
                 }
             })) {
                subscriberApproveCounter.get().await();
                subscriberApproveCounter.set(new CountDownLatch(2));

                dataNow.set("Second message.");
                publisher1.publish(dataNow.get().getBytes(StandardCharsets.UTF_8));
                subscriberApproveCounter.get().await();
                subscriberApproveCounter.set(new CountDownLatch(2));

                /* Create another publisher */
                try (Publisher publisher2 = Publisher.newPublisher(client, path, semaphoreName)) {
                    dataNow.set("Third message.");
                    publisher2.publish(dataNow.get().getBytes(StandardCharsets.UTF_8));
                    subscriberApproveCounter.get().await();
                } catch (Exception e) {
                    Assert.fail("Exception in Configuration scenario test.");
                }
            } catch (Exception e) {
                Assert.fail("Exception in Configuration scenario test.");
            }
        } catch (Exception e) {
            Assert.fail("Exception in Configuration scenario test.");
        }
    }

    @Test(timeout = 20_000)
    public void configurationScenarioResetObserverTest() {
        final String name = "configuration-reset-observer-test";
        try (Publisher publisher = Publisher.newPublisher(client, path, name);
             Subscriber subscriber = Subscriber.newSubscriber(client, path, name, Function.identity()::apply)
        ) {
            publisher.publish("data-1".getBytes(StandardCharsets.UTF_8));
            publisher.publish("data-2".getBytes(StandardCharsets.UTF_8));
            CountDownLatch counter = new CountDownLatch(1);
            subscriber.resetObserver(data -> {
                if (Arrays.equals(data, "data-3".getBytes(StandardCharsets.UTF_8))) {
                    counter.countDown();
                }
            });
            publisher.publish("data-3".getBytes(StandardCharsets.UTF_8));
            Assert.assertTrue(counter.await(20_000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            Assert.fail("Catch exception in configuration scenario test, exception = " + e.getMessage());
        }
    }

    @Test(timeout = 60_000)
    public void configurationScenarioStressTest1() throws InterruptedException {
        final String semaphoreName = SEMAPHORE_PREFIX + 1_000_001;
        final int n = 20;
        Publisher publisher = Publisher.newPublisher(client, path, semaphoreName);

        final String[] dataNow = new String[1];
        final CountDownLatch[] updateCounter = new CountDownLatch[1];

        updateCounter[0] = new CountDownLatch(n);
        dataNow[0] = "Nothing";
        publisher.publish(dataNow[0].getBytes(StandardCharsets.UTF_8));

        final List<Subscriber> subscribers = Stream.generate(() ->
                Subscriber.newSubscriber(client, path, semaphoreName, data -> {
                    if (Arrays.equals(data, dataNow[0].getBytes(StandardCharsets.UTF_8))) {
                        updateCounter[0].countDown();
                    }
                })
        ).limit(n).collect(Collectors.toList());

        updateCounter[0].await();
        dataNow[0] = "Second message";
        updateCounter[0] = new CountDownLatch(n);
        publisher.publish(dataNow[0].getBytes(StandardCharsets.UTF_8));

        updateCounter[0].await();
        publisher.close();
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
