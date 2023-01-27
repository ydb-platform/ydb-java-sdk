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

        Assert.assertTrue(r1.equals(r1));
        Assert.assertTrue(r1.equals(r2));
        Assert.assertTrue(r3.equals(r7));

        Assert.assertFalse(r1.equals(r3));
        Assert.assertFalse(r1.equals(r4));
        Assert.assertFalse(r1.equals(r5));
        Assert.assertFalse(r1.equals(r6));

        Assert.assertEquals(r1.hashCode(), r2.hashCode());
        Assert.assertEquals("Success{1, status=Status{code = SUCCESS}}", r1.toString());
        Assert.assertNotEquals(r1.hashCode(), r3.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r3.hashCode());
        Assert.assertNotEquals(r1.hashCode(), r5.hashCode());
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

        Assert.assertTrue(r1.equals(r2));
        Assert.assertFalse(r1.equals(r3));
    }

    @Test
    public void successWithRU() {
        Result<Integer> r1 = Result.success(1, Status.of(StatusCode.SUCCESS).withConsumedRu(190.75d));
        Result<Integer> r2 = Result.success(1, Status.of(StatusCode.SUCCESS).withConsumedRu(190.75d));
        Result<Integer> r3 = Result.success(1, Status.of(StatusCode.SUCCESS).withConsumedRu(190.57d));

        assertSuccess(r1, 1, 190.75d);
        assertSuccess(r2, 1, 190.75d);
        assertSuccess(r3, 1, 190.57d);

        Assert.assertTrue(r1.equals(r2));
        Assert.assertFalse(r1.equals(r3));
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

        UnexpectedResultException ex = Assert.assertThrows(UnexpectedResultException.class, r1::getValue);
        Assert.assertEquals(
            "Cannot get value, code: BAD_SESSION, issues: [issue1 (S_ERROR), issue2 (S_FATAL)]",
            ex.getMessage());
        Assert.assertEquals(StatusCode.BAD_SESSION, ex.getStatus().getCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, ex.getStatus().getIssues());
    }

    @Test
    public void error() {
        Result<Void> r = Result.error("some message", new RuntimeException("some exception"));

        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, r.getStatus().getCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getStatus().getIssues());
        Assert.assertEquals(Status.of(StatusCode.CLIENT_INTERNAL_ERROR), r.getStatus());

        try {
            r.getValue();
            Assert.fail("expected exception not thrown");
        } catch (UnexpectedResultException e) {
            Assert.assertTrue(e instanceof UnexpectedResultException);
            Assert.assertEquals("some message, code: CLIENT_INTERNAL_ERROR", e.getMessage());

            Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, e.getStatus().getCode());
            Assert.assertSame(Issue.EMPTY_ARRAY, e.getStatus().getIssues());

            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            Assert.assertEquals("some exception", cause.getMessage());
        }

        Result<String> r2 = r.map(v -> "");
        Assert.assertSame(r2, r);
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

    private static <T> void assertFail(Result<?> r, StatusCode code, Issue... issues) {
        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(code, r.getStatus().getCode());
        Assert.assertFalse(r.getStatus().hasConsumedRu());
        Assert.assertEquals(Status.of(code, null, issues), r.getStatus());
        Assert.assertArrayEquals(issues, r.getStatus().getIssues());
    }

}
