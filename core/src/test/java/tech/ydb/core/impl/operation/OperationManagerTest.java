package tech.ydb.core.impl.operation;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import tech.ydb.OperationProtos;
import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.operation.v1.OperationServiceGrpc;
import tech.ydb.table.YdbTable;
import tech.ydb.table.v1.TableServiceGrpc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Kurdyukov
 */
public class OperationManagerTest {

    private static final String OPERATION_ID = "123";

    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private OperationManager operationManager;
    private final ScheduledExecutorServiceTest scheduledExecutorServiceTest = new ScheduledExecutorServiceTest();

    @Before
    public void before() {
        when(transport.getScheduler()).thenReturn(scheduledExecutorServiceTest);

        operationManager = new OperationManager(transport);
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

        OperationManager.Operation<YdbTable.ExplainQueryResult> resultCompletableFuture = operationUnwrap();

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

        OperationManager.Operation<YdbTable.ExplainQueryResult> resultCompletableFuture = operationUnwrap();

        mockCancelOperationMethodTransport(Result.fail(Status.of(StatusCode.BAD_SESSION)));
        resultCompletableFuture.cancel();

        mockGetOperationMethodTransport(StatusCodesProtos.StatusIds.StatusCode.SUCCESS);
        scheduledExecutorServiceTest.execCommand();

        checkSuccessOperation(resultCompletableFuture.getResultFuture());
    }

    private OperationManager.Operation<YdbTable.ExplainQueryResult> operationUnwrap() {
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
                .thenCompose(operationManager
                        .resultUnwrapper(
                                YdbTable.ExplainDataQueryResponse::getOperation,
                                YdbTable.ExplainQueryResult.class
                        )
                );
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
                .setResult(Any.pack(
                        YdbTable.ExplainQueryResult.getDefaultInstance()))
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
}
