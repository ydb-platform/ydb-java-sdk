package tech.ydb.core.keygen;

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
 * Correctness tests for {@link TextKeyGen}.
 */
public class TextKeyGenTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2024-06-15T12:34:56Z");
    private static final LocalDate FIXED_DATE = LocalDate.of(2024, 6, 15);
    private static final long FIXED_PREFIX = 0x2A5L;

    @Test
    public void defaultConstructorUsesTenPrefixBits() {
        TextKeyGen generator = new TextKeyGen();
        Assert.assertEquals(10, generator.getPrefixBits());
        Assert.assertEquals(0xFFC0000000000000L, generator.getPrefixMask());
    }

    @Test
    public void customPrefixBitsExposeMask() {
        TextKeyGen oneBit = new TextKeyGen(1);
        TextKeyGen eighteenBit = new TextKeyGen(18);

        Assert.assertEquals(1, oneBit.getPrefixBits());
        Assert.assertEquals(0x8000000000000000L, oneBit.getPrefixMask());
        Assert.assertEquals(18, eighteenBit.getPrefixBits());
        Assert.assertEquals(0xFFFFC00000000000L, eighteenBit.getPrefixMask());
    }

    @Test
    public void unsupportedPrefixLengthIsRejected() {
        try {
            new TextKeyGen(0);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported prefix length: 0", ex.getMessage());
        }

        try {
            new TextKeyGen(19);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported prefix length: 19", ex.getMessage());
        }
    }

    @Test
    public void nextPrefixProducesDistinctValues() {
        TextKeyGen generator = new TextKeyGen(12);
        Set<Long> seen = new HashSet<>();

        for (int i = 0; i < 100; ++i) {
            seen.add(generator.nextPrefix());
        }

        Assert.assertTrue("Expected multiple distinct prefixes", seen.size() > 1);
    }

    @Test
    public void fixedPrefixIsMaskedWhenEmbedded() {
        TextKeyGen generator = new TextKeyGen(12);
        long rawPrefix = 0x123456789ABCDEFL;

        String key = generator.nextValue(rawPrefix, FIXED_INSTANT);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);

        Assert.assertEquals(rawPrefix & generator.getPrefixMask(),
                KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
    }

    @Test
    public void nextValueProducesTwentyTwoCharUrlSafeBase64() {
        TextKeyGen generator = new TextKeyGen();
        String key = generator.nextValue();

        Assert.assertEquals(22, key.length());
        Assert.assertFalse(key.contains("="));
        Assert.assertFalse(key.contains("+"));
        Assert.assertFalse(key.contains("/"));
        Base64.getUrlDecoder().decode(key + "==");
    }

    @Test
    public void nextValueEmbedsFixedPrefixAndTimestamp() {
        TextKeyGen generator = new TextKeyGen(10);

        String key = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);

        Assert.assertEquals(FIXED_PREFIX & generator.getPrefixMask(),
                KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        Assert.assertEquals(BaseKeyGen.getTimestampCode(FIXED_INSTANT),
                KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits()));
    }

    @Test
    public void nextValueWithLocalDateUsesUtcMidnight() {
        TextKeyGen generator = new TextKeyGen(10);
        Instant expectedInstant = FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant();

        String key = generator.nextValue(FIXED_PREFIX, FIXED_DATE);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);

        Assert.assertEquals(BaseKeyGen.getTimestampCode(expectedInstant),
                KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits()));
    }

    @Test
    public void nextValueWithRandomPrefixPreservesRandomPrefixBits() {
        TextKeyGen generator = new TextKeyGen(10);
        Set<Long> prefixes = new HashSet<>();

        for (int i = 0; i < 50; ++i) {
            String key = generator.nextValue(FIXED_INSTANT);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);
            prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        }

        Assert.assertTrue("Expected varying random prefixes", prefixes.size() > 1);
    }

    @Test
    public void nextValueWithPrefixOnlyUsesCurrentSecond() {
        TextKeyGen generator = new TextKeyGen(10);
        int before = BaseKeyGen.getTimestampCode(Instant.now());

        String key = generator.nextValue(FIXED_PREFIX);
        long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);
        int embedded = KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits());
        int after = BaseKeyGen.getTimestampCode(Instant.now());

        Assert.assertEquals(FIXED_PREFIX & generator.getPrefixMask(),
                KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        Assert.assertTrue(embedded >= before && embedded <= after);
    }

    @Test
    public void nextValueWithDateOnlyUsesUtcMidnightAndRandomPrefix() {
        TextKeyGen generator = new TextKeyGen(8);
        Instant expectedInstant = FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant();
        Set<Long> prefixes = new HashSet<>();

        for (int i = 0; i < 20; ++i) {
            String key = generator.nextValue(FIXED_DATE);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);
            prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
            Assert.assertEquals(BaseKeyGen.getTimestampCode(expectedInstant),
                    KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits()));
        }

        Assert.assertTrue("Expected varying random prefixes", prefixes.size() > 1);
    }

    @Test
    public void nextValueWithoutArgumentsUsesRandomPrefixAndCurrentSecond() {
        TextKeyGen generator = new TextKeyGen(10);
        int before = BaseKeyGen.getTimestampCode(Instant.now());
        Set<Long> prefixes = new HashSet<>();

        String key = generator.nextValue();
        long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);
        int embedded = KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, generator.getPrefixBits());
        int after = BaseKeyGen.getTimestampCode(Instant.now());

        prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
        for (int i = 0; i < 20; ++i) {
            prefixes.add(KeyGenTestSupport.extractEmbeddedPrefix(
                    KeyGenTestSupport.logicalMsbFromTextKey(generator.nextValue()), generator));
        }

        Assert.assertTrue(embedded >= before && embedded <= after);
        Assert.assertTrue("Expected varying random prefixes", prefixes.size() > 1);
    }

    @Test
    public void nextValueDoesNotForceUuidVersionBits() {
        TextKeyGen textGenerator = new TextKeyGen(10);
        UuidKeyGen uuidGenerator = new UuidKeyGen(10);
        Set<Integer> textVersions = new HashSet<>();

        for (int i = 0; i < 50; ++i) {
            String key = textGenerator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
            textVersions.add(KeyGenTestSupport.logicalVersionNibble(
                    KeyGenTestSupport.logicalMsbFromTextKey(key)));

            UUID uuid = uuidGenerator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
            Assert.assertEquals(8, KeyGenTestSupport.logicalVersionNibble(
                    KeyGenTestSupport.logicalMsbFromUuid(uuid)));
        }

        Assert.assertTrue("Text keys should not all share version nibble 8", textVersions.size() > 1);
    }

    @Test
    public void nextValueWithSamePrefixChangesRandomSuffix() {
        TextKeyGen generator = new TextKeyGen(10);

        String first = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
        String second = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);

        Assert.assertNotEquals(first, second);
        Assert.assertNotEquals(KeyGenTestSupport.logicalLsbFromTextKey(first),
                KeyGenTestSupport.logicalLsbFromTextKey(second));
        Assert.assertEquals(
                KeyGenTestSupport.extractEmbeddedPrefix(
                        KeyGenTestSupport.logicalMsbFromTextKey(first), generator),
                KeyGenTestSupport.extractEmbeddedPrefix(
                        KeyGenTestSupport.logicalMsbFromTextKey(second), generator));
    }

    @Test
    public void generatedTextKeyMatchesBaseKeyGenToStringForEquivalentUuid() {
        TextKeyGen generator = new TextKeyGen(10);

        for (int i = 0; i < 20; ++i) {
            String key = generator.nextValue(FIXED_PREFIX, FIXED_INSTANT);
            Assert.assertEquals(BaseKeyGen.toString(KeyGenTestSupport.uuidFromTextKey(key)), key);
        }
    }

    @Test
    public void prefixAndTimestampLayoutsWorkForEdgePrefixSizes() {
        for (int prefixBits : new int[]{1, 18}) {
            TextKeyGen generator = new TextKeyGen(prefixBits);

            String key = generator.nextValue(0x123456789ABCDEFL, FIXED_INSTANT);
            long logicalMsb = KeyGenTestSupport.logicalMsbFromTextKey(key);

            Assert.assertEquals(0x123456789ABCDEFL & generator.getPrefixMask(),
                    KeyGenTestSupport.extractEmbeddedPrefix(logicalMsb, generator));
            Assert.assertEquals(BaseKeyGen.getTimestampCode(FIXED_INSTANT),
                    KeyGenTestSupport.extractEmbeddedTimestampCode(logicalMsb, prefixBits));
        }
    }

    @Test
    public void distinctCallsProduceDistinctKeys() {
        TextKeyGen generator = new TextKeyGen();
        Set<String> keys = new HashSet<>();

        for (int i = 0; i < 100; ++i) {
            keys.add(generator.nextValue(FIXED_PREFIX, FIXED_INSTANT));
        }

        Assert.assertEquals(100, keys.size());
    }
}
