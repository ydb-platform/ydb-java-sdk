package tech.ydb.topic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicWritersIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(TopicWritersIntegrationTest.class);

    private final static FailableWriterInterceptor PROXY = new FailableWriterInterceptor();

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule()
            .withGrpcTransportCustomizer(b -> b.addChannelInitializer(PROXY));

    private final static String TEST_TOPIC = "topic_writers_test";

    private final static String TEST_PRODUCER1 = "producer";
    private final static String TEST_CONSUMER1 = "consumer";

    private static TopicClient client;

    @BeforeClass
    public static void initClient() {
        client = TopicClient.newClient(ydbTransport).build();
    }

    @AfterClass
    public static void closeClient() {
        client.close();
    }

    @Before
    public void initTopic() {
        PROXY.reset();

        logger.info("Create test topic  {} ...", TEST_TOPIC);
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .build())
                .join().expectSuccess("can't create a new topic");
    }

    @After
    public void dropTable() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        client.dropTopic(TEST_TOPIC).join();
    }

    private void assertTopicContent(List<byte[]> messages) {
        try {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic(
                    TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build()
            ).setConsumerName(TEST_CONSUMER1).build());

            reader.initAndWait();
            int idx = 0;
            DeferredCommitter committer = DeferredCommitter.newInstance();
            for (byte[] expected: messages) {
                tech.ydb.topic.read.Message next = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull("Expected message " + idx, next);
                Assert.assertArrayEquals("Unexpected content for message " + idx, expected, next.getData());
                idx++;

                committer.add(next);
            }

            committer.commit();
            reader.shutdown();
        } catch (InterruptedException ex) {
            throw new AssertionError("Unexpected exception", ex);
        }
    }

    @Test
    public void messageBufferOverflowTest() throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId(TEST_PRODUCER1)
                .setMaxSendBufferMemorySize(1000)
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);

        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));

        writer.flush();
        writer.shutdown(10, TimeUnit.SECONDS);

        assertTopicContent(Arrays.asList(msg1, msg1, msg1, msg2, msg1, msg2, msg2, msg2, msg1, msg1));
    }

    @Test
    public void lazyInitTest() throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId(TEST_PRODUCER1)
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);

        CountDownLatch latch = new CountDownLatch(1);
        List<byte[]> written = new ArrayList<>();
        CompletableFuture<WriteAck> lastMessage = CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            try {
                CompletableFuture<WriteAck> ack = FutureTools.failedFuture(new RuntimeException("not started"));
                for (int idx = 0; idx < 100; idx++) {
                    byte[] msg = new byte[1000];
                    rnd.nextBytes(msg);
                    ack = writer.send(Message.of(msg));
                    written.add(msg);
                }
                latch.countDown();
                return ack.join();
            } catch (QueueOverflowException ex) {
                latch.countDown();
                throw new RuntimeException(ex);
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        writer.init();

        WriteAck ack = lastMessage.get(10, TimeUnit.SECONDS);
        Assert.assertEquals(WriteAck.State.WRITTEN, ack.getState());

        writer.shutdown().join();

        assertTopicContent(written);
    }

    @Test
    public void doubleInitTest() throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId(TEST_PRODUCER1)
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);

        writer.init();
        writer.init();

        byte[] msg = "hello".getBytes();
        writer.send(Message.of(msg)).join();

        writer.shutdown().join();

        assertTopicContent(Collections.singletonList(msg));
    }

    @Test
    public void defaultRetryPolicyWriter() throws Exception {
        // errors pattern in order of processing
        PROXY.unavailableOnAckWithSeqNo(15);
        PROXY.badRequestOnInit(2);
        PROXY.badSessionOnSendMsgWithSeqNo(35);
        PROXY.unavailableOnInit(4);
        PROXY.unavailableOnInit(5);
        PROXY.unavailableOnInit(6);
        PROXY.badRequestOnAckWithSeqNo(60);
        PROXY.unavailableOnAckWithSeqNo(90);

        List<StatusCode> expectedErrors = Arrays.asList(
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.BAD_SESSION,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.TRANSPORT_UNAVAILABLE
        );

        List<StatusCode> realErrors = new ArrayList<>();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId(TEST_PRODUCER1)
                .setErrorsHandler((st, th) -> {
                    if (st != null) {
                        realErrors.add(st.getCode());
                    }
                    if (th != null) {
                        realErrors.add(StatusCode.CLIENT_INTERNAL_ERROR);
                    }
                })
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        List<byte[]> written = new ArrayList<>();
        for (int batch = 0; batch < 10; batch++) {
            for (int idx = 0; idx < 10; idx++) {
                byte[] msg = new byte[1000];
                Arrays.fill(msg, (byte) (batch * 10 + idx));
                writer.send(Message.of(msg), 1, TimeUnit.MINUTES);
                written.add(msg);
            }
            writer.flush();
        }

        writer.shutdown(10, TimeUnit.SECONDS);

        Assert.assertEquals(expectedErrors, realErrors);
        assertTopicContent(written);
    }
}
