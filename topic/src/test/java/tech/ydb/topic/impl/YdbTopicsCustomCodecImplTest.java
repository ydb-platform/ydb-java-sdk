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
public class YdbTopicsCustomCodecImplTest {
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
    public void registerCustomCodecShouldFailedWhenRegisterReservedCode() {
        CodecTopic codec1 = new CodecTopic();
        expectErrorRegister(-1, codec1);
        expectErrorRegister(-100, codec1);
        expectErrorRegister(0, codec1);
        expectErrorRegister(1, codec1);
        expectErrorRegister(2, codec1);
        expectErrorRegister(3, codec1);
        expectErrorRegister(4, codec1);
        expectErrorRegister(10000, codec1);
    }

    void expectErrorRegister(int codecId, CodecTopic codec) {
        codec.setCodecId(codecId);
        Exception e = Assert.assertThrows(
                RuntimeException.class,
                () -> registry.registerCodec(codec));

        Assert.assertEquals("Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000", e.getMessage());
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
