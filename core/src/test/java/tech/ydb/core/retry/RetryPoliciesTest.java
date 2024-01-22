package tech.ydb.core.retry;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.RetryPolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryPoliciesTest {

    @Test
    public void foreverRetryTest() {
        RetryPolicy policy = new RetryForever(1234);

        Assert.assertEquals(1234, policy.nextRetryMs(0, 0));
        Assert.assertEquals(1234, policy.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(1234, policy.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(1234, policy.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void zeroRetriesTest() {
        RetryPolicy policy = new RetryNTimes(0, 100, 3);

        Assert.assertEquals(-1, policy.nextRetryMs(0, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void zeroTimeoutTest() {
        RetryPolicy policy = new RetryUntilElapsed(0, 100, 3);

        Assert.assertEquals(-1, policy.nextRetryMs(0, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void maxRetriesTest() {
        RetryPolicy policy = new RetryNTimes(5, 100, 3);

        assertDuration(100,  200, policy.nextRetryMs(0, 0));
        assertDuration(200,  400, policy.nextRetryMs(1, 150));
        assertDuration(400,  800, policy.nextRetryMs(2, 400));
        assertDuration(800, 1600, policy.nextRetryMs(3, 1600));
        assertDuration(800, 1600, policy.nextRetryMs(4, 2800));

        Assert.assertEquals(-1, policy.nextRetryMs(5, 4000));
    }

    @Test
    public void maxTimeoutTest() {
        RetryPolicy policy = new RetryUntilElapsed(2500, 50, 3);

        assertDuration(50,  100, policy.nextRetryMs(0, 0));
        assertDuration(100, 200, policy.nextRetryMs(1, 75));
        assertDuration(200, 400, policy.nextRetryMs(2, 225));
        assertDuration(400, 800, policy.nextRetryMs(3, 525));
        assertDuration(400, 800, policy.nextRetryMs(4, 1125));
        assertDuration(400, 800, policy.nextRetryMs(5, 1725));

        Assert.assertEquals(175, policy.nextRetryMs(6, 2325));
        Assert.assertEquals(-1, policy.nextRetryMs(7, 2500));
    }

    private void assertDuration(long from, long to, long ms) {
        Assert.assertTrue(from <= ms);
        Assert.assertTrue(to >= ms);
    }
}
