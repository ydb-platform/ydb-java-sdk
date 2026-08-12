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

        Assert.assertEquals("VirtualTimestamp{planStep=1193046, txId=39030}", vt.toString());
    }

    @Test
    public void hashCodeAndEqualsTest() {
        VirtualTimestamp vt1 = new VirtualTimestamp(0x123456L, 0x9876L);
        VirtualTimestamp vt2 = new VirtualTimestamp(0x123456L, 0x9877L);
        VirtualTimestamp vt3 = new VirtualTimestamp(0x123455L, 0x9876L);
        VirtualTimestamp vt4 = new VirtualTimestamp(0x123456L, 0x9876L);

        Assert.assertNotEquals(vt1, null);
        Assert.assertNotEquals(vt1, new Object());

        Assert.assertEquals(vt1, vt1);
        Assert.assertEquals(vt1, vt4);
        Assert.assertNotEquals(vt1, vt2);
        Assert.assertNotEquals(vt1, vt3);
        Assert.assertNotEquals(vt2, vt3);

        Assert.assertEquals(vt1.hashCode(), vt4.hashCode());
        Assert.assertNotEquals(vt1.hashCode(), vt2.hashCode());
        Assert.assertNotEquals(vt1.hashCode(), vt3.hashCode());
        Assert.assertNotEquals(vt2.hashCode(), vt3.hashCode());
    }
}
