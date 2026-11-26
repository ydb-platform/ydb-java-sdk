package tech.ydb.topic.settings;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.topic.description.Codec;

public class MultiWriterSettingsTest {
    @Test
    public void defaultsAndExactBufferBoundary() {
        MultiWriterSettings config = settings().build();
        Assert.assertEquals(Codec.RAW, config.getCodec());
        Assert.assertEquals(MultiWriterSettings.RoutingMode.AUTO, config.getRoutingMode());
        Assert.assertEquals(64L * 1024 * 1024, config.getBufferBytes());
        Assert.assertEquals(10000, config.getBufferMessages());
        Assert.assertEquals(1024 * 1024, config.getMaxMessageBytes());
        Assert.assertEquals(32, config.getMaxWriters());
        Assert.assertEquals(4, config.getMaxConcurrentInits());
        Assert.assertEquals(4, config.getIoThreads());
        Assert.assertEquals(8L * 1024 * 1024, config.getWriterBufferBytes());
        Assert.assertEquals(1024, config.getWriterBufferMessages());
        Assert.assertEquals(Duration.ZERO, config.getDeliveryTimeout());
        Assert.assertEquals(Duration.ofSeconds(30), config.getInitTimeout());
        Assert.assertFalse(config.isDirectWrite());
        Assert.assertEquals(Integer.MAX_VALUE - 1L,
                settings().setWriterBufferBytes(Integer.MAX_VALUE - 1L).build().getWriterBufferBytes());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setWriterBufferBytes(Integer.MAX_VALUE).build());
    }

    @Test
    public void rejectsImpossibleCapacityAndUnboundedDeadlines() {
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setBufferBytes(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setBufferMessages(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setMaxMessageBytes(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setBufferBytes(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setWriterBufferBytes(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setMaxWriters(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setMaxConcurrentInits(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setIoThreads(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setInitTimeout(Duration.ZERO).build());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setDeliveryTimeout(Duration.ofNanos(-1)).build());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setRecoveryTimeout(Duration.ofSeconds(Long.MAX_VALUE)).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setCodec(0).build());
    }

    @Test
    public void requiresStablePrefixAndMeasuresItInUtf8Bytes() {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            prefix.append('я');
        }
        Assert.assertEquals(prefix.toString(), settings().setProducerPrefix(prefix.toString()).build()
                .getProducerPrefix());
        prefix.append('я');
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setProducerPrefix(prefix.toString()).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setProducerPrefix(" ").build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setProducerPrefix(null).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setTopicPath(null).build());
    }

    private static MultiWriterSettings.Builder settings() {
        return MultiWriterSettings.newBuilder().setTopicPath("/test/topic").setProducerPrefix("owner");
    }
}
