package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OperationImplTest {
    private final GrpcTransport mocked = Mockito.mock(GrpcTransport.class);
    private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);

    @Before
    public void setup() {
        Mockito.when(mocked.getScheduler()).thenReturn(scheduler);
    }

    @Test
    public void asyncNotReadyOperationTest() {
        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setId("test-id")
                .setReady(false)
                .build();

        Mockito.when(mocked.unaryCall(
                Mockito.eq(OperationServiceGrpc.getCancelOperationMethod()), Mockito.any(), Mockito.any()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(
                OperationProtos.CancelOperationResponse.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .build()
        )));
        Mockito.when(mocked.unaryCall(
                Mockito.eq(OperationServiceGrpc.getForgetOperationMethod()), Mockito.any(), Mockito.any()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(
                OperationProtos.ForgetOperationResponse.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST)
                        .build()
        )));
        Mockito.when(mocked.unaryCall(
                Mockito.eq(OperationServiceGrpc.getGetOperationMethod()), Mockito.any(), Mockito.any()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(
                OperationProtos.GetOperationResponse.newBuilder().setOperation(operation).build()
        )));

        AsyncOperation<Any> o = new OperationImpl<>(mocked, operation, OperationProtos.Operation::getResult);

        Assert.assertEquals(scheduler, o.getScheduler());
        Assert.assertEquals("Operation{id=test-id, ready=false}", o.toString());
        Assert.assertFalse(o.isReady());
        Assert.assertNull(o.getValue());

        Assert.assertEquals(StatusCode.BAD_REQUEST, o.forget().join().getCode());
        Assert.assertEquals(StatusCode.SUCCESS, o.cancel().join().getCode());
    }
}
