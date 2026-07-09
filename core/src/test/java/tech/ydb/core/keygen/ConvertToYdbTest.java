package tech.ydb.core.keygen;

import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zinal
 */
public class ConvertToYdbTest {

    @Test
    public void convertToYdbTest() {
        byte[] input = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        long v0 = ByteBuffer.wrap(input).getLong();
        long v1 = UuidKeyGen.reorder(v0);
        long v2 = UuidKeyGen.reorder(v1);
        Assert.assertEquals(v0, v2);
        Assert.assertNotEquals(v0, v1);
    }

}
