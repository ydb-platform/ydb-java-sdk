package tech.ydb.core;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class ResultTest {

    @Test
    public void success() {
        Result<Integer> r1 = Result.success(1);
        Result<Integer> r2 = Result.success(1);
        Result<Integer> r3 = Result.success(2);
        Result<Long> r4 = Result.success(1l);
        Result<Integer> r5 = Result.fail(new UnexpectedResultException("error", Status.of(StatusCode.ABORTED)));
        Result<Integer> r6 = null;

        Result<Integer> r7 = r1.map(v -> v + 1);

        assertSuccess(r1, 1);
        assertSuccess(r2, 1);
        assertSuccess(r3, 2);
        assertSuccess(r7, 2);

        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r1, r2);
        Assert.assertEquals(r3, r7);

        Assert.assertNotEquals(r1, r3);
        Assert.assertNotEquals(r1, r4);
        Assert.assertNotEquals(r1, r5);
        Assert.assertNotEquals(r1, r6);

        Assert.assertEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r3.hashCode());
        Assert.assertEquals("Success{1, status=Status{code = SUCCESS}}", r1.toString());
    }

    @Test
    public void successWithIssue() {
        Issue issue1 = Issue.of("issue1", Issue.Severity.ERROR);
        Issue issue2 = Issue.of("issue2", Issue.Severity.FATAL);

        Result<Integer> r1 = Result.success(1, Status.of(StatusCode.SUCCESS).withIssues(issue1, issue2));
        Result<Integer> r2 = Result.success(1, Status.of(StatusCode.SUCCESS).withIssues(issue1, issue2));
        Result<Integer> r3 = Result.success(1, Status.of(StatusCode.SUCCESS).withIssues(issue2, issue1));

        assertSuccess(r1, 1, issue1, issue2);
        assertSuccess(r2, 1, issue1, issue2);
        assertSuccess(r3, 1, issue2, issue1);

        Assert.assertEquals(r1, r2);
        Assert.assertNotEquals(r1, r3);

        Assert.assertEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r3.hashCode());
    }

    @Test
    public void successWithRU() {
        Result<Integer> r1 = Result.success(1, Status.of(StatusCode.SUCCESS).withConsumedRu(190.75d));
        Result<Integer> r2 = Result.success(1, Status.of(StatusCode.SUCCESS).withConsumedRu(190.75d));
        Result<Integer> r3 = Result.success(1, Status.of(StatusCode.SUCCESS).withConsumedRu(190.57d));

        assertSuccess(r1, 1, 190.75d);
        assertSuccess(r2, 1, 190.75d);
        assertSuccess(r3, 1, 190.57d);

        Assert.assertEquals(r1, r2);
        Assert.assertNotEquals(r1, r3);

        Assert.assertEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r3.hashCode());
    }

    @Test
    public void fail() {
        Issue issue1 = Issue.of("issue1", Issue.Severity.ERROR);
        Issue issue2 = Issue.of("issue2", Issue.Severity.FATAL);

        Result<Void> r1 = Result.fail(Status.of(StatusCode.BAD_SESSION).withIssues(issue1, issue2));
        Result<Void> r2 = Result.fail(Status.of(StatusCode.BAD_SESSION).withIssues(issue1, issue2));
        Result<Void> r3 = Result.fail(Status.of(StatusCode.ABORTED).withIssues(issue1, issue2));
        Result<Void> r4 = Result.fail(Status.of(StatusCode.BAD_SESSION).withIssues(issue2, issue1));

        Result<String> r5 = r1.map(v -> "");

        assertFail(r1, StatusCode.BAD_SESSION, issue1, issue2);
        assertFail(r2, StatusCode.BAD_SESSION, issue1, issue2);
        assertFail(r3, StatusCode.ABORTED, issue1, issue2);
        assertFail(r4, StatusCode.BAD_SESSION, issue2, issue1);
        assertFail(r5, StatusCode.BAD_SESSION, issue1, issue2);

        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r1, r2);
        Assert.assertNotEquals(r1, r3);
        Assert.assertNotEquals(r1, r4);
        Assert.assertNotEquals(r1, null);

        Assert.assertEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r3.hashCode());
        Assert.assertEquals(
                "Fail{Status{code = BAD_SESSION, issues = [issue1 (S_ERROR), issue2 (S_FATAL)]}}",
                r1.toString());

        UnexpectedResultException ex = Assert.assertThrows(UnexpectedResultException.class, r1::getValue);
        Assert.assertEquals(
                "Cannot get value, code: BAD_SESSION, issues: [issue1 (S_ERROR), issue2 (S_FATAL)]",
                ex.getMessage());
        Assert.assertEquals(StatusCode.BAD_SESSION, ex.getStatus().getCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, ex.getStatus().getIssues());
    }

    @Test
    public void error() {
        Result<Void> r1 = Result.error("error message", new RuntimeException("some exception"));
        Result<Void> r2 = Result.error("error message", new RuntimeException("some exception"));
        Result<Void> r3 = r1.map(null);

        assertFail(r1, StatusCode.CLIENT_INTERNAL_ERROR);
        assertFail(r2, StatusCode.CLIENT_INTERNAL_ERROR);
        assertFail(r3, StatusCode.CLIENT_INTERNAL_ERROR);

        Assert.assertEquals(r1, r3);
        Assert.assertNotEquals(r1, r2); // different instance of exceptions
        Assert.assertNotEquals(r1, null);

        Assert.assertEquals(r1.hashCode(), r3.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r2.hashCode());
        Assert.assertEquals(
                "Error{message=error message, cause=java.lang.RuntimeException: some exception}",
                r1.toString());

        UnexpectedResultException ex = Assert.assertThrows(UnexpectedResultException.class, r1::getValue);
        Assert.assertEquals("error message, code: CLIENT_INTERNAL_ERROR", ex.getMessage());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, ex.getStatus().getCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, ex.getStatus().getIssues());

        Throwable cause = ex.getCause();
        Assert.assertNotNull(cause);
        Assert.assertEquals("some exception", cause.getMessage());
    }

    @Test
    public void unexpected() {
        Issue i1 = Issue.of("issue1", Issue.Severity.ERROR);
        UnexpectedResultException ex1 = new UnexpectedResultException("unexpected 1", Status
                .of(StatusCode.CLIENT_CANCELLED).withIssues(i1)
        );
        UnexpectedResultException ex2 = new UnexpectedResultException("unexpected 2", Status
                .of(StatusCode.INTERNAL_ERROR).withConsumedRu(5d), new RuntimeException("inner cause")
        );

        Result<Void> r1 = Result.error(null, ex1);
        Result<Void> r2 = Result.error("some message", ex2);

        Result<Void> r3 = Result.fail(ex1);
        Result<Void> r4 = r2.map(null);

        Assert.assertEquals(r1, r3); // equals exception
        Assert.assertEquals(r2, r4); // r4 is copy of r2
        Assert.assertNotEquals(r1, r2);
        Assert.assertNotEquals(r1, null);

        Assert.assertEquals(r1.hashCode(), r3.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r2.hashCode());
        Assert.assertEquals(
                "Unexpected{message=unexpected 1, code: CLIENT_CANCELLED, issues: [issue1 (S_ERROR)]}",
                r1.toString());
        Assert.assertEquals("Unexpected{message=some message: unexpected 2, "
                + "code: INTERNAL_ERROR, cause=java.lang.RuntimeException: inner cause}",
                r2.toString());

        UnexpectedResultException res1 = Assert.assertThrows(UnexpectedResultException.class, r1::getValue);
        Assert.assertEquals(r1.getStatus(), ex1.getStatus());
        Assert.assertEquals(res1, ex1);
        Assert.assertEquals("unexpected 1, code: CLIENT_CANCELLED, issues: [issue1 (S_ERROR)]", res1.getMessage());
        Assert.assertEquals(res1.getStatus(), Status.of(StatusCode.CLIENT_CANCELLED).withIssues(i1));
        Assert.assertNull(res1.getCause());

        UnexpectedResultException res2 = Assert.assertThrows(UnexpectedResultException.class, r2::getValue);
        Assert.assertEquals(r2.getStatus(), ex2.getStatus());
        Assert.assertNotEquals(res2, ex2);
        Assert.assertEquals("some message: unexpected 2, code: INTERNAL_ERROR", res2.getMessage());
        Assert.assertEquals(res2.getStatus(), Status.of(StatusCode.INTERNAL_ERROR).withConsumedRu(5d));
        Assert.assertNotNull(res2.getCause());
        Assert.assertEquals("inner cause", res2.getCause().getMessage());
    }

    private static <T> void assertSuccess(Result<T> r, T expectedValue) {
        Assert.assertTrue(r.isSuccess());
        Assert.assertSame(Status.SUCCESS, r.getStatus());
        Assert.assertFalse(r.getStatus().hasConsumedRu());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getStatus().getIssues());
        Assert.assertEquals(expectedValue, r.getValue());
    }

    private static <T> void assertSuccess(Result<T> r, T expectedValue, Issue... issues) {
        Assert.assertTrue(r.isSuccess());
        Assert.assertFalse(r.getStatus().hasConsumedRu());
        Assert.assertArrayEquals(issues, r.getStatus().getIssues());
        Assert.assertEquals(expectedValue, r.getValue());
    }

    private static <T> void assertSuccess(Result<T> r, T expectedValue, Double consumedRU) {
        Assert.assertTrue(r.isSuccess());
        Assert.assertTrue(r.getStatus().hasConsumedRu());
        Assert.assertEquals(r.getStatus().getConsumedRu(), consumedRU);

        Assert.assertSame(Issue.EMPTY_ARRAY, r.getStatus().getIssues());
        Assert.assertEquals(expectedValue, r.getValue());
    }

    private static <T> void assertFail(Result<?> r, StatusCode code) {
        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(code, r.getStatus().getCode());
        Assert.assertFalse(r.getStatus().hasConsumedRu());
        Assert.assertEquals(Status.of(code), r.getStatus());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getStatus().getIssues());
    }

    private static <T> void assertFail(Result<?> r, StatusCode code, Issue... issues) {
        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(code, r.getStatus().getCode());
        Assert.assertFalse(r.getStatus().hasConsumedRu());
        Assert.assertEquals(Status.of(code, null, issues), r.getStatus());
        Assert.assertArrayEquals(issues, r.getStatus().getIssues());
    }

}
