package tech.ydb.core.keygen;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Random UUID generator creates cache friendly identifiers to be used as
 * primary keys for YDB row-organized tables.
 *
 * Each generated value consists of a random prefix of the specified length, a
 * second-precision timestamp code of 30 bits, followed by the random suffix.
 *
 * The timestamp is UNIX epoch seconds, modulo 2^30 (about 34 years), which
 * avoids code reuse for well over 30 years from the overlap.
 *
 * In addition, the generator supports the "fixed prefix" schema, in which a
 * common prefix value is used for a series of related ids generated (typically
 * to be written in a single transaction).
 *
 * The generated value is optimized for the actual internal format of UUID
 * storage in YDB, which uses GUID (Microsoft-style) mixed-endian byte ordering.
 * This affects the actual ordering, so it is important to properly put the
 * bytes in the correct order.
 *
 * @author zinal
 */
public class UuidKeyGen extends BaseKeyGen {

    /**
     * Constructs the generator instance with the default prefix size of 10
     * bits.
     *
     * Works best for up to 1k table partitions.
     */
    public UuidKeyGen() {
        super(10);
    }

    /**
     * Constructs the generator instance with the custom prefix size.
     *
     * @param prefixBits Number of bits for the prefix, 1 to 18 bits.
     */
    public UuidKeyGen(int prefixBits) {
        super(prefixBits);
    }

    /**
     * Generates the new ID with the specified prefix value and calendar date
     * (UTC midnight).
     *
     * @param prefix Prefix value
     * @param date The date whose start-of-day UTC instant is embedded
     * @return Random UUID with the embedded prefix, timestamp code and suffix.
     */
    public UUID nextValue(long prefix, LocalDate date) {
        return nextValue(prefix, date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    /**
     * Generates the new ID with the specified prefix value and instant
     * (truncated to whole seconds for the embedded timestamp field).
     *
     * @param prefix Prefix value
     * @param instant The instant whose second is embedded in the UUID
     * @return Random UUID with the embedded prefix, timestamp code and suffix.
     */
    public UUID nextValue(long prefix, Instant instant) {
        SecureRandom ng = Holder.numberGenerator;
        byte[] data = new byte[16];
        ng.nextBytes(data);

        data[6] &= 0x0f;
        data[6] |= (byte) 0x80;
        data[8] &= 0x3f;
        data[8] |= (byte) 0x80;

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (data[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (data[i] & 0xff);
        }

        msb = update(msb, prefix, instant);
        return new UUID(reorder(msb), lsb);
    }

    /**
     * Generates the new ID with the specified prefix value.
     *
     * @param prefix Prefix value
     * @return Random UUID with the embedded prefix, timestamp code and suffix.
     */
    public UUID nextValue(long prefix) {
        return nextValue(prefix, Instant.now());
    }

    /**
     * Generates the new ID with the random prefix value and a specified
     * instant.
     *
     * @param instant Timestamp for the value being generated.
     * @return Random UUID with the embedded prefix, timestamp code and suffix.
     */
    public UUID nextValue(Instant instant) {
        return nextValue(-1L, instant);
    }

    /**
     * Generates the new ID with the random prefix value and a specified date.
     *
     * @param date The date (UTC midnight) used for the embedded timestamp
     * @return Random UUID with the embedded prefix, timestamp code and suffix.
     */
    public UUID nextValue(LocalDate date) {
        return nextValue(-1L, date);
    }

    /**
     * Generates the new ID with the random prefix value.
     *
     * @return Random UUID with the embedded prefix, timestamp code and suffix.
     */
    public UUID nextValue() {
        return nextValue(-1L, Instant.now());
    }

}
