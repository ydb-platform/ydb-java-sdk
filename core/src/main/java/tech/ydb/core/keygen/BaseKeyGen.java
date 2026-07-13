package tech.ydb.core.keygen;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Implementation helpers for key generators.
 *
 * @author zinal
 */
public class BaseKeyGen {

    /**
     * Bit width of the embedded second-precision timestamp field.
     */
    static final int TIMESTAMP_BITS = 30;

    /**
     * Number of seconds which fit within the bit width specified.
     */
    static final long TIMESTAMP_SECONDS = (1L << TIMESTAMP_BITS);

    /**
     * Low bit index (inclusive) of the 30-bit timestamp field when
     * {@code maskPos == 0} (1-bit prefix). Shifts down with larger prefixes.
     */
    static final int TIMESTAMP_FIELD_LOW_BIT = 33;

    /**
     * A position within an array of pre-computed bitmasks to be used.
     */
    protected final int maskPos;

    protected BaseKeyGen(int prefixBits) {
        if (prefixBits < 1 || prefixBits > 18) {
            throw new IllegalArgumentException("Unsupported prefix length: " + prefixBits);
        }
        this.maskPos = prefixBits - 1;
    }

    /**
     * @return Prefix size used for construction, in bits.
     */
    public int getPrefixBits() {
        return maskPos + 1;
    }

    /**
     * @return Prefix mask to be applied
     */
    public long getPrefixMask() {
        return Holder.PREFIX_MASKS[maskPos];
    }

    /**
     * Generates the new shared prefix to generate a series of related IDs.
     *
     * @return Random value to be used as a prefix.
     */
    public long nextPrefix() {
        final SecureRandom sr = Holder.SR;
        byte[] data = new byte[8];
        sr.nextBytes(data);
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            lsb = (lsb << 8) | (data[i] & 0xff);
        }
        return lsb;
    }

    protected final long update(long msb, long prefix, Instant instant) {
        long tsMask = Holder.TS_MASKS[maskPos];
        long tsCode = getTimestampCode(instant);
        tsCode = tsCode << (TIMESTAMP_FIELD_LOW_BIT - maskPos);
        long bits;
        if (prefix == -1L) {
            bits = msb & ~tsMask;
            bits |= tsCode & tsMask;
        } else {
            long prefixMask = Holder.PREFIX_MASKS[maskPos];
            bits = msb & ~(prefixMask | tsMask);
            bits |= (prefix & prefixMask) | (tsCode & tsMask);
        }
        return bits;
    }

    /**
     * Computes the second-precision timestamp code: seconds since
     * 2020-01-01T00:00:00Z, in the range {@code [0, 2^30)}.
     *
     * @param instant the instant to be used
     * @return timestamp code between 0 and 2^30 - 1, inclusive
     */
    public static int getTimestampCode(Instant instant) {
        Instant sec = instant.truncatedTo(ChronoUnit.SECONDS);
        long diff = sec.getEpochSecond() % TIMESTAMP_SECONDS;
        if (diff < 0) {
            throw new IllegalArgumentException(
                    "Instant out of 30-bit timestamp range: " + instant);
        }
        return (int) diff;
    }

    /**
     * YDB uses GUID (Microsoft-style) mixed-endian format.
     *
     * <pre>
     * xxxxxxxx 0 1 2 3x 4 5 6 7
     * INPUT:  01020304 05060708 090a0b0c 0d0e0f10
     * OUTPUT: 04030201 06050807 090a0b0c 0d0e0f10
     * </pre>
     *
     * This function puts the bytes of MSB in the proper order.
     *
     * @param v MSB value in standard big-endian long representation
     * @return MSB value with byte order adjusted for YDB GUID storage
     */
    public static long reorder(long v) {
        long b0 = (v >>> 56) & 0xffL;
        long b1 = (v >>> 48) & 0xffL;
        long b2 = (v >>> 40) & 0xffL;
        long b3 = (v >>> 32) & 0xffL;
        long b4 = (v >>> 24) & 0xffL;
        long b5 = (v >>> 16) & 0xffL;
        long b6 = (v >>> 8) & 0xffL;
        long b7 = v & 0xffL;
        return (b3 << 56) | (b2 << 48) | (b1 << 40) | (b0 << 32)
                | (b5 << 24) | (b4 << 16) | (b7 << 8) | b6;
    }

    public static long updateVersion(long msb) {
        return (msb & ~0xF000L) | 0x8000L;
    }

    public static long updateVariant(long lsb) {
        return (lsb & 0x3FFFFFFFFFFFFFFFL)
                | 0x8000000000000000L;
    }

    /**
     * Convert a UUID value to a base64 text representation.
     *
     * @param uuid Value to be converted
     * @return Text representation of a UUID value of a fixed length 22 symbols
     */
    public static String toString(UUID uuid) {
        // apply byte swaps to restore the "regular" ordering
        long msb = reorder(uuid.getMostSignificantBits());
        ByteBuffer byteArray = ByteBuffer.allocate(16);
        byteArray.putLong(msb);
        byteArray.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder()
                .encodeToString(byteArray.array())
                .substring(0, 22);
    }

    /**
     * A holder class to defer initialization until needed.
     */
    static class Holder {

        static final SecureRandom SR = new SecureRandom();

        static final long[] PREFIX_MASKS;
        static final long[] TS_MASKS;

        static {
            long[] pf = new long[32];
            long[] ts = new long[32];
            pf[0] = 0x8000000000000000L;
            ts[0] = (((1L << TIMESTAMP_BITS) - 1) << TIMESTAMP_FIELD_LOW_BIT);
            for (int i = 1; i < 32; ++i) {
                pf[i] = pf[i - 1] | (1L << (63 - i));
                ts[i] = (ts[i - 1] >>> 1);
            }
            PREFIX_MASKS = pf;
            TS_MASKS = ts;
        }
    }
}
