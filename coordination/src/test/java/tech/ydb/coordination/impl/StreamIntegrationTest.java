package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.test.junit4.GrpcTransportRule;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class StreamIntegrationTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();

    private static final Rpc RPC = new RpcImpl(YDB_TRANSPORT);

    @Rule
    public final Timeout testTimeoutRule = new Timeout(10, TimeUnit.SECONDS);

    @Test
    public void stopBeforeStartTest() {
        Stream stream = new Stream(RPC);
        Status stopped = stream.stop().join();

        Assert.assertEquals(StatusCode.CLIENT_GRPC_ERROR, stopped.getCode());
        Assert.assertEquals(1, stopped.getIssues().length);
        Issue issue = stopped.getIssues()[0];
        Assert.assertTrue(issue.getMessage().startsWith("gRPC error: (INVALID_ARGUMENT) on"));
        Assert.assertTrue(issue.getMessage().endsWith("First message must be a SessionStart"));
    }

    @Test
    public void closeWithoutStartTest() {
        Stream stream = new Stream(RPC);

        stream.closeStream();

        CompletableFuture<Result<Long>> start = stream.sendSessionStart(0, "test", Duration.ZERO, ByteString.EMPTY);
        Assert.assertTrue(start.isDone());
        Assert.assertEquals(StatusCode.CLIENT_CANCELLED, start.join().getStatus().getCode());

        CompletableFuture<Status> stop = stream.stop();
        Assert.assertEquals(StatusCode.CLIENT_CANCELLED, stop.join().getCode());

        stream.closeStream(); // no-op
    }
}
