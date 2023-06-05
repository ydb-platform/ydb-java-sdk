package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Any;
import org.apache.logging.log4j.core.config.CronScheduledFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;
import tech.ydb.OperationProtos;
import tech.ydb.StatusCodesProtos;
import tech.ydb.StatusCodesProtos.StatusIds;
import tech.ydb.YdbIssueMessage.IssueMessage;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.operation.v1.OperationServiceGrpc;
import tech.ydb.table.YdbTable;
import tech.ydb.table.v1.TableServiceGrpc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Kurdyukov
 */
public class OperationManagerTest {

    private static final String OPERATION_ID = "123";

    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final ScheduledExecutorServiceTest scheduledExecutorServiceTest = new ScheduledExecutorServiceTest();

    private OperationManager operationManager;

    @Before
    public void before() {
        when(transport.getScheduler()).thenReturn(scheduledExecutorServiceTest);

        operationManager = new OperationManager(transport);
    }

    @Test
    public void successWithoutIssues() {
        Status s = OperationManager.status(OperationProtos.Operation.newBuilder()
            .setStatus(StatusIds.StatusCode.SUCCESS)
            .setId("some-id")
            .setReady(true)
            .build());

        assertSame(Status.SUCCESS, s);
        assertEquals(0, s.getIssues().length);
    }

    @Test
    public void successWithIssues() {
        Status s = OperationManager.status(OperationProtos.Operation.newBuilder()
            .setStatus(StatusIds.StatusCode.SUCCESS)
            .setId("some-id")
            .setReady(true)
            .addIssues(IssueMessage.newBuilder()
                .setIssueCode(12345)
                .setSeverity(Issue.Severity.INFO.getCode())
                .setMessage("some-issue")
                .build())
            .build());

        assertTrue(s.isSuccess());
        assertArrayEquals(new Issue[]{
            Issue.of(12345, "some-issue", Issue.Severity.INFO)
        }, s.getIssues());
    }

    @Test
    public void completeSuccessOperation() {
        mockExplainDataQueryMethodTransport(createResult(true, StatusCodesProtos.StatusIds.StatusCode.SUCCESS));

        checkSuccessOperation(resultUnwrap());
    }

    @Test
    public void completeFailOperation() {
        mockExplainDataQueryMethodTransport(createResult(true,
                StatusCodesProtos.StatusIds.StatusCode.BAD_SESSION));

        Result<YdbTable.ExplainQueryResult> result = resultUnwrap().join();

        Assert.assertEquals(Status.of(StatusCode.BAD_SESSION), result.getStatus());
    }

    @Test
    public void failUnwrapOperation() {
        mockExplainDataQueryMethodTransport(Result.fail(Status.of(StatusCode.BAD_SESSION)));

        Result<YdbTable.ExplainQueryResult> result = resultUnwrap().join();

        Assert.assertEquals(Status.of(StatusCode.BAD_SESSION), result.getStatus());
    }

    @Test
    public void completeSuccessPollingOperation() {
        mockExplainDataQueryMethodTransport(createResult(false,
                StatusCodesProtos.StatusIds.StatusCode.SUCCESS));

        CompletableFuture<Result<YdbTable.ExplainQueryResult>> resultCompletableFuture = resultUnwrap();

        Assert.assertFalse(resultCompletableFuture.isDone());

        mockGetOperationMethodTransport(StatusCodesProtos.StatusIds.StatusCode.SUCCESS);
        scheduledExecutorServiceTest.execCommand();

        checkSuccessOperation(resultCompletableFuture);
    }

    @Test
    public void completeFailPollingOperation() {
        mockExplainDataQueryMethodTransport(createResult(false,
                StatusCodesProtos.StatusIds.StatusCode.SUCCESS));

        CompletableFuture<Result<YdbTable.ExplainQueryResult>> resultCompletableFuture = resultUnwrap();

        Assert.assertFalse(resultCompletableFuture.isDone());

        mockGetOperationMethodTransport(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST);
        scheduledExecutorServiceTest.execCommand();

        Assert.assertEquals(Status.of(StatusCode.BAD_REQUEST), resultCompletableFuture.join().getStatus());
    }

    @Test
    public void cancelPollingOperation() {
        mockExplainDataQueryMethodTransport(createResult(false, StatusCodesProtos.StatusIds.StatusCode.SUCCESS));

        Operation<YdbTable.ExplainQueryResult> resultCompletableFuture = operationUnwrap();

        mockCancelOperationMethodTransport(
                Result.success(
                        OperationProtos.CancelOperationResponse
                                .getDefaultInstance()
                )
        );
        resultCompletableFuture.cancel();

        mockGetOperationMethodTransport(StatusCodesProtos.StatusIds.StatusCode.SUCCESS);
        scheduledExecutorServiceTest.execCommand();

        Result<YdbTable.ExplainQueryResult> result = resultCompletableFuture.getResultFuture().join();

        Assert.assertEquals(Status.of(StatusCode.CANCELLED), result.getStatus());
    }

    @Test
    public void failCancelThenSuccessPollingOperation() {
        mockExplainDataQueryMethodTransport(createResult(false, StatusCodesProtos.StatusIds.StatusCode.SUCCESS));

        Operation<YdbTable.ExplainQueryResult> resultCompletableFuture = operationUnwrap();

        mockCancelOperationMethodTransport(Result.fail(Status.of(StatusCode.BAD_SESSION)));
        resultCompletableFuture.cancel();

        mockGetOperationMethodTransport(StatusCodesProtos.StatusIds.StatusCode.SUCCESS);
        scheduledExecutorServiceTest.execCommand();

        checkSuccessOperation(resultCompletableFuture.getResultFuture());
    }

    private Operation<YdbTable.ExplainQueryResult> operationUnwrap() {
        return transport
                .unaryCall(
                        TableServiceGrpc.getExplainDataQueryMethod(),
                        GrpcRequestSettings.newBuilder().build(),
                        YdbTable.ExplainDataQueryRequest.getDefaultInstance()
                )
                .thenApply(operationManager
                        .operationUnwrapper(
                                YdbTable.ExplainDataQueryResponse::getOperation,
                                YdbTable.ExplainQueryResult.class
                        )
                ).join();
    }

    private static void checkSuccessOperation(
            CompletableFuture<Result<YdbTable.ExplainQueryResult>> resultCompletableFuture
    ) {
        Result<YdbTable.ExplainQueryResult> result = resultCompletableFuture.join();

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(YdbTable.ExplainQueryResult.getDefaultInstance(), result.getValue());
    }

    private void mockCancelOperationMethodTransport(Result<OperationProtos.CancelOperationResponse> responseResult) {
        when(transport
                .unaryCall(
                        eq(OperationServiceGrpc.getCancelOperationMethod()),
                        any(GrpcRequestSettings.class),
                        eq(
                                OperationProtos.CancelOperationRequest
                                        .newBuilder()
                                        .setId(OPERATION_ID)
                                        .build()
                        )
                )
        ).thenReturn(
                CompletableFuture
                        .completedFuture(responseResult)
        );
    }

    private void mockGetOperationMethodTransport(StatusCodesProtos.StatusIds.StatusCode statusCode) {
        when(transport
                .unaryCall(
                        eq(OperationServiceGrpc.getGetOperationMethod()),
                        any(GrpcRequestSettings.class),
                        eq(
                                OperationProtos.GetOperationRequest
                                        .newBuilder()
                                        .setId(OPERATION_ID)
                                        .build()
                        )
                )
        ).thenReturn(CompletableFuture
                .completedFuture(
                        Result.success(
                                OperationProtos.GetOperationResponse.newBuilder()
                                        .setOperation(createOperation(true, statusCode))
                                        .build()
                        )
                )
        );
    }

    private CompletableFuture<Result<YdbTable.ExplainQueryResult>> resultUnwrap() {
        return transport
                .unaryCall(
                        TableServiceGrpc.getExplainDataQueryMethod(),
                        GrpcRequestSettings.newBuilder().build(),
                        YdbTable.ExplainDataQueryRequest.getDefaultInstance()
                )
                .thenApply(operationManager
                        .operationUnwrapper(
                                YdbTable.ExplainDataQueryResponse::getOperation,
                                YdbTable.ExplainQueryResult.class
                        )
                ).thenCompose(Operation::getResultFuture);
    }

    private static Result<YdbTable.ExplainDataQueryResponse> createResult(
            boolean ready,
            StatusCodesProtos.StatusIds.StatusCode statusCode
    ) {
        return Result.success(YdbTable.ExplainDataQueryResponse
                .newBuilder()
                .setOperation(
                        createOperation(
                                ready,
                                statusCode
                        )
                )
                .build()
        );
    }

    private static OperationProtos.Operation createOperation(
            boolean ready,
            StatusCodesProtos.StatusIds.StatusCode statusCode
    ) {
        return OperationProtos.Operation
                .newBuilder()
                .setReady(ready)
                .setId(OPERATION_ID)
                .setStatus(statusCode)
                .setResult(Any.pack(YdbTable.ExplainQueryResult.getDefaultInstance()))
                .build();
    }

    private void mockExplainDataQueryMethodTransport(Result<YdbTable.ExplainDataQueryResponse> responseResult) {
        when(transport
                .unaryCall(
                        eq(TableServiceGrpc.getExplainDataQueryMethod()),
                        any(GrpcRequestSettings.class),
                        any(YdbTable.ExplainDataQueryRequest.class)
                )
        ).thenReturn(CompletableFuture
                .completedFuture(responseResult)
        );
    }

    private static class ScheduledExecutorServiceTest extends ScheduledThreadPoolExecutor {
        private Runnable command;

        public ScheduledExecutorServiceTest() {
            super(0);
        }

        public ScheduledExecutorServiceTest(int corePoolSize) {
            super(corePoolSize);
        }

        public ScheduledExecutorServiceTest(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        public ScheduledExecutorServiceTest(int corePoolSize, RejectedExecutionHandler handler) {
            super(corePoolSize, handler);
        }

        public ScheduledExecutorServiceTest(int corePoolSize, ThreadFactory threadFactory,
                                            RejectedExecutionHandler handler) {
            super(corePoolSize, threadFactory, handler);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            this.command = command;

            // unused scheduled future
            return new CronScheduledFuture<>(null, null);
        }

        public void execCommand() {
            command.run();
        }
    }
}
