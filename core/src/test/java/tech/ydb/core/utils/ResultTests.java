package tech.ydb.core.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ResultTests {

    @Test
    public void successTest() {
        Result<String> r = Result.success("Good", Status.of(StatusCode.SUCCESS).withConsumedRu(2d));

        String s1 = "p1";
        boolean b1 = true;
        int i1 = 234;
        long l1 = -100l;

        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error"));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s %s", s1, b1, i1));

        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s", s1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s", b1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s", i1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s", l1));

        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", s1, s1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", b1, s1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", i1, s1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", l1, s1));

        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", s1, b1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", b1, b1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", i1, b1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", l1, b1));

        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", s1, i1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", b1, i1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", i1, i1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", l1, i1));

        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", s1, l1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", b1, l1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", i1, l1));
        Assert.assertEquals("Good", Results.getValueOrThrow(r, "Error %s %s", l1, l1));
    }

    @Test
    public void errorTest() {
        Result<String> r = Result.fail(Status.of(StatusCode.BAD_REQUEST));

        String s1 = "p1";
        boolean b1 = true;
        int i1 = 234;
        long l1 = -100l;

        assertError("Error, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error"));
        assertError("Error p1 true 234, code: BAD_REQUEST",
                () -> Results.getValueOrThrow(r, "Error %s %s %s", s1, b1, i1)
        );

        assertError("Error p1, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s", s1));
        assertError("Error true, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s", b1));
        assertError("Error 234, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s", i1));
        assertError("Error -100, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s", l1));

        assertError("Error p1 p1, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", s1, s1));
        assertError("Error true p1, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", b1, s1));
        assertError("Error 234 p1, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", i1, s1));
        assertError("Error -100 p1, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", l1, s1));

        assertError("Error p1 true, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", s1, b1));
        assertError("Error true true, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", b1, b1));
        assertError("Error 234 true, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", i1, b1));
        assertError("Error -100 true, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", l1, b1));

        assertError("Error p1 234, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", s1, i1));
        assertError("Error true 234, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", b1, i1));
        assertError("Error 234 234, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", i1, i1));
        assertError("Error -100 234, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", l1, i1));

        assertError("Error p1 -100, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", s1, l1));
        assertError("Error true -100, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", b1, l1));
        assertError("Error 234 -100, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", i1, l1));
        assertError("Error -100 -100, code: BAD_REQUEST", () -> Results.getValueOrThrow(r, "Error %s %s", l1, l1));
    }

    private void assertError(String message, ThrowingRunnable runnable) {
        UnexpectedResultException ex = Assert.assertThrows(UnexpectedResultException.class, runnable);
        Assert.assertEquals(message, ex.getMessage());
    }
}
