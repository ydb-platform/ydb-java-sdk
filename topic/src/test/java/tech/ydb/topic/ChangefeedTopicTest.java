package tech.ydb.topic;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import tech.ydb.core.Status;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.TableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.events.SessionStartedEvent;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ChangefeedTopicTest {
    private final static String TEST_TABLE = "changefeed_table";
    private final static String TEST_CHANGEFEED = "updates";
    private final static String TEST_CONSUMER = "consumer";

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private static TopicClient topicClient;
    private static TableClient tableClient;

    @BeforeClass
    public static void initClients() {
        tableClient = TableClient.newClient(ydbTransport).build();
        topicClient = TopicClient.newClient(ydbTransport).build();
    }

    @AfterClass
    public static void closeClients() {
        topicClient.close();
        tableClient.close();
    }

    private void prepareChangefeed() {
        SessionRetryContext retryCtx = SessionRetryContext.create(tableClient).build();
        String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;
        String changefeedPath = tablePath + "/" + TEST_CHANGEFEED;

        retryCtx.supplyStatus(s -> s.executeSchemeQuery(
                "CREATE TABLE `" + tablePath + "` ("
                + "id Uint32, value Text, PRIMARY KEY (id)) "
                + "WITH ("
                + "  AUTO_PARTITIONING_MIN_PARTITIONS_COUNT = 2, "
                + "  PARTITION_AT_KEYS = (100));"
        )).join().expectSuccess("cannot create table");

        retryCtx.supplyStatus(s -> s.executeSchemeQuery(
                "ALTER TABLE `" + tablePath + "` "
                + "ADD CHANGEFEED " + TEST_CHANGEFEED
                + " WITH (FORMAT = 'JSON', MODE = 'NEW_IMAGE', INITIAL_SCAN = true, VIRTUAL_TIMESTAMPS = true);"
        )).join().expectSuccess("cannot alter table");

        retryCtx.supplyStatus(s -> s.executeSchemeQuery(""
                + "ALTER TOPIC `" + changefeedPath + "` ADD CONSUMER " + TEST_CONSUMER
        )).join().expectSuccess("cannot alter changefeed");

        retryCtx.supplyResult(s -> s.executeDataQuery(
                "INSERT INTO `" + tablePath + "` (id, value) VALUES (1, '1'), (1000, '2');",
                TxControl.serializableRw(), Params.empty()
        )).join().getStatus().expectSuccess("cannot insert data");
    }

    private Status dropChangefeed() {
        SessionRetryContext retryCtx = SessionRetryContext.create(tableClient).build();
        return retryCtx.supplyStatus(s -> s.dropTable(ydbTransport.getDatabase() + "/" + TEST_TABLE)).join();
    }

    @Test
    public void changefeedReadTest() throws Exception {
        prepareChangefeed();

        String changefeedPath = ydbTransport.getDatabase() + "/" + TEST_TABLE + "/" + TEST_CHANGEFEED;
        ReaderSettings rs = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(changefeedPath).build())
                .setConsumerName(TEST_CONSUMER)
                .build();

        TestHandler handler1 = new TestHandler(2, 1);
        AsyncReader reader1 = topicClient.createAsyncReader(rs, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(handler1).build());

        reader1.init().join();
        handler1.waitData();
        handler1.assertEvents("INIT", "START", "START", "DATA1", "DATA1");

        TestHandler handler2 = new TestHandler(1, 1);
        AsyncReader reader2 = topicClient.createAsyncReader(rs, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(handler2).build());

        reader2.init().join();
        handler2.waitData();

        handler1.assertEvents("INIT", "START", "START", "DATA1", "DATA1", "STOP");
        handler2.assertEvents("INIT", "START", "DATA1");

        dropChangefeed().expectSuccess("cannot drop changefeed");

        handler1.waitClosed();
        handler1.assertEvents("INIT", "START", "START", "DATA1", "DATA1", "STOP", "CLOSED");

        handler2.waitClosed();
        handler2.assertEvents("INIT", "START", "DATA1", "CLOSED");

        reader1.shutdown().join();
        handler1.assertEvents("INIT", "START", "START", "DATA1", "DATA1", "STOP", "CLOSED", "READER CLOSED");

        reader2.shutdown().join();
        handler2.assertEvents("INIT", "START", "DATA1", "CLOSED", "READER CLOSED");
    }

    private static class TestHandler implements ReadEventHandler {
        private final CountDownLatch dataLatch;
        private final CountDownLatch closedLatch;
        private final Queue<String> events = new ConcurrentLinkedQueue<>();

        public TestHandler(int expectedData, int expectedClosed) {
            this.dataLatch = new CountDownLatch(expectedData);
            this.closedLatch = new CountDownLatch(expectedClosed);
        }

        public void assertEvents(String... expected) {
            Iterator<String> it = events.iterator();
            for (String ex : expected) {
                Assert.assertTrue("Expected " + ex + " but have nothing", it.hasNext());
                Assert.assertEquals(ex, it.next());
            }
        }

        public void waitData() throws InterruptedException {
            Assert.assertTrue("Timed out waiting for data events", dataLatch.await(1, TimeUnit.MINUTES));
        }

        public void waitClosed() throws InterruptedException {
            Assert.assertTrue("Timed out waiting for closed events", closedLatch.await(1, TimeUnit.MINUTES));
        }

        @Override
        public void onMessages(DataReceivedEvent dre) {
            events.add("DATA" + dre.getMessages().size());
            dataLatch.countDown();
        }

        @Override
        public void onSessionStarted(SessionStartedEvent event) {
            events.add("INIT");
        }

        @Override
        public void onStartPartitionSession(StartPartitionSessionEvent event) {
            events.add("START");
            event.confirm();
        }

        @Override
        public void onStopPartitionSession(StopPartitionSessionEvent event) {
            events.add("STOP");
            event.confirm();
        }

        @Override
        public void onPartitionSessionClosed(PartitionSessionClosedEvent event) {
            events.add("CLOSED");
            closedLatch.countDown();
        }

        @Override
        public void onReaderClosed(ReaderClosedEvent event) {
            events.add("READER CLOSED");
        }
    }
}
