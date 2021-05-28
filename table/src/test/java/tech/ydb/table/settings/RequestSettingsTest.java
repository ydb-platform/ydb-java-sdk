package tech.ydb.table.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import tech.ydb.table.utils.RequestSettingsUtils;
import org.junit.Assert;
import org.junit.Test;

public class RequestSettingsTest {

    @Test
    public void deadlineVsTimeout() {
        ExecuteDataQuerySettings settings = new ExecuteDataQuerySettings();
        Assert.assertEquals(0, settings.getDeadlineAfter());
        Assert.assertFalse(settings.getTimeout().isPresent());
        Assert.assertFalse(settings.getOperationTimeout().isPresent());
        Assert.assertFalse(settings.getCancelAfter().isPresent());

        settings.setDeadlineAfter(System.nanoTime() + Duration.ofSeconds(5).toNanos());
        Assert.assertTrue(settings.getDeadlineAfter() > System.nanoTime() + Duration.ofSeconds(4).toNanos());
        Assert.assertTrue(settings.getDeadlineAfter() < System.nanoTime() + Duration.ofSeconds(6).toNanos());
        Assert.assertFalse(settings.getTimeout().isPresent());

        settings.setTimeout(Duration.ofSeconds(2));
        long calculatedDeadline = RequestSettingsUtils.calculateDeadlineAfter(settings);
        Assert.assertTrue(calculatedDeadline > System.nanoTime() + Duration.ofSeconds(1).toNanos());
        Assert.assertTrue(calculatedDeadline < System.nanoTime() + Duration.ofSeconds(3).toNanos());
        Assert.assertTrue(settings.getTimeout().isPresent());
        Assert.assertEquals(Duration.ofSeconds(2), settings.getTimeout().get());

        settings.setTimeout(Duration.ZERO);
        calculatedDeadline = RequestSettingsUtils.calculateDeadlineAfter(settings);
        Assert.assertEquals(0, calculatedDeadline);
        Assert.assertTrue(settings.getTimeout().isPresent());
        Assert.assertEquals(Duration.ZERO, settings.getTimeout().get());

        settings.setTimeout(Duration.ofSeconds(8).toMillis(), TimeUnit.MILLISECONDS);
        calculatedDeadline = RequestSettingsUtils.calculateDeadlineAfter(settings);
        Assert.assertTrue(calculatedDeadline > System.nanoTime() + Duration.ofSeconds(7).toNanos());
        Assert.assertTrue(calculatedDeadline < System.nanoTime() + Duration.ofSeconds(9).toNanos());
        Assert.assertTrue(settings.getTimeout().isPresent());

        settings.setTimeout(0, TimeUnit.MICROSECONDS);
        calculatedDeadline = RequestSettingsUtils.calculateDeadlineAfter(settings);
        Assert.assertEquals(0, calculatedDeadline);
        Assert.assertTrue(settings.getTimeout().isPresent());
        Assert.assertEquals(Duration.ZERO, settings.getTimeout().get());

        settings.setDeadlineAfter(System.nanoTime() + Duration.ofSeconds(3).toNanos());
        calculatedDeadline = RequestSettingsUtils.calculateDeadlineAfter(settings);
        // Setting Timeout has higher priority than setting DeadlineAfter
        Assert.assertEquals(0, calculatedDeadline);
        Assert.assertTrue(settings.getTimeout().isPresent());
        Assert.assertTrue(settings.getDeadlineAfter() > System.nanoTime() + Duration.ofSeconds(2).toNanos());
        Assert.assertTrue(settings.getDeadlineAfter() < System.nanoTime() + Duration.ofSeconds(4).toNanos());

        settings.setOperationTimeout(Duration.ofSeconds(3));
        Assert.assertTrue(settings.getOperationTimeout().isPresent());
        Assert.assertEquals(Duration.ofSeconds(3), settings.getOperationTimeout().get());

        settings.setCancelAfter(Duration.ofSeconds(4));
        Assert.assertTrue(settings.getCancelAfter().isPresent());
        Assert.assertEquals(Duration.ofSeconds(4), settings.getCancelAfter().get());
    }
}
