package tech.ydb.topic.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tech.ydb.topic.description.CustomTopicCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Unit tests for check simple logic for register custom codec
 *
 * @author Evgeny Kuvardin
 */
public class YdbTopicsCustomCodecTest {
    CodecRegistryImpl registry;

    @Before
    public void beforeTest() {
        registry = new CodecRegistryImpl();
    }

    @Test
    public void registerCustomCodecShouldUnRegisterCodec() {
        registry.registerCustomCodec(10224, new CustomCustomTopicCode());
        registry.unregisterCustomCodec(10224);

        Assert.assertNull(registry.getCustomCodec(10224));
    }

    @Test
    public void registerCustomCodecShouldDoubleRegisterCodecAndReturnLastCodec() {
        CustomTopicCodec codec1 = new CustomCustomTopicCode();
        CustomTopicCodec codec2 = new CustomCustomTopicCode();

        registry.registerCustomCodec(10224, codec1);
        Assert.assertEquals(codec1, registry.registerCustomCodec(10224, codec2));

        Assert.assertEquals(codec2, registry.getCustomCodec(10224));
        Assert.assertNotEquals(codec1, registry.getCustomCodec(10224));
    }

    @Test
    public void registerCustomCodecShouldNotAcceptNull() {
        Assert.assertThrows(
                AssertionError.class,
                () -> registry.registerCustomCodec(10224, null));
    }

    @Test
    public void registerCustomCodecShouldFailedWhenRegisterReservedCode() {
        CustomTopicCodec codec1 = new CustomCustomTopicCode();
        expectErrorRegister(-1, codec1);
        expectErrorRegister(-100, codec1);
        expectErrorRegister(0, codec1);
        expectErrorRegister(1, codec1);
        expectErrorRegister(2, codec1);
        expectErrorRegister(3, codec1);
        expectErrorRegister(4, codec1);
        expectErrorRegister(10000, codec1);
    }

    @Test
    public void unregisterCustomCodecShouldFailedWhenRegisterReservedCode() {
        expectErrorUnregister(-1);
        expectErrorUnregister(-100);
        expectErrorUnregister(0);
        expectErrorUnregister(1);
        expectErrorUnregister(2);
        expectErrorUnregister(3);
        expectErrorUnregister(4);
        expectErrorUnregister(10000);
    }

    void expectErrorRegister(int codec, CustomTopicCodec customTopicCodec) {
        Exception e = Assert.assertThrows(
                RuntimeException.class,
                () -> registry.registerCustomCodec(codec, customTopicCodec));

        Assert.assertEquals("Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000", e.getMessage());
    }

    void expectErrorUnregister(int codec) {
        Exception e = Assert.assertThrows(
                RuntimeException.class,
                () -> registry.unregisterCustomCodec(codec));

        Assert.assertEquals("Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000", e.getMessage());
    }


    static class CustomCustomTopicCode implements CustomTopicCodec {

        @Override
        public InputStream decode(ByteArrayInputStream byteArrayOutputStream) {
            return null;
        }

        @Override
        public OutputStream encode(ByteArrayOutputStream byteArrayOutputStream) {
            return null;
        }
    }
}
