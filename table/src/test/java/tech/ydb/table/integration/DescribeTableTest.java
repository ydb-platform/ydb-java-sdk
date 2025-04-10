package tech.ydb.table.integration;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DescribeTableTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final SessionRetryContext CTX = SessionRetryContext.create(SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(YDB_TRANSPORT)
    ).build()).build();

    @Test
    public void wrongTypeTest() {
        String databasePath = YDB_TRANSPORT.getDatabase();
        Status status = CTX.supplyResult(s -> s.describeTable(databasePath)).join().getStatus();

        Assert.assertFalse("Unexpected success", status.isSuccess());
        Assert.assertEquals(StatusCode.SCHEME_ERROR, status.getCode());
        Issue[] issues = status.getIssues();
        Assert.assertEquals(1, issues.length);
        String entryName = databasePath.substring(1); // remove lead /
        Assert.assertEquals("Entry " + entryName + " with type DIRECTORY is not a table", issues[0].getMessage());
        Assert.assertEquals(Issue.Severity.ERROR, issues[0].getSeverity());
    }
}
