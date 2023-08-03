package tech.ydb.query;


import java.time.Duration;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryIntegrationTest {
    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @Test
    public void testQueryClient() throws InterruptedException {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                session.executeQuery("SELECT 2 + 3;",
                        TxMode.serializableRw(),
                        ExecuteQuerySettings.newBuilder().build()
                ).start(part -> {
                    ResultSetReader rs = part.getResultSetReader();

                    Assert.assertTrue(rs.next());
                    Assert.assertEquals(1, rs.getColumnCount());
                    Assert.assertEquals("column0", rs.getColumnName(0));
                    Assert.assertEquals(5, rs.getColumn(0).getInt32());

                    Assert.assertFalse(rs.next());
                }).join().expectSuccess();
            }
        }
    }
}
