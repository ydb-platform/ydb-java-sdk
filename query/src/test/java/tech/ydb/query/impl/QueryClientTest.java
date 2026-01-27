package tech.ydb.query.impl;

import java.time.Duration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.test.junit4.YdbHelperRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryClientTest {

    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static final GrpcTestInterceptor grpcInterceptor = new GrpcTestInterceptor();

    private static GrpcTransport transport;

    private static QueryClient queryClient;

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
        queryClient = QueryClient.newClient(transport).build();
    }

    @After
    public void closeTableClient() {
        queryClient.close();
    }

    private QuerySession getSession() {
        return queryClient.createSession(Duration.ofSeconds(5)).join().getValue();
    }

    @Test
    public void sessionReuseTest() {
        QuerySession s1 = getSession();
        String id1 = s1.getId();
        s1.close();

        QuerySession s2 = getSession();
        Assert.assertEquals(id1, s2.getId());

        QuerySession s3 = getSession();
        Assert.assertNotEquals(id1, s3.getId());
        String id2 = s3.getId();

        s2.close();
        s3.close();

        QuerySession s4 = getSession();
        QuerySession s5 = getSession();

        Assert.assertEquals(id2, s4.getId());
        Assert.assertEquals(id1, s5.getId());

        s4.close();
        s5.close();
    }

    @Test
    public void sessionExecuteQueryTest() {
        QuerySession s1 = getSession();
        String id1 = s1.getId();

        grpcInterceptor.addOverrideStatus(io.grpc.Status.UNAVAILABLE);

        Result<QueryInfo> res = s1.createQuery("SELECT 1 + 2", TxMode.NONE).execute().join();
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, res.getStatus().getCode());

        res = s1.createQuery("SELECT 1 + 2", TxMode.NONE).execute().join();
        Assert.assertEquals(StatusCode.SUCCESS, res.getStatus().getCode());

        s1.close();

        QuerySession s2 = getSession();
        Assert.assertNotEquals(id1, s2.getId());
        String id2 = s2.getId();

        res = s2.createQuery("SELECT * FROM wrongTable", TxMode.NONE).execute().join();
        Assert.assertEquals(StatusCode.SCHEME_ERROR, res.getStatus().getCode());

        s2.close();

        try (QuerySession s3 = getSession()) {
            Assert.assertEquals(id2, s3.getId());
        }
    }
}
