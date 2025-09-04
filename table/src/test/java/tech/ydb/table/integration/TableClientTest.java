package tech.ydb.table.integration;

import java.time.Duration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.test.junit4.YdbHelperRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TableClientTest {

    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static final GrpcTestInterceptor grpcInterceptor = new GrpcTestInterceptor();

    private static GrpcTransport transport;

    private static TableClient tableClient;

    @BeforeClass
    public static void initTransport() {
        transport = GrpcTransport.forEndpoint(YDB.endpoint(), YDB.database())
                .withAuthProvider(new TokenAuthProvider(YDB.authToken()))
                .addChannelInitializer(grpcInterceptor)
                .build();
    }

    @AfterClass
    public static void closeTransport() {
        transport.close();
    }

    @Before
    public void initTableClient() {
        grpcInterceptor.reset();
        tableClient = TableClient.newClient(transport).build();
    }

    @After
    public void closeTableClient() {
        tableClient.close();
    }

    private Session getSession() {
        return tableClient.createSession(Duration.ofSeconds(5)).join().getValue();
    }

    @Test
    public void sessionReuseTest() {
        Session s1 = getSession();
        String id1 = s1.getId();
        s1.close();

        Session s2 = getSession();
        Assert.assertEquals(id1, s2.getId());

        Session s3 = getSession();
        Assert.assertNotEquals(id1, s3.getId());
        String id2 = s3.getId();

        s2.close();
        s3.close();

        Session s4 = getSession();
        Session s5 = getSession();

        Assert.assertEquals(id2, s4.getId());
        Assert.assertEquals(id1, s5.getId());

        s4.close();
        s5.close();
    }

    @Test
    public void sessionExecuteDataQueryTest() {
        Session s1 = getSession();
        String id1 = s1.getId();

        grpcInterceptor.addOverrideStatus(io.grpc.Status.UNAVAILABLE);

        Result<DataQueryResult> res = s1.executeDataQuery("SELECT 1 + 2", TxControl.snapshotRo()).join();
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, res.getStatus().getCode());

        res = s1.executeDataQuery("SELECT 1 + 2", TxControl.snapshotRo()).join();
        Assert.assertEquals(StatusCode.SUCCESS, res.getStatus().getCode());

        s1.close();

        Session s2 = getSession();
        Assert.assertNotEquals(id1, s2.getId());
        String id2 = s2.getId();

        res = s2.executeDataQuery("SELECT * FROM wrongTable", TxControl.snapshotRo()).join();
        Assert.assertEquals(StatusCode.SCHEME_ERROR, res.getStatus().getCode());

        s2.close();

        try (Session s3 = getSession()) {
            Assert.assertEquals(id2, s3.getId());
        }
    }

    @Test
    public void sessionExecuteScanQueryTest() {
        ExecuteScanQuerySettings settings = ExecuteScanQuerySettings.newBuilder().build();

        Session s1 = getSession();
        String id1 = s1.getId();

        grpcInterceptor.addOverrideStatus(io.grpc.Status.UNAVAILABLE);

        Status res = s1.executeScanQuery("SELECT 1 + 2", Params.empty(), settings).start(rsr -> {}).join();
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, res.getCode());

        res = s1.executeScanQuery("SELECT 1 + 2", Params.empty(), settings).start(rsr -> {}).join();
        Assert.assertEquals(StatusCode.SUCCESS, res.getCode());

        s1.close();

        Session s2 = getSession();
        Assert.assertNotEquals(id1, s2.getId());
        String id2 = s2.getId();

        res = s2.executeScanQuery("SELECT * FROM wrongTable", Params.empty(), settings).start(rsr -> {}).join();
        Assert.assertEquals(StatusCode.SCHEME_ERROR, res.getCode());

        s2.close();

        try (Session s3 = getSession()) {
            Assert.assertEquals(id2, s3.getId());
        }
    }
}
