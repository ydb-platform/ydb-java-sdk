package tech.ydb.query;


import java.time.Duration;

import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryIntegrationTest {
    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @Test
    public void testQueryClient() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue();
            session.close();
        }
    }
}
