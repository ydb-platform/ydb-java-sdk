package tech.ydb.table.utils;

import org.junit.Assert;
import org.junit.Test;



/**
 * @author Sergey Polovko
 */
public class LittleEndianTest {

    @Test
    public void bswap() {
        Assert.assertEquals(0x0000000000000000L, LittleEndian.bswap(0x0000000000000000L));
        Assert.assertEquals(0xefcdab9078563412L, LittleEndian.bswap(0x1234567890abcdefL));
        Assert.assertEquals(0xffffffffffffffffL, LittleEndian.bswap(0xffffffffffffffffL));
    }
}
