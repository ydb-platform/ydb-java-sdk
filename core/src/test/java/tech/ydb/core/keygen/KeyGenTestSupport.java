package tech.ydb.core.keygen;

import java.util.Base64;
import java.util.UUID;

/**
 * Shared helpers for {@link UuidKeyGen} and {@link TextKeyGen} tests.
 */
final class KeyGenTestSupport {

    private KeyGenTestSupport() {
    }

    static long timestampMask(int prefixBits) {
        int maskPos = prefixBits - 1;
        return (((1L << BaseKeyGen.TIMESTAMP_BITS) - 1L) << BaseKeyGen.TIMESTAMP_FIELD_LOW_BIT) >>> maskPos;
    }

    static long logicalMsbFromUuid(UUID uuid) {
        return BaseKeyGen.reorder(uuid.getMostSignificantBits());
    }

    static long logicalMsbFromTextKey(String key) {
        return bytesFromTextKey(key).getLong(0);
    }

    static long logicalLsbFromTextKey(String key) {
        return bytesFromTextKey(key).getLong(8);
    }

    private static java.nio.ByteBuffer bytesFromTextKey(String key) {
        String padded = key;
        while (padded.length() % 4 != 0) {
            padded += "=";
        }
        byte[] data = Base64.getUrlDecoder().decode(padded);
        if (data.length != 16) {
            throw new IllegalArgumentException("Expected 16 bytes, got " + data.length);
        }
        return java.nio.ByteBuffer.wrap(data);
    }

    static long extractEmbeddedPrefix(long logicalMsb, BaseKeyGen generator) {
        return logicalMsb & generator.getPrefixMask();
    }

    static int extractEmbeddedTimestampCode(long logicalMsb, int prefixBits) {
        long tsMask = timestampMask(prefixBits);
        int shift = BaseKeyGen.TIMESTAMP_FIELD_LOW_BIT - (prefixBits - 1);
        return (int) ((logicalMsb & tsMask) >>> shift);
    }

    static int logicalVersionNibble(long logicalMsb) {
        return (int) ((logicalMsb >> 12) & 0x0f);
    }

    static int logicalVariantBits(long logicalLsb) {
        return (int) ((logicalLsb >>> 56) & 0xc0);
    }

    static UUID uuidFromTextKey(String key) {
        return new UUID(
                BaseKeyGen.reorder(KeyGenTestSupport.logicalMsbFromTextKey(key)),
                KeyGenTestSupport.logicalLsbFromTextKey(key));
    }
}
