package tech.ydb.topic.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Unit tests for check simple logic for register custom codec
 *
 * @author Evgeny Kuvardin
 */
public class CodecRegistryTest {
    CodecRegistry registry;

    private static final int codecId = 10113;

    @Before
    public void beforeTest() {
        registry = new CodecRegistry();
    }

    @Test
    public void registerCustomCodecShouldDoubleRegisterCodecAndReturnLastCodec() {
        Codec codec1 = new CodecTopic();
        Codec codec2 = new CodecTopic();

        registry.registerCodec(codec1);
        Assert.assertEquals(codec1, registry.registerCodec(codec2));

        Assert.assertEquals(codec2, registry.getCodec(codecId));
        Assert.assertNotEquals(codec1, registry.getCodec(codecId));
    }

    @Test
    public void registerCustomCodecShouldNotAcceptNull() {
        Assert.assertThrows(
                AssertionError.class,
                () -> registry.registerCodec(null));
    }

    @Test
    public void registerCustomCodecShouldRegisterAndOverrideAnyCodec() {
        CodecTopic codec1 = new CodecTopic();
        expectRegisterCodec(1, codec1, RawCodec.getInstance());
        expectRegisterCodec(2, codec1, GzipCodec.getInstance());
        expectRegisterCodec(3, codec1, LzopCodec.getInstance());
        expectRegisterCodec(4, codec1, ZstdCodec.getInstance());
    }

    void expectRegisterCodec(int codecId, CodecTopic newCodec, Codec oldCodec) {
        newCodec.setCodecId(codecId);
        Codec codecOldPredefined = registry.registerCodec(newCodec);
        Assert.assertSame(codecOldPredefined, oldCodec);
    }

    static class CodecTopic implements Codec {

        int codec;

        public CodecTopic() {
            this.codec = codecId;
        }

        public void setCodecId(int codecId) {
            this.codec = codecId;
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
