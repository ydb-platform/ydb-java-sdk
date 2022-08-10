package tech.ydb.core.grpc;

import java.util.Optional;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import org.junit.Test;

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
        Result<?> result = GrpcStatuses.toResult(io.grpc.Status.INTERNAL.withDescription("error description"));

        assertFalse(result.isSuccess());
        assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, result.getStatus().getCode());
        assertArrayEquals(new Issue[] {
            Issue.of("gRPC error: (INTERNAL) error description", Issue.Severity.ERROR)
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
            .withCause(new MyException("exception message")));

        assertFalse(result.isSuccess());
        assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, result.getStatus().getCode());
        assertArrayEquals(Issue.EMPTY_ARRAY, result.getStatus().getIssues());

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
        Status status = GrpcStatuses.toStatus(io.grpc.Status.OK);
        assertEquals(Status.SUCCESS, status);
    }

    @Test
    public void statusFail() {
        Status status = GrpcStatuses.toStatus(io.grpc.Status.DEADLINE_EXCEEDED);
        Issue issue = Issue.of("gRPC error: (DEADLINE_EXCEEDED)", Issue.Severity.ERROR);
        assertEquals(Status.of(StatusCode.CLIENT_DEADLINE_EXCEEDED, null, issue), status);
    }
}
