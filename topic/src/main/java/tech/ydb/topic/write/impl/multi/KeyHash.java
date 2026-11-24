package tech.ydb.topic.write.impl.multi;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Murmur2 variants used by the Go SDK partition choosers. */
final class KeyHash {
    private static final long MULTIPLIER_64 = 0xc6a4a7935bd1e995L;
    private static final int MULTIPLIER_32 = 0x5bd1e995;
    private static final int KAFKA_SEED = 0x9747b28c;

    private KeyHash() { }

    static byte[] boundKey(String key) {
        return ByteBuffer.allocate(Long.BYTES).putLong(murmur64(key.getBytes(StandardCharsets.UTF_8))).array();
    }

    static int kafkaHash(String key) {
        return murmur32(key.getBytes(StandardCharsets.UTF_8)) & 0x7fffffff;
    }

    static long murmur64(byte[] data) {
        long hash = data.length * MULTIPLIER_64;
        int offset = 0;
        while (offset <= data.length - Long.BYTES) {
            long block = 0;
            for (int index = 0; index < Long.BYTES; index++) {
                block |= (data[offset + index] & 0xffL) << (index * Byte.SIZE);
            }
            block *= MULTIPLIER_64;
            block ^= block >>> 47;
            block *= MULTIPLIER_64;
            hash ^= block;
            hash *= MULTIPLIER_64;
            offset += Long.BYTES;
        }
        int remaining = data.length - offset;
        for (int index = 0; index < remaining; index++) {
            hash ^= (data[offset + index] & 0xffL) << (index * Byte.SIZE);
        }
        if (remaining > 0) {
            hash *= MULTIPLIER_64;
        }
        hash ^= hash >>> 47;
        hash *= MULTIPLIER_64;
        hash ^= hash >>> 47;
        return hash;
    }

    static int murmur32(byte[] data) {
        int hash = KAFKA_SEED ^ data.length;
        int offset = 0;
        while (offset <= data.length - Integer.BYTES) {
            int block = 0;
            for (int index = 0; index < Integer.BYTES; index++) {
                block |= (data[offset + index] & 0xff) << (index * Byte.SIZE);
            }
            block *= MULTIPLIER_32;
            block ^= block >>> 24;
            block *= MULTIPLIER_32;
            hash *= MULTIPLIER_32;
            hash ^= block;
            offset += Integer.BYTES;
        }
        int remaining = data.length - offset;
        for (int index = 0; index < remaining; index++) {
            hash ^= (data[offset + index] & 0xff) << (index * Byte.SIZE);
        }
        if (remaining > 0) {
            hash *= MULTIPLIER_32;
        }
        hash ^= hash >>> 13;
        hash *= MULTIPLIER_32;
        hash ^= hash >>> 15;
        return hash;
    }
}
