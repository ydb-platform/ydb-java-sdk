package ru.yandex.ydb.table.utils;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class HexTest {

    @Test
    public void empty() {
        StringBuilder sb = new StringBuilder();
        Hex.toHex(ByteString.EMPTY, sb);
        Assert.assertEquals("", sb.toString());
    }

    @Test
    public void oneByte() {
        for (int i = 0; i <= 255; i++) {
            String expected = String.format("%02x", i);

            StringBuilder sb = new StringBuilder();
            Hex.toHex(ByteString.copyFrom(new byte[] { (byte) i }), sb);
            Assert.assertEquals(expected, sb.toString());
        }
    }

    @Test
    public void manyBytes() {
        StringBuilder sb = new StringBuilder();
        Hex.toHex(ByteString.copyFrom(new byte[] {
            (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33,
            (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
            (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff,
        }), sb);
        Assert.assertEquals("00112233445566778899aabbccddeeff", sb.toString());
    }
}
