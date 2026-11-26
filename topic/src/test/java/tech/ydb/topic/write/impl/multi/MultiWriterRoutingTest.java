package tech.ydb.topic.write.impl.multi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;

public class MultiWriterRoutingTest {
    @Test
    public void hashesMatchReferenceVectors() {
        // Columns: raw input hex, Murmur2Hash32(seed 0x9747b28c), Murmur2Hash64A(seed 0).
        String[][] vectors = {
            {"", "106e08d9", "0000000000000000"},
            {"61", "a2d0b27c", "071717d2d36b6b11"},
            {"6162", "12d8262a", "62be85b2fe53d1f8"},
            {"616263", "1c94221b", "9cc9c33498a95efb"},
            {"61626364", "b11ab5f4", "ec1044c45cc5097a"},
            {"6162636465", "1b897edd", "1182974836d6dbb7"},
            {"616263646566", "6f7fdafc", "b78e3425fc996779"},
            {"61626364656667", "eb595499", "241aa52b0a62005d"},
            {"6162636465666768", "c70d55dd", "afdb0257ff41aa98"},
            {"616263646566676869", "db106fbe", "c9b9d84356146ac2"},
            {"68656c6c6f", "7f1ddbbd", "1e68d17c457bf117"},
            {"d0bad0bbd18ed187", "7e806270", "8cfe416fc44d80c4"},
            {"f09f9a80", "2d67d1c1", "1670199edc5ecaa3"},
            {"610062", "fed14e49", "f24435ae2d6f1cac"},
            {"80ff007f", "fcb12a3c", "da44aab3968cfbbe"},
            {"70726f64756365722d707265666978", "15dc366d", "4f45737d1188fc48"}
        };
        for (String[] vector : vectors) {
            byte[] input = hex(vector[0]);
            Assert.assertEquals(vector[0], Integer.parseUnsignedInt(vector[1], 16), KeyHash.murmur32(input));
            Assert.assertEquals(vector[0], Long.parseUnsignedLong(vector[2], 16), KeyHash.murmur64(input));
        }
    }

    @Test
    public void stringKeysUseUtf8AndNetworkByteOrder() {
        Assert.assertArrayEquals(hex("8cfe416fc44d80c4"), KeyHash.boundKey("ключ"));
        Assert.assertArrayEquals(hex("1670199edc5ecaa3"), KeyHash.boundKey("🚀"));
        Assert.assertArrayEquals(hex("f24435ae2d6f1cac"), KeyHash.boundKey("a\0b"));
        Assert.assertArrayEquals(new byte[8], KeyHash.boundKey(""));
        Assert.assertEquals(0x7ed14e49, KeyHash.kafkaHash("a\0b"));
    }

    @Test
    public void kafkaRoutingSortsPartitionIdsAndMasksSignBit() {
        Topology topology = fixed(RoutingMode.HASH, plain(90), plain(2), plain(30));
        Assert.assertEquals(30, topology.route("a", null));
        Assert.assertEquals(2, topology.route("abc", null));
        Assert.assertEquals(90, topology.route("ключ", null));
        Assert.assertNull(topology.partitionKey("a"));
        Assert.assertThrows(IllegalArgumentException.class, () -> topology.route("a", 2L));
    }

    @Test
    public void boundRoutingUsesUnsignedInclusiveLowerExclusiveUpper() {
        Topology topology = split();
        Assert.assertEquals(1, topology.routeBound(hex("7fffffffffffffff")));
        Assert.assertEquals(2, topology.routeBound(hex("8000000000000000")));
        Assert.assertEquals(2, topology.routeBound(hex("ffffffffffffffff")));
        Assert.assertEquals(1, topology.route("a", null));
        Assert.assertEquals(2, topology.route("ключ", null));
        Assert.assertArrayEquals(hex("8cfe416fc44d80c4"), topology.partitionKey("ключ"));
        Assert.assertTrue(Topology.compareUnsigned(hex("80"), hex("7f")) > 0);
        Assert.assertTrue(Topology.compareUnsigned(hex("0000"), hex("00")) > 0);
    }

    @Test
    public void autoUsesTopicStrategyAndExplicitRanges() {
        PartitionInfo unranged = plain(0);
        Assert.assertEquals(RoutingMode.HASH, fixed(RoutingMode.AUTO, unranged).getRoutingMode());
        Assert.assertEquals(RoutingMode.BOUND, fixed(RoutingMode.AUTO, bounded(0, "", "")).getRoutingMode());
        for (AutoPartitioningStrategy strategy : Arrays.asList(AutoPartitioningStrategy.SCALE_UP,
                AutoPartitioningStrategy.SCALE_UP_AND_DOWN, AutoPartitioningStrategy.PAUSED)) {
            Topology initial = new Topology(Collections.singletonList(unranged), RoutingMode.AUTO, strategy);
            Assert.assertEquals(strategy.name(), RoutingMode.BOUND, initial.getRoutingMode());
            Assert.assertEquals(0, initial.route("ключ", null));
            Assert.assertArrayEquals(hex("8cfe416fc44d80c4"), initial.partitionKey("ключ"));
        }
        Assert.assertThrows(IllegalArgumentException.class, () -> fixed(RoutingMode.BOUND, unranged));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> fixed(RoutingMode.HASH, bounded(0, "", "")));
    }

    @Test
    public void firstSplitAcceptsBothEncodingsOfTheWholeAutoRoot() {
        Topology initial = auto(plain(0));
        Topology describedWithRange = auto(bounded(0, "", ""));
        Assert.assertTrue(describedWithRange.sameLayout(initial));
        describedWithRange.validateRefresh(initial);
        initial.validateRefresh(describedWithRange);
        PartitionInfo left = node(1, true, "", "8000000000000000", longs(0), longs());
        PartitionInfo right = node(2, true, "8000000000000000", "", longs(0), longs());
        for (PartitionInfo root : Arrays.asList(node(0, false, null, null, longs(), longs(1, 2)),
                node(0, false, "", "", longs(), longs(1, 2)))) {
            Topology successor = auto(root, left, right);
            successor.validateRefresh(initial);
            Assert.assertEquals(RoutingMode.BOUND, successor.getRoutingMode());
            Assert.assertEquals(2, successor.route("ключ", null));
            Assert.assertEquals(1, successor.route("a", null));
        }
    }

    @Test
    public void absentRangeCannotStandInForANonRootOrAnIncompleteSplit() {
        PartitionInfo root = node(0, false, null, null, longs(), longs(1, 2));
        PartitionInfo left = node(1, true, "", "80", longs(0), longs());
        Assert.assertThrows(IllegalArgumentException.class, () -> auto(
                root, left, node(2, true, null, null, longs(0), longs())));
        Assert.assertThrows(IllegalArgumentException.class, () -> auto(
                root, left, node(2, true, "90", "", longs(0), longs())));
        Assert.assertThrows(IllegalArgumentException.class, () -> auto(plain(0), plain(1)));
        Assert.assertThrows(IllegalArgumentException.class, () -> auto(root, left));
    }

    @Test
    public void explicitPartitionDoesNotGuessChildAfterSplit() {
        Topology topology = new Topology(new ArrayList<>(split().getPartitions().values()),
                RoutingMode.EXPLICIT, AutoPartitioningStrategy.DISABLED);
        Assert.assertEquals(2, topology.route("", 2L));
        Assert.assertThrows(IllegalArgumentException.class, () -> topology.route("", 0L));
        Assert.assertThrows(IllegalArgumentException.class, () -> topology.route("", 3L));
        Assert.assertThrows(IllegalArgumentException.class, () -> topology.route("", null));
    }

    @Test
    public void rejectsGapsOverlapsAndMissingEndpoints() {
        assertInvalid(bounded(0, "", "40"), bounded(1, "80", ""));
        assertInvalid(bounded(0, "", "c0"), bounded(1, "80", ""));
        assertInvalid(bounded(0, "01", ""));
        assertInvalid(bounded(0, "", "ff"));
        assertInvalid(bounded(0, "", ""), bounded(1, "80", ""));
        assertInvalid(bounded(0, "", "80"), bounded(1, "80", "80"), bounded(2, "80", ""));
    }

    @Test
    public void rejectsMixedBoundsAndMissingActivePartitions() {
        assertInvalid(bounded(0, "", "80"), plain(1));
        assertInvalid();
    }

    @Test
    public void rejectsDuplicateAndNegativePartitionIds() {
        assertInvalid(plain(0), plain(0));
        assertInvalid(plain(-1));
    }

    @Test
    public void rejectsMissingAndNonreciprocalGraphEdges() {
        assertInvalid(node(0, false, "", "", longs(), longs(1)), bounded(2, "", ""));
        assertInvalid(node(0, false, "", "", longs(), longs(1)), bounded(1, "", ""));
        assertInvalid(node(1, true, "", "", longs(0), longs()));
        assertInvalid(node(0, false, "", "", longs(), longs()), bounded(1, "", ""));
    }

    @Test
    public void rejectsActiveParentAndRepeatedEdges() {
        assertInvalid(node(0, true, "", "", longs(), longs(1)), node(1, true, "", "", longs(0), longs()));
        assertInvalid(node(0, false, "", "", longs(), longs(1, 1)), node(1, true, "", "", longs(0), longs()));
    }

    @Test
    public void rejectsCyclesAndChildOutsideParentRange() {
        assertInvalid(node(0, false, "", "", longs(1), longs(1)),
                node(1, false, "", "", longs(0), longs(0)), bounded(2, "", ""));
        assertInvalid(node(0, false, "", "80", longs(), longs(1)), node(1, true, "", "", longs(0), longs()));
    }

    @Test
    public void rejectsIncompleteChildCoverageDespiteGlobalActiveCoverage() {
        assertInvalid(node(0, false, "", "", longs(), longs(1)),
                node(1, true, "", "80", longs(0), longs()), bounded(2, "80", ""));
    }

    @Test
    public void cascadeAncestorsAreDeterministicAndIncludeSelf() {
        Topology topology = cascade();
        Assert.assertEquals(Arrays.asList(3L, 1L, 0L), topology.ancestors(3));
        Assert.assertEquals(Arrays.asList(2L, 0L), topology.ancestors(2));
        Assert.assertTrue(topology.isDescendant(3, 0));
        Assert.assertTrue(topology.isDescendant(3, 3));
        Assert.assertFalse(topology.isDescendant(2, 1));
        Assert.assertFalse(topology.isDescendant(99, 0));
        Assert.assertThrows(UnsupportedOperationException.class, () -> topology.getPartitions().clear());
        Assert.assertThrows(UnsupportedOperationException.class, () -> topology.ancestors(3).clear());
    }

    @Test
    public void acceptsSplitAndCascadeRefresh() {
        Topology original = fixed(RoutingMode.BOUND, bounded(0, "", ""));
        split().validateRefresh(original);
        cascade().validateRefresh(split());
        cascade().validateRefresh(original);
        Assert.assertFalse(original.sameLayout(split()));
        Assert.assertTrue(split().sameLayout(fixed(RoutingMode.BOUND,
                node(2, true, "8000000000000000", "", longs(0), longs()),
                node(0, false, "", "", longs(), longs(2, 1)),
                node(1, true, "", "8000000000000000", longs(0), longs()))));
    }

    @Test
    public void rejectsChangedHashPartitionSetAndAlgorithm() {
        Topology original = fixed(RoutingMode.HASH, plain(0), plain(1));
        Topology changed = fixed(RoutingMode.HASH, plain(0), plain(1), plain(2));
        Assert.assertThrows(IllegalArgumentException.class, () -> changed.validateRefresh(original));
        Topology bound = fixed(RoutingMode.BOUND, bounded(0, "", ""));
        Topology hash = fixed(RoutingMode.HASH, plain(0));
        Assert.assertThrows(IllegalArgumentException.class, () -> bound.validateRefresh(hash));
    }

    @Test
    public void rejectsDisappearingNodesAndReactivation() {
        Topology original = fixed(RoutingMode.BOUND, bounded(0, "", ""));
        Assert.assertThrows(IllegalArgumentException.class, () -> original.validateRefresh(split()));
        Topology replacement = fixed(RoutingMode.BOUND, bounded(99, "", ""));
        Assert.assertThrows(IllegalArgumentException.class, () -> replacement.validateRefresh(original));
        Topology previous = split();
        Topology changed = fixed(RoutingMode.BOUND, bounded(0, "", "8000000000000000"),
                bounded(1, "8000000000000000", "c0"), bounded(2, "c0", ""));
        Assert.assertThrows(IllegalArgumentException.class, () -> changed.validateRefresh(previous));
    }

    @Test
    public void rejectsChangedBoundsWithoutSplitLineage() {
        Topology original = fixed(RoutingMode.BOUND, bounded(0, "", "80"), bounded(1, "80", ""));
        Topology shifted = fixed(RoutingMode.BOUND, bounded(0, "", "90"), bounded(1, "90", ""));
        Assert.assertThrows(IllegalArgumentException.class, () -> shifted.validateRefresh(original));
        Assert.assertEquals(1, original.routeBound(hex("80")));
    }

    @Test
    public void boundKeyReturnsIndependentBytes() {
        Topology topology = split();
        byte[] partitionKey = topology.partitionKey("ключ");
        Arrays.fill(partitionKey, (byte) 0);
        Assert.assertArrayEquals(hex("8cfe416fc44d80c4"), topology.partitionKey("ключ"));
        Assert.assertEquals(2, topology.route(new String(hex("d0bad0bbd18ed187"), StandardCharsets.UTF_8), null));
    }

    private static Topology split() {
        return fixed(RoutingMode.BOUND, node(0, false, "", "", longs(), longs(1, 2)),
                node(1, true, "", "8000000000000000", longs(0), longs()),
                node(2, true, "8000000000000000", "", longs(0), longs()));
    }

    private static Topology cascade() {
        return fixed(RoutingMode.BOUND, node(0, false, "", "", longs(), longs(1, 2)),
                node(1, false, "", "8000000000000000", longs(0), longs(3, 4)),
                node(2, true, "8000000000000000", "", longs(0), longs()),
                node(3, true, "", "4000000000000000", longs(1), longs()),
                node(4, true, "4000000000000000", "8000000000000000", longs(1), longs()));
    }

    private static void assertInvalid(PartitionInfo... partitions) {
        Assert.assertThrows(IllegalArgumentException.class, () -> fixed(RoutingMode.AUTO, partitions));
    }

    private static Topology fixed(RoutingMode mode, PartitionInfo... partitions) {
        return new Topology(Arrays.asList(partitions), mode, AutoPartitioningStrategy.DISABLED);
    }

    private static Topology auto(PartitionInfo... partitions) {
        return new Topology(Arrays.asList(partitions), RoutingMode.AUTO, AutoPartitioningStrategy.SCALE_UP);
    }

    private static PartitionInfo plain(long id) {
        return node(id, true, null, null, longs(), longs());
    }

    private static PartitionInfo bounded(long id, String from, String to) {
        return node(id, true, from, to, longs(), longs());
    }

    private static PartitionInfo node(long id, boolean active, String from, String to,
            List<Long> parents, List<Long> children) {
        YdbTopic.DescribeTopicResult.PartitionInfo.Builder info = YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder()
                .setPartitionId(id).setActive(active)
                .addAllParentPartitionIds(parents).addAllChildPartitionIds(children);
        if (from != null) {
            info.setKeyRange(YdbTopic.PartitionKeyRange.newBuilder()
                    .setFromBound(ByteString.copyFrom(hex(from))).setToBound(ByteString.copyFrom(hex(to))));
        }
        return new PartitionInfo(info.build());
    }

    private static List<Long> longs(long... ids) {
        List<Long> result = new ArrayList<>();
        for (long id : ids) {
            result.add(id);
        }
        return result;
    }

    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(value.substring(index * 2, index * 2 + 2), 16);
        }
        return bytes;
    }
}
