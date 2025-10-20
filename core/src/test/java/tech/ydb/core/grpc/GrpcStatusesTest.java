package tech.ydb.core.grpc;

import org.junit.Test;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Sergey Polovko
 */
public class GrpcStatusesTest {

    static class MyException extends RuntimeException {
        private static final long serialVersionUID = 2224988377466493155L;

        MyException(String message) {
            super(message);
        }
    }

    @Test
    public void error() {
        Result<?> result = GrpcStatuses.toResult(io.grpc.Status.INTERNAL.withDescription("error description"), "test");

        assertFalse(result.isSuccess());
        assertEquals(StatusCode.CLIENT_GRPC_ERROR, result.getStatus().getCode());
        assertArrayEquals(new Issue[] {
            Issue.of("gRPC error: (INTERNAL) on test, error description", Issue.Severity.ERROR)
        }, result.getStatus().getIssues());

        try {
            result.getValue();
            assertFalse("error hasn't value", true);
        } catch (UnexpectedResultException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void errorWithCause() {
        Result<?> result = GrpcStatuses.toResult(io.grpc.Status.INTERNAL
            .withDescription("error description")
            .withCause(new MyException("exception message")), "test");

        assertFalse(result.isSuccess());
        assertEquals(StatusCode.CLIENT_GRPC_ERROR, result.getStatus().getCode());
        assertArrayEquals(new Issue[] {
            Issue.of("gRPC error: (INTERNAL) on test, error description", Issue.Severity.ERROR),
            Issue.of(MyException.class.getName() + ": exception message", Issue.Severity.ERROR)
        }, result.getStatus().getIssues());

        try {
            result.getValue();
            assertFalse("error hasn't value", true);
        } catch (UnexpectedResultException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof MyException);
            assertEquals("exception message", cause.getMessage());
        }
    }

    @Test
    public void statusOk() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.OK, "test");
        assertEquals(Status.SUCCESS, status);
    }

    @Test
    public void statusDeadlineExceeded() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.DEADLINE_EXCEEDED, "test");
        Issue issue = Issue.of("gRPC error: (DEADLINE_EXCEEDED) on test", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_DEADLINE_EXCEEDED).withIssues(issue), status);
    }

    @Test
    public void statusUnavailable() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.UNAVAILABLE, "test");
        Issue issue = Issue.of("gRPC error: (UNAVAILABLE) on test", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.TRANSPORT_UNAVAILABLE).withIssues(issue), status);
    }

    @Test
    public void statusUnauthenticated() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.UNAUTHENTICATED, "test");
        Issue issue = Issue.of("gRPC error: (UNAUTHENTICATED) on test", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_UNAUTHENTICATED).withIssues(issue), status);
    }

    @Test
    public void statusCancelled() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.CANCELLED, "test");
        Issue issue = Issue.of("gRPC error: (CANCELLED) on test", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_CANCELLED).withIssues(issue), status);
    }

    @Test
    public void statusUnimplemented() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.UNIMPLEMENTED, "test");
        Issue issue = Issue.of("gRPC error: (UNIMPLEMENTED) on test", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_CALL_UNIMPLEMENTED).withIssues(issue), status);
    }

    @Test
    public void statusResourceExhausted() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.RESOURCE_EXHAUSTED, "test");
        Issue issue = Issue.of("gRPC error: (RESOURCE_EXHAUSTED) on test", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_RESOURCE_EXHAUSTED).withIssues(issue), status);
    }

    @Test
    public void statusThrowable() {
        Throwable th = new RuntimeException("Hello");
        Status status = GrpcStatuses.toStatus(io.grpc.Status.RESOURCE_EXHAUSTED.withCause(th), "test");
        Issue issue1 = Issue.of("gRPC error: (RESOURCE_EXHAUSTED) on test", Issue.Severity.ERROR);
        Issue issue2 = Issue.of("java.lang.RuntimeException: Hello", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_RESOURCE_EXHAUSTED).withIssues(issue1, issue2).withCause(th), status);
    }
}
