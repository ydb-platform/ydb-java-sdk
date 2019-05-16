package tech.ydb.table.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class LittleEndianTest {

    @Test
    public void bswap() {
        assertThat(LittleEndian.bswap(0x0000000000000000L)).isEqualTo(0x0000000000000000L);
        assertThat(LittleEndian.bswap(0x1234567890abcdefL)).isEqualTo(0xefcdab9078563412L);
        assertThat(LittleEndian.bswap(0xffffffffffffffffL)).isEqualTo(0xffffffffffffffffL);
    }
}
