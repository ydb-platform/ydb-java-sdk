package tech.ydb.topic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;

/**
 * Unit tests for check simple logic for register custom codec
 *
 * @author Evgeny Kuvardin
 */
public class CodecRegistryTest {
    @Test
    public void standardCodecsTest() {
        CodecRegistry registry = new CodecRegistry();

        Assert.assertSame(RawCodec.getInstance(), registry.getCodec(Codec.RAW));
        Assert.assertSame(GzipCodec.getInstance(), registry.getCodec(Codec.GZIP));
        Assert.assertSame(LzopCodec.getInstance(), registry.getCodec(Codec.LZOP));
        Assert.assertSame(ZstdCodec.getInstance(), registry.getCodec(Codec.ZSTD));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void registerCustomCodecShouldDoubleRegisterCodecAndReturnLastCodec() {
        CodecRegistry registry = new CodecRegistry();

        Codec codec1 = new CodecTopic(10001);
        Codec codec2 = new CodecTopic(10001);

        registry.registerCodec(codec1);
        Assert.assertEquals(codec1, registry.registerCodec(codec2));

        Assert.assertEquals(codec2, registry.getCodec(10001));
        Assert.assertNotEquals(codec1, registry.getCodec(10001));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void registerCustomCodecShouldNotAcceptNull() {
        CodecRegistry registry = new CodecRegistry();
        Exception ex = Assert.assertThrows(IllegalArgumentException.class, () -> registry.registerCodec(null));
        Assert.assertEquals("Codec must be not null", ex.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void customCodecShouldNotAcceptNull() {
        Collection<Codec> codecs = Arrays.asList(RawCodec.getInstance(), null);
        Exception ex = Assert.assertThrows(IllegalArgumentException.class, () -> new CodecRegistry(codecs));
        Assert.assertEquals("Codec must be not null", ex.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void registerCustomCodecShouldRegisterAndOverrideAnyCodec() {
        CodecRegistry registry = new CodecRegistry();

        Assert.assertSame(RawCodec.getInstance(), registry.registerCodec(new CodecTopic(1)));
        Assert.assertSame(GzipCodec.getInstance(), registry.registerCodec(new CodecTopic(2)));
        Assert.assertSame(LzopCodec.getInstance(), registry.registerCodec(new CodecTopic(3)));
        Assert.assertSame(ZstdCodec.getInstance(), registry.registerCodec(new CodecTopic(4)));

    }

    static class CodecTopic implements Codec {
        private final int codec;

        public CodecTopic(int codecId) {
            this.codec = codecId;
        }

        @Override
        public String toString() {
            return "CustomCodec";
        }

        @Override
        public int getId() {
            return codec;
        }

        @Override
        public InputStream decode(InputStream byteArrayInputStream) throws IOException {
            return null;
        }

        @Override
        public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
            return null;
        }
    }
}
