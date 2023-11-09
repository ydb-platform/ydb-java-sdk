package tech.ydb.query;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(QueryIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @Test
    public void testQueryClient() {
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

    public void printQuerySetPart(QueryResultPart part) {
        ResultSetReader rs = part.getResultSetReader();
        logger.info("got query result part with index {} and {} rows", part.getResultSetIndex(), rs.getRowCount());
    }

    @Test
    public void testSchemeQuery() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                TxMode tx = TxMode.serializableRw();
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder().build();

                CompletableFuture<Status> createTable = session
                        .executeQuery("CREATE TABLE demo_table (id Int32, data Text, PRIMARY KEY(id));", tx, settings)
                        .start(this::printQuerySetPart);
                createTable.join().expectSuccess();


                CompletableFuture<Status> dropTable = session
                        .executeQuery("DROP TABLE demo_table;", tx, settings)
                        .start(this::printQuerySetPart);
                dropTable.join().expectSuccess();
            }
        }
    }
}
