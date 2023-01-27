package tech.ydb.core;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class UnexpectedResultExceptionTest {

    @Test
    public void testToString() {
        UnexpectedResultException e1 = new UnexpectedResultException("",
                Status.of(StatusCode.OVERLOADED));
        Assert.assertEquals("code: OVERLOADED", e1.getMessage());

        UnexpectedResultException e2 = new UnexpectedResultException("some message",
                Status.of(StatusCode.OVERLOADED));
        Assert.assertEquals("some message, code: OVERLOADED", e2.getMessage());

        UnexpectedResultException e3 = new UnexpectedResultException(
                "some message",
                Status.of(StatusCode.OVERLOADED).withIssues(Issue.of("issue message", Issue.Severity.ERROR))
        );
        Assert.assertEquals("some message, code: OVERLOADED, issues: [issue message (S_ERROR)]", e3.getMessage());
    }
}
