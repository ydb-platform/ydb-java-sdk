package tech.ydb.topic.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Assert;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ErrorsHandler implements BiConsumer<Status, Throwable> {
    private final List<StatusCode> problems = new ArrayList<>();

    @Override
    public void accept(Status st, Throwable th) {
        if (st != null) {
            problems.add(st.getCode());
        }
        if (th != null) {
            problems.add(StatusCode.CLIENT_INTERNAL_ERROR);
        }
    }

    public void assertEmpty() {
        Assert.assertTrue("No reties was expected", problems.isEmpty());
    }

    public void assertCodes(StatusCode... codes) {
        Iterator<StatusCode> it = problems.iterator();
        for (StatusCode code: codes) {
            Assert.assertTrue("Expected " + code + ", but has nothing", it.hasNext());
            Assert.assertEquals(code, it.next());
        }
        Assert.assertFalse("Unexpected error code", it.hasNext());
    }
}
