package tech.ydb.core.operation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.OperationProtos.GetOperationResponse;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;
import tech.ydb.proto.table.YdbTable.ExplainDataQueryResponse;
import tech.ydb.proto.table.YdbTable.ExplainQueryResult;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class OperationTrayTest {
    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);

    private final Function<Result<ExplainDataQueryResponse>, Operation<Result<ExplainQueryResult>>> binder =
            OperationBinder.bindAsync(transport, ExplainDataQueryResponse::getOperation, ExplainQueryResult.class);

    private final Function<Result<ExplainQueryResult>, Result<String>> mapper =
            r -> r.map(ExplainQueryResult::getQueryPlan);

    @Before
    public void prepare() {
        Mockito.when(transport.getScheduler()).thenReturn(scheduler);
//        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyInt(), Mockito.any())).thenAnswer(
//                (InvocationOnMock iom) -> {
//                    scheduledActions.offer(iom.getArgument(0, Runnable.class));
//                    return null;
//                });
    }

    @Test
    public void errorOperationTest() {
        Status error = Status.of(StatusCode.UNAVAILABLE);
        Operation<Result<String>> operation = binder.apply(Result.fail(error)).transform(mapper);

        Assert.assertTrue(operation.isReady());
        Assert.assertNull(operation.getId());

        CompletableFuture<Result<String>> future = OperationTray.fetchOperation(operation, 100);

        Assert.assertTrue(future.isDone());
        Assert.assertEquals(Status.of(StatusCode.UNAVAILABLE), future.join().getStatus());

        assertMockCalls(0, 0);
    }

    @Test
    public void failedOperationTest() {
        ExplainDataQueryResponse response = ExplainDataQueryResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setStatus(StatusCodesProtos.StatusIds.StatusCode.NOT_FOUND)
                                .setId("error-id")
                                .setReady(true)
                                .build())
                .build();

        Operation<Result<String>> operation = binder.apply(Result.success(response)).transform(mapper);

        Assert.assertTrue(operation.isReady());
        Assert.assertEquals("error-id", operation.getId());

        CompletableFuture<Result<String>> future = OperationTray.fetchOperation(operation, 100);

        Assert.assertTrue(future.isDone());
        Assert.assertEquals(Status.of(StatusCode.NOT_FOUND), future.join().getStatus());

        assertMockCalls(0, 0);
    }

    @Test
    public void readyOperationTest() {
        Any data = Any.pack(ExplainQueryResult.newBuilder().setQueryPlan("plan").build());
        ExplainDataQueryResponse response = ExplainDataQueryResponse.newBuilder()
                .setOperation(OperationProtos.Operation.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .setId("ready-id")
                        .setReady(true)
                        .setResult(data)
                        .build()
                ).build();

        Operation<Result<String>> operation = binder.apply(Result.success(response)).transform(mapper);

        Assert.assertTrue(operation.isReady());
        Assert.assertEquals("ready-id", operation.getId());

        CompletableFuture<Result<String>> future = OperationTray.fetchOperation(operation, 5);

        Assert.assertTrue(future.isDone());
        Assert.assertEquals(Status.SUCCESS, future.join().getStatus());
        Assert.assertEquals("plan", future.join().getValue());

        assertMockCalls(0, 0);
    }

    @Test
    public void notReadyOperationTest() {
        String id = "op1";

        final Queue<Runnable> scheduled = new LinkedList<>();
        CompletableFuture<Result<GetOperationResponse>> getOperation1 = new CompletableFuture<>();
        CompletableFuture<Result<GetOperationResponse>> getOperation2 = new CompletableFuture<>();
        CompletableFuture<Result<GetOperationResponse>> getOperation3 = new CompletableFuture<>();

        Mockito.when(transport.unaryCall(
                Mockito.eq(OperationServiceGrpc.getGetOperationMethod()), Mockito.any(), Mockito.any()
        )).thenReturn(getOperation1).thenReturn(getOperation2).thenReturn(getOperation3);

        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any()))
                .thenAnswer((InvocationOnMock iom) -> {
                    scheduled.offer(iom.getArgument(0, Runnable.class));
                    return null;
                });

        ExplainDataQueryResponse notReady = ExplainDataQueryResponse.newBuilder()
                .setOperation(OperationProtos.Operation.newBuilder()
                        .setId(id)
                        .setReady(false)
                        .build()
                ).build();
        Operation<Result<String>> operation = binder.apply(Result.success(notReady)).transform(mapper);

        Assert.assertFalse(operation.isReady());
        Assert.assertEquals(id, operation.getId());
        assertMockCalls(0, 0);

        CompletableFuture<Result<String>> future = OperationTray.fetchOperation(operation, 5);
        Assert.assertFalse(future.isDone());

        assertMockCalls(1, 0);
        completeNotReady(getOperation1, id);

        assertMockCalls(1, 1);
        Assert.assertFalse(scheduled.isEmpty());
        scheduled.poll().run();
        Assert.assertTrue(scheduled.isEmpty());

        assertMockCalls(2, 1);
        completeNotReady(getOperation2, id);

        assertMockCalls(2, 2);
        Assert.assertFalse(scheduled.isEmpty());
        scheduled.poll().run();
        Assert.assertTrue(scheduled.isEmpty());

        assertMockCalls(3, 2);
        completeReady(getOperation3, id, "hello_plan");
        assertMockCalls(3, 2);
        Assert.assertTrue(scheduled.isEmpty());

        Assert.assertTrue(future.isDone());
        Assert.assertEquals(Status.SUCCESS, future.join().getStatus());
        Assert.assertEquals("hello_plan", future.join().getValue());
    }

    private void assertMockCalls(int getOperationCount, int scheduleCount) {
        Mockito.verify(transport, Mockito.times(getOperationCount))
                .unaryCall(Mockito.eq(OperationServiceGrpc.getGetOperationMethod()), Mockito.any(), Mockito.any());
        Mockito.verify(scheduler, Mockito.times(scheduleCount))
                .schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any());
    }

    private void completeNotReady(CompletableFuture<Result<GetOperationResponse>> future, String id) {
        GetOperationResponse result = GetOperationResponse.newBuilder().setOperation(
                OperationProtos.Operation.newBuilder()
                        .setId(id)
                        .setReady(false)
                        .build()
        ).build();
        future.complete(Result.success(result));
    }

    private void completeReady(CompletableFuture<Result<GetOperationResponse>> future, String id, String plan) {
        Any data = Any.pack(ExplainQueryResult.newBuilder().setQueryPlan(plan).build());
        GetOperationResponse result = GetOperationResponse.newBuilder().setOperation(
                OperationProtos.Operation.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .setId(id)
                        .setReady(true)
                        .setResult(data)
                        .build()
        ).build();
        future.complete(Result.success(result));
    }
}
