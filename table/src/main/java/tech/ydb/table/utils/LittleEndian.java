package tech.ydb.table.utils;

/**
 * @author Sergey Polovko
 */
public class LittleEndian {
    private LittleEndian() { }

    /**
     * Reverses the byte order of a long value.
     *
     * @param v long value
     * @return reversed long value
     */
    public static long bswap(long v) {
        return
            (v & 0x00000000000000ffL) << 56 |
            (v & 0x000000000000ff00L) << 40 |
            (v & 0x0000000000ff0000L) << 24 |
            (v & 0x00000000ff000000L) << 8 |
            (v & 0x000000ff00000000L) >>> 8 |
            (v & 0x0000ff0000000000L) >>> 24 |
            (v & 0x00ff000000000000L) >>> 40 |
            (v & 0xff00000000000000L) >>> 56;
    }

}
