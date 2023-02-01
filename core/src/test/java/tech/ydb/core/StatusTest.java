package tech.ydb.core;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class StatusTest {

    @Test
    public void simpleStatus() {
        Issue i1 = Issue.of("issue1", Issue.Severity.INFO);
        Issue i2 = Issue.of("issue2", Issue.Severity.INFO);
        Status s1 = Status.of(StatusCode.ABORTED).withConsumedRu(5d).withIssues(i1, i2);
        Status s2 = Status.of(StatusCode.ABORTED, 5d, i1, i2);

        Assert.assertEquals(s1, s2);
        Assert.assertNotEquals(s1, null);
        Assert.assertEquals(s1.hashCode(), s2.hashCode());

        Assert.assertEquals("Status{code = ABORTED, consumed RU = 5.0, issues = [issue1 (S_INFO), issue2 (S_INFO)]}",
                s1.toString());
    }

    @Test
    public void cloneEqualStatuses() {
        Issue i1 = Issue.of("issue1", Issue.Severity.INFO);
        Issue i2 = Issue.of("issue2", Issue.Severity.INFO);

        Status s1 = Status.of(StatusCode.BAD_REQUEST);
        Status s2 = s1.withConsumedRu(5d);
        Status s3 = s2.withConsumedRu(5d);

        Assert.assertNotSame(s1, s2);
        Assert.assertSame(s2, s3);

        Status s4 = s1.withIssues(i1, i2);
        Status s5 = s4.withIssues(i1, i2);
        Status s6 = s4.withIssues(i2, i1);

        Assert.assertNotSame(s1, s4);
        Assert.assertSame(s4, s5);
        Assert.assertNotSame(s4, s6);
    }

    @Test
    public void checkSuccess() {
        Issue i1 = Issue.of("issue1", Issue.Severity.FATAL);
        Issue i2 = Issue.of("issue2", Issue.Severity.INFO);

        Status good = Status.SUCCESS.withConsumedRu(5d);
        good.expectSuccess(); // no throws
        good.expectSuccess("check message"); // no throws

        Status wrong = Status.of(StatusCode.BAD_REQUEST).withConsumedRu(1d).withIssues(i1, i2);

        UnexpectedResultException ex1 = Assert.assertThrows(UnexpectedResultException.class,
                wrong::expectSuccess);
        Assert.assertEquals("Expected success status, but got BAD_REQUEST, code: BAD_REQUEST, consumed 1.0 RU"
                + ", issues: [issue1 (S_FATAL), issue2 (S_INFO)]",
                ex1.getMessage());
        Assert.assertEquals(wrong, ex1.getStatus());

        UnexpectedResultException ex2 = Assert.assertThrows(UnexpectedResultException.class,
                () -> wrong.expectSuccess("check success"));
        Assert.assertEquals(
                "check success, code: BAD_REQUEST, consumed 1.0 RU, issues: [issue1 (S_FATAL), issue2 (S_INFO)]",
                ex2.getMessage());
        Assert.assertEquals(wrong, ex2.getStatus());
    }
}
