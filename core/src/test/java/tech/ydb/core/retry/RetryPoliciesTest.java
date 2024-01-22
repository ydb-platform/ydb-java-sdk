package tech.ydb.core.retry;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryPoliciesTest {

    @Test
    public void foreverRetryTest() {
        RetryForever policy = new RetryForever(1234);

        Assert.assertEquals(1234, policy.nextRetryMs(0, 0));
        Assert.assertEquals(1234, policy.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(1234, policy.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(1234, policy.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void foreverUpdateTest() {
        RetryForever policy = new RetryForever(50);
        Assert.assertEquals(50, policy.getIntervalMillis());
        Assert.assertEquals(50, policy.nextRetryMs(0, 0));

        RetryForever updated = policy.withIntervalMs(150);
        Assert.assertEquals(150, updated.getIntervalMillis());
        Assert.assertEquals(150, updated.nextRetryMs(0, 0));
    }

    @Test
    public void zeroRetriesTest() {
        RetryNTimes policy = new RetryNTimes(0, 100, 3);

        Assert.assertEquals(-1, policy.nextRetryMs(0, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void nRetriesTest() {
        RetryNTimes policy = new RetryNTimes(5, 100, 3);

        assertDuration(100,  200, policy.nextRetryMs(0, 0));
        assertDuration(200,  400, policy.nextRetryMs(1, 150));
        assertDuration(400,  800, policy.nextRetryMs(2, 400));
        assertDuration(800, 1600, policy.nextRetryMs(3, 1600));
        assertDuration(800, 1600, policy.nextRetryMs(4, 2800));

        Assert.assertEquals(-1, policy.nextRetryMs(5, 4000));
    }

    @Test
    public void updateNRetriesTest() {
        RetryNTimes policy = new RetryNTimes(5, 100, 3);

        Assert.assertEquals(5, policy.getMaxRetries());
        Assert.assertEquals(100, policy.getBackoffMillis());
        Assert.assertEquals(3, policy.getBackoffCeiling());
        assertDuration(100, 200, policy.nextRetryMs(0, 0));

        RetryNTimes updated = policy.withMaxRetries(4).withBackoffMs(150).withBackoffCeiling(1);

        Assert.assertEquals(4, updated.getMaxRetries());
        Assert.assertEquals(150, updated.getBackoffMillis());
        Assert.assertEquals(1, updated.getBackoffCeiling());
        assertDuration(150, 300, updated.nextRetryMs(0, 0));
    }

    @Test
    public void zeroElapsedTest() {
        RetryUntilElapsed policy = new RetryUntilElapsed(0, 100, 3);

        Assert.assertEquals(-1, policy.nextRetryMs(0, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(-1, policy.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(-1, policy.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void untilElapsedTest() {
        RetryUntilElapsed policy = new RetryUntilElapsed(2500, 50, 3);

        assertDuration(50,  100, policy.nextRetryMs(0, 0));
        assertDuration(100, 200, policy.nextRetryMs(1, 75));
        assertDuration(200, 400, policy.nextRetryMs(2, 225));
        assertDuration(400, 800, policy.nextRetryMs(3, 525));
        assertDuration(400, 800, policy.nextRetryMs(4, 1125));
        assertDuration(400, 800, policy.nextRetryMs(5, 1725));

        Assert.assertEquals(175, policy.nextRetryMs(6, 2325));
        Assert.assertEquals(-1, policy.nextRetryMs(7, 2500));
    }

    @Test
    public void updateElapsedTest() {
        RetryUntilElapsed policy = new RetryUntilElapsed(2500, 50, 3);

        Assert.assertEquals(2500, policy.getMaxElapsedMillis());
        Assert.assertEquals(50, policy.getBackoffMillis());
        Assert.assertEquals(3, policy.getBackoffCeiling());
        assertDuration(50, 100, policy.nextRetryMs(0, 0));

        RetryUntilElapsed updated = policy.withMaxElapsedMs(1000).withBackoffMs(100).withBackoffCeiling(1);

        Assert.assertEquals(1000, updated.getMaxElapsedMillis());
        Assert.assertEquals(100, updated.getBackoffMillis());
        Assert.assertEquals(1, updated.getBackoffCeiling());
        assertDuration(100, 200, updated.nextRetryMs(0, 0));
    }

    private void assertDuration(long from, long to, long ms) {
        Assert.assertTrue(from <= ms);
        Assert.assertTrue(to >= ms);
    }
}
