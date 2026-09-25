package tech.ydb.topic.settings;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReaderSettingsTest {

    @Test
    public void nonPositiveBufferSizeTest() {
        IllegalArgumentException ex1 = Assert.assertThrows(IllegalArgumentException.class,
                () -> ReaderSettings.newBuilder().setMaxMemoryUsageBytes(0).build());
        Assert.assertEquals("maxMemoryUsageBytes must be positive, but got 0", ex1.getMessage());
        IllegalArgumentException ex2 = Assert.assertThrows(IllegalArgumentException.class,
                () -> ReaderSettings.newBuilder().setMaxMemoryUsageBytes(-1).build());
        Assert.assertEquals("maxMemoryUsageBytes must be positive, but got -1", ex2.getMessage());
    }

    @Test
    public void validateTopicsListTest() {
        Exception ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ReaderSettings.newBuilder().setConsumerName("consumer").build()
        );
        Assert.assertEquals("Missing topics for read settings. At least one should be set", ex.getMessage());
    }

    @Test
    public void validateConsumerNameTest() {
        Exception ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ReaderSettings.newBuilder().addTopic("/topic").build()
        );
        Assert.assertEquals("Missing consumer name for read settings. Use withoutConsumer option explicitly if you "
                + "want to read without a consumer", ex.getMessage());
    }

    @Test
    public void validateWithoutConsumerTest() {
        Exception ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ReaderSettings.newBuilder().addTopic("/topic").setConsumerName("c").withoutConsumer().build()
        );
        Assert.assertEquals(
                "Both mutually exclusive options consumerName and withoutConsumer are set for read settings",
                ex.getMessage()
        );
    }

    @Test
    public void validateRetryConfigTest() {
        Exception ex = Assert.assertThrows(
                NullPointerException.class,
                () -> ReaderSettings.newBuilder().addTopic("/topic").setConsumerName("c").setRetryConfig(null).build()
        );
        Assert.assertEquals("RetryConfig must not be null", ex.getMessage());
    }
}
