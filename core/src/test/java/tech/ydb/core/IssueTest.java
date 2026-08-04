package tech.ydb.core;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Issue.Position;


/**
 * @author Sergey Polovko
 */
public class IssueTest {

    @Test
    public void testToString() {
        Issue i1 = Issue.of("message", Issue.Severity.ERROR);
        Assert.assertEquals("message (S_ERROR)", i1.toString());

        Issue i2 = Issue.of("message", Issue.Severity.FATAL);
        Assert.assertEquals("message (S_FATAL)", i2.toString());

        Issue i3 = Issue.of(3, "message", Issue.Severity.WARNING);
        Assert.assertEquals("#3 message (S_WARNING)", i3.toString());

        Issue i4 = Issue.of(Position.of(11, 22), 4, "message", Issue.Severity.WARNING);
        Assert.assertEquals("11:22: #4 message (S_WARNING)", i4.toString());

        Issue i5 = Issue.of(Position.of(11, 22, "file.cpp"), 5, "message", Issue.Severity.WARNING);
        Assert.assertEquals("11:22 at file.cpp: #5 message (S_WARNING)", i5.toString());

        Issue i6 = Issue.of(Position.of(10, 20), Position.of(15, 30), 6, "message", Issue.Severity.INFO);
        Assert.assertEquals("10:20 - 15:30: #6 message (S_INFO)", i6.toString());

        Issue x = Issue.of(Position.EMPTY, Position.EMPTY, 7, "root cause", Issue.Severity.FATAL, i3, i4, i5);
        Assert.assertEquals(
            "#7 root cause (S_FATAL)\n" +
            "  #3 message (S_WARNING)\n" +
            "  11:22: #4 message (S_WARNING)\n" +
            "  11:22 at file.cpp: #5 message (S_WARNING)", x.toString());
    }

    @Test
    public void testEquals() {
        Issue nested1 = Issue.of(1, "nested 1", Issue.Severity.WARNING);
        Issue nested2 = Issue.of(2, "nested 2", Issue.Severity.WARNING);

        Issue plain = Issue.of(7, "cause", Issue.Severity.FATAL);
        Issue samePlain = Issue.of(7, "cause", Issue.Severity.FATAL);

        Assert.assertEquals(plain, samePlain);
        Assert.assertEquals(plain.hashCode(), samePlain.hashCode());
        Assert.assertNotEquals(plain, Issue.of(8, "root cause", Issue.Severity.FATAL));
        Assert.assertNotEquals(plain, null);

        Issue withNested = Issue.of(Position.EMPTY, Position.EMPTY, 7, "cause", Issue.Severity.FATAL, nested1);
        Issue withSameNested = Issue.of(Position.EMPTY, Position.EMPTY, 7, "cause", Issue.Severity.FATAL, nested1);
        Issue withOtherNested = Issue.of(Position.EMPTY, Position.EMPTY, 7, "cause", Issue.Severity.FATAL, nested2);

        Assert.assertEquals(withNested, withSameNested);
        Assert.assertEquals(withNested.hashCode(), withSameNested.hashCode());

        // issues differing only in their nested issues are different issues
        Assert.assertNotEquals(withNested, withOtherNested);
        Assert.assertNotEquals(withNested, plain);
    }
}
