package tech.ydb.common.transaction;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class VirtualTimestampTest {

    @Test
    public void getterTest() {
        VirtualTimestamp vt = new VirtualTimestamp(0x123456L, 0x9876L);
        Assert.assertEquals(0x123456L, vt.getPlanStep());
        Assert.assertEquals(0x9876L, vt.getTxId());
    }
}
