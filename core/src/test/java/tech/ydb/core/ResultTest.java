package tech.ydb.core;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class ResultTest {

    @Test
    public void success() {
        Result<Integer> r = Result.success(1);
        assertSuccess(r, 1);

        Result<Integer> rInc = r.map(v -> v + 1);
        assertSuccess(rInc, 2);
    }

    @Test
    public void successWithIssue() {
        Issue issue1 = Issue.of("issue1", Issue.Severity.ERROR);
        Issue issue2 = Issue.of("issue2", Issue.Severity.FATAL);

        Result<Integer> r = Result.success(1, Status.of(StatusCode.SUCCESS, null, issue1, issue2));
        Assert.assertTrue(r.isSuccess());
        Assert.assertEquals(StatusCode.SUCCESS, r.getStatus().getCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, r.getStatus().getIssues());
        Assert.assertNotSame(Status.SUCCESS, r.getStatus());
        Assert.assertEquals((Integer) 1, r.getValue());
    }

    @Test
    public void fail() {
        Issue issue1 = Issue.of("issue1", Issue.Severity.ERROR);
        Issue issue2 = Issue.of("issue2", Issue.Severity.FATAL);

        Result<Void> r = Result.fail(Status.of(StatusCode.BAD_SESSION, null, issue1, issue2));

        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(StatusCode.BAD_SESSION, r.getStatus().getCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, r.getStatus().getIssues());
        Assert.assertEquals(Status.of(StatusCode.BAD_SESSION, null, issue1, issue2), r.getStatus());

        try {
            r.getValue();
            Assert.fail("expected exception not thrown");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnexpectedResultException);
            Assert.assertEquals(
                "Cannot get value, code: BAD_SESSION, issues: [issue1 (S_ERROR), issue2 (S_FATAL)]",
                e.getMessage());

            UnexpectedResultException ex = (UnexpectedResultException) e;
            Assert.assertEquals(StatusCode.BAD_SESSION, ex.getStatus().getCode());
            Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, ex.getStatus().getIssues());
        }

        Result<String> r2 = r.map(v -> "");
        Assert.assertSame(r2, r);
    }

    @Test
    public void error() {
        Result<Void> r = Result.error("some message", new RuntimeException("some exception"));

        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, r.getStatus().getCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getStatus().getIssues());
        Assert.assertEquals(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null), r.getStatus());

        try {
            r.getValue();
            Assert.fail("expected exception not thrown");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnexpectedResultException);
            Assert.assertEquals("some message, code: CLIENT_INTERNAL_ERROR", e.getMessage());

            UnexpectedResultException ex = (UnexpectedResultException) e;
            Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, ex.getStatus().getCode());
            Assert.assertSame(Issue.EMPTY_ARRAY, ex.getStatus().getIssues());

            Throwable cause = ex.getCause();
            Assert.assertNotNull(cause);
            Assert.assertEquals("some exception", cause.getMessage());
        }

        Result<String> r2 = r.map(v -> "");
        Assert.assertSame(r2, r);
    }

    private static <T> void assertSuccess(Result<T> r, T expectedValue) {
        Assert.assertTrue(r.isSuccess());
        Assert.assertEquals(StatusCode.SUCCESS, r.getStatus().getCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getStatus().getIssues());
        Assert.assertSame(Status.SUCCESS, r.getStatus());
        Assert.assertEquals(expectedValue, r.getValue());
    }
}
