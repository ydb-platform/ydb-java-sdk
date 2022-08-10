package tech.ydb.core;

import java.util.Optional;

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

        Result<Integer> r = Result.success(1, null, issue1, issue2);
        Assert.assertTrue(r.isSuccess());
        Assert.assertEquals(StatusCode.SUCCESS, r.getCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, r.getIssues());
        Assert.assertNotSame(Status.SUCCESS, r.toStatus());
        Assert.assertEquals((Integer) 1, r.expect("cannot get result value"));

        Optional<Integer> ok = r.ok();
        Assert.assertTrue(ok.isPresent());
        Assert.assertEquals((Integer) 1, ok.get());

        Optional<Throwable> error = r.error();
        Assert.assertFalse(error.isPresent());
    }

    @Test
    public void fail() {
        Issue issue1 = Issue.of("issue1", Issue.Severity.ERROR);
        Issue issue2 = Issue.of("issue2", Issue.Severity.FATAL);

        Result<Void> r = Result.fail(StatusCode.BAD_SESSION, issue1, issue2);

        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(StatusCode.BAD_SESSION, r.getCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, r.getIssues());
        Assert.assertEquals(Status.of(StatusCode.BAD_SESSION, issue1, issue2), r.toStatus());

        try {
            r.expect("cannot get result value");
            Assert.fail("expected exception not thrown");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnexpectedResultException);
            Assert.assertEquals(
                "cannot get result value, code: BAD_SESSION, issues: [issue1 (S_ERROR), issue2 (S_FATAL)]",
                e.getMessage());

            UnexpectedResultException ex = (UnexpectedResultException) e;
            Assert.assertEquals(StatusCode.BAD_SESSION, ex.getStatusCode());
            Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, ex.getIssues());
        }

        Optional<Void> ok = r.ok();
        Assert.assertFalse(ok.isPresent());

        Optional<Throwable> error = r.error();
        Assert.assertTrue(error.isPresent());

        Throwable e = error.get();
        Assert.assertTrue(e instanceof UnexpectedResultException);
        UnexpectedResultException ex = (UnexpectedResultException) e;
        Assert.assertEquals(StatusCode.BAD_SESSION, ex.getStatusCode());
        Assert.assertArrayEquals(new Issue[]{ issue1, issue2 }, ex.getIssues());

        Result<String> r2 = r.map(v -> "");
        Assert.assertSame(r2, r);
    }

    @Test
    public void error() {
        Result<Void> r = Result.error("some message", new RuntimeException("some exception"));

        Assert.assertFalse(r.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, r.getCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getIssues());
        Assert.assertEquals(Status.of(StatusCode.CLIENT_INTERNAL_ERROR), r.toStatus());

        try {
            r.expect("cannot get result value");
            Assert.fail("expected exception not thrown");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnexpectedResultException);
            Assert.assertEquals("cannot get result value: some message", e.getMessage());

            UnexpectedResultException ex = (UnexpectedResultException) e;
            Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, ex.getStatusCode());
            Assert.assertSame(Issue.EMPTY_ARRAY, ex.getIssues());

            Throwable cause = ex.getCause();
            Assert.assertNotNull(cause);
            Assert.assertEquals("some exception", cause.getMessage());
        }

        Optional<Void> ok = r.ok();
        Assert.assertFalse(ok.isPresent());

        Optional<Throwable> error = r.error();
        Assert.assertTrue(error.isPresent());

        UnexpectedResultException ex = (UnexpectedResultException) error.get();
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, ex.getStatusCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, ex.getIssues());

        Throwable cause = ex.getCause();
        Assert.assertNotNull(cause);
        Assert.assertEquals("some exception", cause.getMessage());

        Result<String> r2 = r.map(v -> "");
        Assert.assertSame(r2, r);
    }

    private static <T> void assertSuccess(Result<T> r, T expectedValue) {
        Assert.assertTrue(r.isSuccess());
        Assert.assertEquals(StatusCode.SUCCESS, r.getCode());
        Assert.assertSame(Issue.EMPTY_ARRAY, r.getIssues());
        Assert.assertSame(Status.SUCCESS, r.toStatus());
        Assert.assertEquals(expectedValue, r.expect("cannot get result value"));

        Optional<T> ok = r.ok();
        Assert.assertTrue(ok.isPresent());
        Assert.assertEquals(expectedValue, ok.get());

        Optional<Throwable> error = r.error();
        Assert.assertFalse(error.isPresent());
    }
}
