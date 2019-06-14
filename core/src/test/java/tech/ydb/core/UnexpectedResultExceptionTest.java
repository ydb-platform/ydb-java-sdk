package tech.ydb.core;

import com.yandex.yql.proto.IssueSeverity.TSeverityIds.ESeverityId;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class UnexpectedResultExceptionTest {

    @Test
    public void testToString() {
        UnexpectedResultException e1 = new UnexpectedResultException("", StatusCode.OVERLOADED);
        Assert.assertEquals("code: OVERLOADED", e1.getMessage());

        UnexpectedResultException e2 = new UnexpectedResultException("some message", StatusCode.OVERLOADED);
        Assert.assertEquals("some message, code: OVERLOADED", e2.getMessage());

        UnexpectedResultException e3 = new UnexpectedResultException(
            "some message",
            StatusCode.OVERLOADED,
            Issue.of("issue message", ESeverityId.S_ERROR));
        Assert.assertEquals("some message, code: OVERLOADED, issues: [issue message (S_ERROR)]", e3.getMessage());
    }
}
