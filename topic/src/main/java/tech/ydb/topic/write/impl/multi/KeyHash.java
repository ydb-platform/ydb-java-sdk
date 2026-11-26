package tech.ydb.topic.write.impl.multi;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.MurmurHash2;

final class KeyHash {
    private static final int KAFKA_SEED = 0x9747b28c;

    private KeyHash() { }

    static byte[] boundKey(String key) {
        return ByteBuffer.allocate(Long.BYTES).putLong(murmur64(key.getBytes(StandardCharsets.UTF_8))).array();
    }

    static int kafkaHash(String key) {
        return murmur32(key.getBytes(StandardCharsets.UTF_8)) & 0x7fffffff;
    }

    static long murmur64(byte[] data) {
        return MurmurHash2.hash64(data, data.length, 0);
    }

    static int murmur32(byte[] data) {
        return MurmurHash2.hash32(data, data.length, KAFKA_SEED);
    }
}
