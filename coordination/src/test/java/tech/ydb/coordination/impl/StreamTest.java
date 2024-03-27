package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.proto.coordination.SessionRequest;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class StreamTest {
    private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
    private final Rpc rpc = Mockito.mock(Rpc.class);
    private final List<GrpcStreamMock> grpcMocks = new ArrayList<>();

    @Rule
    public final Timeout timeout = Timeout.seconds(10);

    @Before
    public void beforeEach() {
        Mockito.when(rpc.getScheduler()).thenReturn(scheduler);
        Mockito.when(rpc.getDatabase()).thenReturn("/mocked");
        Mockito.when(rpc.createSession(Mockito.any())).thenAnswer(i -> {
            GrpcStreamMock mock = new GrpcStreamMock(r -> r.run());
            grpcMocks.add(mock);
            return mock;
        });
    }

    @Test
    public void baseConnectTest() {
        Assert.assertTrue(grpcMocks.isEmpty());

        Stream stream = new Stream(rpc);
        Assert.assertEquals(1, grpcMocks.size());
        GrpcStreamMock grpc = grpcMocks.get(0);

        Assert.assertFalse(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());
        Assert.assertFalse(grpc.hasNextRequest());

        CompletableFuture<Status> finished = stream.startStream();
        CompletableFuture<Result<Long>> connected = stream.sendSessionStart(0, "demo", Duration.ZERO, ByteString.EMPTY);

        Assert.assertFalse(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());
        Assert.assertFalse(finished.isDone());
        Assert.assertFalse(connected.isDone());

        Assert.assertTrue(grpc.hasNextRequest());
        SessionRequest startSession = grpc.pollNextRequest();
        Assert.assertTrue(startSession.hasSessionStart());
        Assert.assertEquals(0, startSession.getSessionStart().getTimeoutMillis());
        Assert.assertEquals("demo", startSession.getSessionStart().getPath());
        Assert.assertEquals(0, startSession.getSessionStart().getProtectionKey().size());

        Assert.assertFalse(grpc.hasNextRequest());
        grpc.responseSessionStarted(1, 1000);

        Assert.assertFalse(finished.isDone());
        Assert.assertTrue(connected.isDone());

        long sessionID = connected.join().getValue();
        Assert.assertEquals(1l, sessionID);


        CompletableFuture<Status> stopped = stream.stop();

        Assert.assertFalse(finished.isDone());
        Assert.assertFalse(stopped.isDone());

        Assert.assertTrue(grpc.hasNextRequest());
        SessionRequest stopSession = grpc.pollNextRequest();
        Assert.assertTrue(stopSession.hasSessionStop());
        Assert.assertNotNull(startSession.getSessionStop());

        Assert.assertFalse(finished.isDone());
        Assert.assertFalse(stopped.isDone());

        grpc.responseSessionStopped(1);

        Assert.assertFalse(stopped.isDone());
        Assert.assertFalse(finished.isDone());
        Assert.assertTrue(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());

        grpc.closeConnectionOK();

        Assert.assertTrue(stopped.join().isSuccess());
        Assert.assertTrue(finished.isDone());
        Assert.assertTrue(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());
    }
}
