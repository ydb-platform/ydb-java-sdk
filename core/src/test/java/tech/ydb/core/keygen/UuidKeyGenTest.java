package tech.ydb.core.keygen;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

/**
 * Correctness tests for {@link UuidKeyGen}.
 */
public class UuidKeyGenTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2024-06-15T12:34:56Z");
    private static final LocalDate FIXED_DATE = LocalDate.of(2024, 6, 15);
    private static final long FIXED_PREFIX = 0x2A5L;

    @Test
    public void defaultConstructorUsesTenPrefixBits() {
        UuidKeyGen generator = new UuidKeyGen();
        Assert.assertEquals(10, generator.getPrefixBits());
        Assert.assertEquals(0xFFC0000000000000L, generator.getPrefixMask());
    }

    @Test
    public void customPrefixBitsExposeMask() {
        UuidKeyGen oneBit = new UuidKeyGen(1);
        UuidKeyGen eighteenBit = new UuidKeyGen(18);

        Assert.assertEquals(1, oneBit.getPrefixBits());
        Assert.assertEquals(0x8000000000000000L, oneBit.getPrefixMask());
        Assert.assertEquals(18, eighteenBit.getPrefixBits());
        Assert.assertEquals(0xFFFFC00000000000L, eighteenBit.getPrefixMask());
    }

    @Test
    public void unsupportedPrefixLengthIsRejected() {
        assertUnsupportedPrefix(0);
        assertUnsupportedPrefix(-1);
        assertUnsupportedPrefix(19);
        assertUnsupportedPrefix(100);
    }

    private static void assertUnsupportedPrefix(int prefixBits) {
        try {
            new UuidKeyGen(prefixBits);
            Assert.fail("Expected IllegalArgumentException for prefixBits=" + prefixBits);
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported prefix length: " + prefixBits, ex.getMessage());
        }
    }

    @Test
    public void getTimestampCodeUsesWholeSeconds() {
        Instant withNanos = Instant.parse("2024-01-02T03:04:05.999999999Z");
        Instant truncated = Instant.parse("2024-01-02T03:04:05Z");

        Assert.assertEquals(BaseKeyGen.getTimestampCode(truncated),
                BaseKeyGen.getTimestampCode(withNanos));
        Assert.assertEquals(truncated.getEpochSecond() % BaseKeyGen.TIMESTAMP_SECONDS,
                BaseKeyGen.getTimestampCode(withNanos));
    }

    @Test
    public void getTimestampCodeRejectsNegativeEpochSecond() {
        try {
            BaseKeyGen.getTimestampCode(Instant.parse("1969-12-31T23:59:59Z"));
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("Instant out of 30-bit timestamp range"));
        }
    }

    @Test
    public void reorderSwapsMsbBytesAndIsInvolutory() {
        byte[] input = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        long original = ByteBuffer.wrap(input).getLong();
        long swapped = BaseKeyGen.reorder(original);

        Assert.assertNotEquals(original, swapped);
        Assert.assertEquals(original, BaseKeyGen.reorder(swapped));
        Assert.assertEquals(0x0403020106050807L, swapped);
    }

    @Test
    public void toStringProducesTwentyTwoCharUrlSafeBase64() {
        UUID uuid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
        String text = BaseKeyGen.toString(uuid);

        Assert.assertEquals(22, text.length());
        Assert.assertEquals("MyIRAFVEd2aImaq7zN3u_w", text);
        Assert.assertFalse(text.contains("+"));
        Assert.assertFalse(text.contains("/"));
    }

    @Test
    public void nextPrefixProducesDistinctValues() {
        UuidKeyGen generator = new UuidKeyGen(12);
        Set<Long> seen = new HashSet<>();

        for (int i = 0; i < 100; ++i) {
            seen.add(generator.nextPrefix());
        }

        Assert.assertTrue("Expected multiple distinct prefixes", seen.size() > 1);
    }

    @Test
    public void fixedPrefixIsMaskedWhenEmbedded() {
        UuidKeyGen generator = new UuidKeyGen(12);
        long rawPrefix = 0x123456789ABCDEFL;

        UUID uuid = generator.nextValue(rawPrefix, FIXED_INSTANT);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);

        Assert.assertEquals(rawPrefix & generator.getPrefixMask(),
                KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
    }

    @Test
    public void nextValueEmbedsFixedPrefixAndTimestamp() {
        UuidKeyGen generator = new UuidKeyGen(10);

        UUID uuid = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);

        Assert.assertEquals(FIXED_PREFIX & generator.getPrefixMask(),
                KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        Assert.assertEquals(BaseKeyGen.getTimestampCode(FIXED_INSTANT),
                KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits()));
    }

    @Test
    public void nextValueWithLocalDateUsesUtcMidnight() {
        UuidKeyGen generator = new UuidKeyGen(10);
        Instant expectedInstant = FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant();

        UUID uuid = generator.nextValue(FIXED_PREFIX, FIXED_DATE);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);

        Assert.assertEquals(BaseKeyGen.getTimestampCode(expectedInstant),
                KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits()));
    }

    @Test
    public void nextValueWithRandomPrefixPreservesRandomPrefixBits() {
        UuidKeyGen generator = new UuidKeyGen(10);
        Set<Long> prefixes = new HashSet<>();

        for (int i = 0; i < 50; ++i) {
            UUID uuid = generator.nextValue(FIXED_INSTANT);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);
            prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        }

        Assert.assertTrue("Expected varying random prefixes", prefixes.size() > 1);
    }

    @Test
    public void nextValueWithPrefixOnlyUsesCurrentSecond() {
        UuidKeyGen generator = new UuidKeyGen(10);
        int before = BaseKeyGen.getTimestampCode(Instant.now());

        UUID uuid = generator.nextValue(FIXED_PREFIX);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);
        int embedded = KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits());
        int after = BaseKeyGen.getTimestampCode(Instant.now());

        Assert.assertTrue(embedded >= before && embedded <= after);
    }

    @Test
    public void nextValueWithDateOnlyUsesUtcMidnightAndRandomPrefix() {
        UuidKeyGen generator = new UuidKeyGen(8);
        Instant expectedInstant = FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant();
        Set<Long> prefixes = new HashSet<>();

        for (int i = 0; i < 20; ++i) {
            UUID uuid = generator.nextValue(FIXED_DATE);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);
            prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
            Assert.assertEquals(BaseKeyGen.getTimestampCode(expectedInstant),
                    KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits()));
        }

        Assert.assertTrue("Expected varying random prefixes", prefixes.size() > 1);
    }

    @Test
    public void nextValueWithoutArgumentsUsesRandomPrefixAndCurrentSecond() {
        UuidKeyGen generator = new UuidKeyGen(10);
        int before = BaseKeyGen.getTimestampCode(Instant.now());
        Set<Long> prefixes = new HashSet<>();

        UUID uuid = generator.nextValue();
        long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);
        int embedded = KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits());
        int after = BaseKeyGen.getTimestampCode(Instant.now());

        prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        for (int i = 0; i < 20; ++i) {
            UUID next = generator.nextValue();
            prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(
                    KeyGenTestSupport.logicalMsbFromUuid(next), generator));
        }

        Assert.assertTrue(embedded >= before && embedded <= after);
        Assert.assertTrue("Expected varying random prefixes", prefixes.size() > 1);
    }

    @Test
    public void nextValueSetsVersionAndVariantBits() {
        UuidKeyGen generator = new UuidKeyGen();

        for (int i = 0; i < 20; ++i) {
            UUID uuid = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);

            Assert.assertEquals(2, uuid.variant());
            Assert.assertEquals(8, KeyGenTestSupport.logicalVersionNibble(logicalMsb));
            Assert.assertEquals(0x80, KeyGenTestSupport.logicalVariantBits(uuid.getLeastSignificantBits()));
        }
    }

    @Test
    public void nextValueWithSamePrefixChangesRandomSuffix() {
        UuidKeyGen generator = new UuidKeyGen(10);

        UUID first = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
        UUID second = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);

        Assert.assertNotEquals(first, second);
        Assert.assertNotEquals(first.getLeastSignificantBits(), second.getLeastSignificantBits());
        Assert.assertEquals(
                KeyGenTestSupport.extractEmbeddedPrefix(
                        KeyGenTestSupport.logicalMsbFromUuid(first), generator),
                KeyGenTestSupport.extractEmbeddedPrefix(
                        KeyGenTestSupport.logicalMsbFromUuid(second), generator));
        Assert.assertEquals(
                KeyGenTestSupport.extractEmbeddedTimestampCode(
                        KeyGenTestSupport.logicalMsbFromUuid(first), generator.getPrefixBits()),
                KeyGenTestSupport.extractEmbeddedTimestampCode(
                        KeyGenTestSupport.logicalMsbFromUuid(second), generator.getPrefixBits()));
    }

    @Test
    public void textRepresentationMatchesBaseKeyGenToString() {
        UuidKeyGen generator = new UuidKeyGen(10);

        for (int i = 0; i < 20; ++i) {
            UUID uuid = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
            Assert.assertEquals(BaseKeyGen.toString(uuid), encodeUuidPayload(uuid));
        }
    }

    @Test
    public void prefixAndTimestampLayoutsWorkForEdgePrefixSizes() {
        for (int prefixBits : new int[]{1, 18}) {
            UuidKeyGen generator = new UuidKeyGen(prefixBits);

            UUID uuid = generator.nextValue(0x123456789ABCDEFL, FIXED_INSTANT);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromUuid(uuid);

            Assert.assertEquals(0x123456789ABCDEFL & generator.getPrefixMask(),
                    KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
            Assert.assertEquals(BaseKeyGen.getTimestampCode(FIXED_INSTANT),
                    KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, prefixBits));
        }
    }

    @Test
    public void distinctCallsProduceDistinctValues() {
        UuidKeyGen generator = new UuidKeyGen();
        Set<UUID> values = new HashSet<>();

        for (int i = 0; i < 100; ++i) {
            values.add(generator.nextValue(FIXED_PREFIX, FIXED_INSTANT));
        }

        Assert.assertEquals(100, values.size());
    }

    private static String encodeUuidPayload(UUID uuid) {
        long msb = BaseKeyGen.reorder(uuid.getMostSignificantBits());
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(msb);
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().encodeToString(buffer.array()).substring(0, 22);
    }
}
