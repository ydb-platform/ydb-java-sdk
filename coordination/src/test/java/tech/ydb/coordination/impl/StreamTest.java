package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
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
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        Mockito.when(rpc.getScheduler()).thenReturn(scheduler);
        Mockito.when(rpc.getDatabase()).thenReturn("/mocked");
        Mockito.when(rpc.createSession(Mockito.any())).thenAnswer(i -> {
            GrpcStreamMock mock = new GrpcStreamMock(Runnable::run);
            grpcMocks.add(mock);
            return mock;
        });

        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any()))
                .thenReturn(Mockito.mock(ScheduledFuture.class));

        Assert.assertTrue(grpcMocks.isEmpty());
    }

    @Test
    public void baseConnectTest() {
        Stream stream = new Stream(rpc);
        Assert.assertEquals(1, grpcMocks.size());
        GrpcStreamMock grpc = grpcMocks.get(0);

        Assert.assertFalse(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());
        Assert.assertFalse(grpc.hasNextRequest());

        CompletableFuture<Status> finished = stream.getFinishedFuture();
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

    @Test
    public void closeWithoutStartTest() {
        Assert.assertTrue(grpcMocks.isEmpty());

        Stream stream = new Stream(rpc);
        Assert.assertEquals(1, grpcMocks.size());
        GrpcStreamMock grpc = grpcMocks.get(0);

        Assert.assertFalse(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());
        Assert.assertFalse(grpc.hasNextRequest());

        stream.closeStream();

        Assert.assertTrue(grpc.isClosed());
        Assert.assertFalse(grpc.isCanceled());
        Assert.assertFalse(grpc.hasNextRequest());
    }

    @Test
    public void connectTimeoutTest() {
        Assert.assertTrue(grpcMocks.isEmpty());

        Stream stream = new Stream(rpc);
        Assert.assertEquals(1, grpcMocks.size());
        GrpcStreamMock grpc = grpcMocks.get(0);

        CompletableFuture<Result<Long>> start = stream.sendSessionStart(0, "/test", Duration.ofMillis(100), ByteString.EMPTY);

        Assert.assertFalse(start.isDone());
        Assert.assertTrue(grpc.hasNextRequest());

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> taskTimeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> taskUnit = ArgumentCaptor.forClass(TimeUnit.class);
        Mockito.verify(scheduler, Mockito.times(1)).schedule(task.capture(), taskTimeout.capture(), taskUnit.capture());

        Assert.assertEquals(1000L, taskTimeout.getValue().longValue()); // use STREAM_CANCEL_TIMEOUT_MS
        Assert.assertEquals(TimeUnit.MILLISECONDS, taskUnit.getValue());

        task.getValue().run();

        Assert.assertFalse(grpc.isClosed());
        Assert.assertTrue(grpc.isCanceled());
        Assert.assertTrue(grpc.hasNextRequest());
        grpc.closeConnectionCancelled();

        Assert.assertTrue(start.isDone());
        Assert.assertEquals(StatusCode.CLIENT_CANCELLED, start.join().getStatus().getCode());
        Assert.assertEquals(StatusCode.CLIENT_CANCELLED, stream.stop().join().getCode());
    }
}
