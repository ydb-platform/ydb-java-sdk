package tech.ydb.core.operation;


import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.YdbIssueMessage.IssueMessage;
import tech.ydb.proto.common.CommonProtos;
import tech.ydb.proto.table.YdbTable;

/**
 * @author Kirill Kurdyukov
 */
public class OperationBinderTest {
    private static final IssueMessage TEST_ISSUE_MESSAGE = IssueMessage.newBuilder()
            .setIssueCode(12345)
            .setSeverity(Issue.Severity.INFO.getCode())
            .setMessage("some-issue")
            .build();

    private static final Issue TEST_ISSUE = Issue.of(12345, "some-issue", Issue.Severity.INFO);

    private static final Status NOT_FOUND = Status.of(StatusCode.NOT_FOUND)
            .withIssues(Issue.of("not-found", Issue.Severity.ERROR));

    private final GrpcTransport mocked = Mockito.mock(GrpcTransport.class);

    @Test
    public void syncStatusBinderTest() {
        YdbTable.AlterTableResponse response = YdbTable.AlterTableResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                                .setId("ready-id")
                                .setReady(true)
                                .setCostInfo(CommonProtos.CostInfo.newBuilder().setConsumedUnits(15d).build())
                                .build())
                .build();

        Status status = OperationBinder
                .bindSync(YdbTable.AlterTableResponse::getOperation)
                .apply(Result.success(response));

        Assert.assertEquals(StatusCode.SUCCESS, status.getCode());
        Assert.assertTrue(status.hasConsumedRu());
        Assert.assertEquals(Double.valueOf(15d), status.getConsumedRu());
        Assert.assertEquals(0, status.getIssues().length);
    }

    @Test
    public void syncStatusBinderFailTest() {
        YdbTable.AlterTableResponse response = YdbTable.AlterTableResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setStatus(StatusCodesProtos.StatusIds.StatusCode.NOT_FOUND)
                                .setId("errored-id")
                                .setReady(true)
                                .addIssues(TEST_ISSUE_MESSAGE)
                                .build())
                .build();

        Status status = OperationBinder
                .bindSync(YdbTable.AlterTableResponse::getOperation)
                .apply(Result.success(response));

        Assert.assertEquals(StatusCode.NOT_FOUND, status.getCode());
        Assert.assertFalse(status.hasConsumedRu());
        Assert.assertArrayEquals(new Issue[] { TEST_ISSUE }, status.getIssues());
    }

    @Test
    public void syncStatusBinderErrorTest() {
        Status failed = OperationBinder
                .bindSync(YdbTable.AlterTableResponse::getOperation)
                .apply(Result.fail(NOT_FOUND));
        Assert.assertEquals(NOT_FOUND, failed);
    }

    @Test
    public void syncStatusBinderNotReadyTest() {
        YdbTable.AlterTableResponse response = YdbTable.AlterTableResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setId("not-ready-id")
                                .setReady(false)
                                .build()
                ).build();

        Status status = OperationBinder
                .bindSync(YdbTable.AlterTableResponse::getOperation)
                .apply(Result.success(response));

        Assert.assertEquals(OperationBinder.UNEXPECTED_ASYNC, status);
    }

    @Test
    public void syncResultBinderTest() {
        Any data = Any.pack(YdbTable.ExplainQueryResult.newBuilder().setQueryPlan("plan").build());
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(OperationProtos.Operation.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .setId("ready-id")
                        .setReady(true)
                        .setCostInfo(CommonProtos.CostInfo.newBuilder().setConsumedUnits(15d).build())
                        .setResult(data)
                        .build()
                ).build();

        Result<YdbTable.ExplainQueryResult> result = OperationBinder
                .bindSync(YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.success(response));

        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getStatus().hasConsumedRu());
        Assert.assertEquals(Double.valueOf(15d), result.getStatus().getConsumedRu());
        Assert.assertEquals(0, result.getStatus().getIssues().length);
    }

    @Test
    public void syncResultBinderFailTest() {
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setStatus(StatusCodesProtos.StatusIds.StatusCode.NOT_FOUND)
                                .setId("error-id")
                                .setReady(true)
                                .addIssues(TEST_ISSUE_MESSAGE)
                                .build())
                .build();

        Result<YdbTable.ExplainQueryResult> result = OperationBinder
                .bindSync(YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.success(response));

        Assert.assertEquals(StatusCode.NOT_FOUND, result.getStatus().getCode());
        Assert.assertFalse(result.getStatus().hasConsumedRu());
        Assert.assertArrayEquals(new Issue[] { TEST_ISSUE }, result.getStatus().getIssues());
    }

    @Test
    public void syncResultBinderNotReadyTest() {
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setId("not-ready-id")
                                .setReady(false)
                                .build()
                ).build();

        Result<YdbTable.ExplainQueryResult> result = OperationBinder
                .bindSync(YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.success(response));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(OperationBinder.UNEXPECTED_ASYNC, result.getStatus());
    }

    @Test
    public void syncResultBinderErrorTest() {
        Result<YdbTable.ExplainQueryResult> result = OperationBinder
                .bindSync(YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.fail(NOT_FOUND));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(NOT_FOUND, result.getStatus());
    }

    @Test
    public void syncResultBinderMixClassesTest() {
        Any data = Any.pack(YdbTable.ExplainQueryResult.newBuilder().setQueryPlan("plan").build());
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(OperationProtos.Operation.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .setId("ready-id")
                        .setReady(true)
                        .setCostInfo(CommonProtos.CostInfo.newBuilder().setConsumedUnits(15d).build())
                        .setResult(data)
                        .build()
                ).build();

        final Result<YdbTable.ExecuteQueryResult> result = OperationBinder
                .bindSync(YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExecuteQueryResult.class)
                .apply(Result.success(response));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, result.getStatus().getCode());
        UnexpectedResultException ex = Assert.assertThrows(UnexpectedResultException.class, result::getValue);
        Assert.assertEquals(
                "Can't unpack message tech.ydb.proto.table.YdbTable$ExecuteQueryResult, code: CLIENT_INTERNAL_ERROR",
                ex.getMessage()
        );
    }

    @Test
    public void asyncStatusBinderTest() {
        YdbTable.AlterTableResponse response = YdbTable.AlterTableResponse.newBuilder()
                .setOperation(OperationProtos.Operation.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .setId("ready-id")
                        .setReady(true)
                        .setCostInfo(CommonProtos.CostInfo.newBuilder().setConsumedUnits(15d).build())
                        .build()
                ).build();

        Operation<Status> operation = OperationBinder
                .bindAsync(mocked, YdbTable.AlterTableResponse::getOperation)
                .apply(Result.success(response));

        Assert.assertTrue(operation.isReady());
        Assert.assertEquals("ready-id", operation.getId());

        Status status = operation.getValue();
        Assert.assertNotNull(status);
        Assert.assertEquals(StatusCode.SUCCESS, status.getCode());
        Assert.assertTrue(status.hasConsumedRu());
        Assert.assertEquals(Double.valueOf(15d), status.getConsumedRu());
        Assert.assertEquals(0, status.getIssues().length);
    }

    @Test
    public void asyncStatusBinderFailTest() {
        YdbTable.AlterTableResponse response = YdbTable.AlterTableResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setStatus(StatusCodesProtos.StatusIds.StatusCode.NOT_FOUND)
                                .setId("error-id")
                                .setReady(true)
                                .addIssues(TEST_ISSUE_MESSAGE)
                                .build())
                .build();
        Status error = Status.of(StatusCode.NOT_FOUND, TEST_ISSUE);

        Operation<Status> operation = OperationBinder
                .bindAsync(mocked, YdbTable.AlterTableResponse::getOperation)
                .apply(Result.success(response));

        Assert.assertTrue(operation.isReady());
        Assert.assertEquals("error-id", operation.getId());

        Status status = operation.getValue();
        Assert.assertNotNull(status);
        Assert.assertEquals(error, status);
    }

    @Test
    public void asyncStatusBinderErrorTest() {
        Status error = Status.of(StatusCode.NOT_FOUND, TEST_ISSUE);

        Operation<Status> operation = OperationBinder
                .bindAsync(mocked, YdbTable.AlterTableResponse::getOperation)
                .apply(Result.fail(error));

        Assert.assertTrue(operation.isReady());
        Assert.assertNull(operation.getId());

        Assert.assertEquals(error, operation.getValue());

        Assert.assertEquals(error, operation.cancel().join());
        Assert.assertEquals(error, operation.forget().join());
        Assert.assertEquals(error, operation.fetch().join().getStatus());
    }

    @Test
    public void asyncStatusBinderNotReadyTest() {
        YdbTable.AlterTableResponse response = YdbTable.AlterTableResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setId("not-ready-id")
                                .setReady(false)
                                .build()
                ).build();

        Operation<Status> operation = OperationBinder
                .bindAsync(mocked, YdbTable.AlterTableResponse::getOperation)
                .apply(Result.success(response));

        Assert.assertFalse(operation.isReady());
        Assert.assertEquals("not-ready-id", operation.getId());
    }

    @Test
    public void asyncResultBinderTest() {
        Any data = Any.pack(YdbTable.ExplainQueryResult.newBuilder().setQueryPlan("plan").build());
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(OperationProtos.Operation.newBuilder()
                        .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                        .setId("ready-id")
                        .setReady(true)
                        .setCostInfo(CommonProtos.CostInfo.newBuilder().setConsumedUnits(15d).build())
                        .setResult(data)
                        .build()
                ).build();

        Operation<Result<YdbTable.ExplainQueryResult>> operation = OperationBinder
                .bindAsync(mocked, YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.success(response));

        Assert.assertTrue(operation.isReady());
        Assert.assertEquals("ready-id", operation.getId());

        Result<YdbTable.ExplainQueryResult> result = operation.getValue();
        Assert.assertNotNull(result);
        Assert.assertEquals(StatusCode.SUCCESS, result.getStatus().getCode());
        Assert.assertTrue(result.getStatus().hasConsumedRu());
        Assert.assertEquals(Double.valueOf(15d), result.getStatus().getConsumedRu());
        Assert.assertEquals(0, result.getStatus().getIssues().length);
    }

    @Test
    public void asyncResultBinderFailTest() {
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setStatus(StatusCodesProtos.StatusIds.StatusCode.NOT_FOUND)
                                .setId("error-id")
                                .setReady(true)
                                .addIssues(TEST_ISSUE_MESSAGE)
                                .build())
                .build();

        Operation<Result<YdbTable.ExplainQueryResult>> operation = OperationBinder
                .bindAsync(mocked, YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.success(response));
        Status error = Status.of(StatusCode.NOT_FOUND, TEST_ISSUE);

        Assert.assertTrue(operation.isReady());
        Assert.assertEquals("error-id", operation.getId());

        Result<YdbTable.ExplainQueryResult> result = operation.getValue();
        Assert.assertNotNull(result);
        Assert.assertEquals(error, result.getStatus());
    }

    @Test
    public void asyncResultBinderErrorTest() {
        Status error = Status.of(StatusCode.BAD_SESSION, TEST_ISSUE, TEST_ISSUE);
        Operation<Result<YdbTable.ExplainQueryResult>> operation = OperationBinder
                .bindAsync(mocked, YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.fail(error));

        Assert.assertTrue(operation.isReady());
        Assert.assertNull(operation.getId());

        Result<YdbTable.ExplainQueryResult> result = operation.getValue();
        Assert.assertNotNull(result);
        Assert.assertEquals(error, result.getStatus());

        Assert.assertEquals(error, operation.cancel().join());
        Assert.assertEquals(error, operation.forget().join());
        Assert.assertEquals(error, operation.fetch().join().getStatus());
    }

    @Test
    public void asyncResultBinderNotReadyTest() {
        YdbTable.ExplainDataQueryResponse response = YdbTable.ExplainDataQueryResponse.newBuilder()
                .setOperation(
                        OperationProtos.Operation.newBuilder()
                                .setId("not-ready-id")
                                .setReady(false)
                                .build()
                ).build();

        Operation<Result<YdbTable.ExplainQueryResult>> operation = OperationBinder
                .bindAsync(mocked, YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class)
                .apply(Result.success(response));

        Assert.assertFalse(operation.isReady());
        Assert.assertEquals("not-ready-id", operation.getId());
    }
}
