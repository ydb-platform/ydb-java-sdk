package tech.ydb.topic.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.impl.DisjointOffsetRangeSet;
import tech.ydb.topic.read.impl.OffsetsRangeImpl;

/**
 * @author Nikolay Perfilov
 */
public class DisjointOffsetRangeSetTest {

    private OffsetsRange range(long start, long end) {
        return new OffsetsRangeImpl(start, end);
    }

    @Test
    public void testRangesSimple() {
        DisjointOffsetRangeSet ranges = new DisjointOffsetRangeSet();
        ranges.add(Arrays.asList(range(0, 1), range(1, 2), range(3, 4)));
        List<OffsetsRange> rangesResult = ranges.getRangesAndClear();
        Assert.assertEquals(2, rangesResult.size());
        Assert.assertEquals(0, rangesResult.get(0).getStart());
        Assert.assertEquals(2, rangesResult.get(0).getEnd());
        Assert.assertEquals(3, rangesResult.get(1).getStart());
        Assert.assertEquals(4, rangesResult.get(1).getEnd());
    }

    private void assertException(ThrowingRunnable runnable, String addedRange, String existingRange) {
        RuntimeException ex = Assert.assertThrows(RuntimeException.class, runnable);
        String message = "Error adding new offset range. Added range " + addedRange +
                " clashes with existing range " + existingRange;
        Assert.assertEquals(message, ex.getMessage());
    }

    @Test
    public void testReuseRangeSet() {
        DisjointOffsetRangeSet ranges = new DisjointOffsetRangeSet();

        ranges.add(Arrays.asList(range(30, 40), range(10, 20), range(0, 9)));
        assertException(() -> ranges.add(Collections.singletonList(range(8, 11))), "[8,11)", "[0,9)");
        assertException(() -> ranges.add(Collections.singletonList(range(8, 10))), "[8,10)", "[0,9)");
        assertException(() -> ranges.add(Collections.singletonList(range(9, 11))), "[9,11)", "[10,20)");
        assertException(() -> ranges.add(Collections.singletonList(range(25, 31))), "[25,31)", "[30,40)");
        assertException(() -> ranges.add(Collections.singletonList(range(31, 100))), "[31,100)", "[30,40)");
        assertException(() -> ranges.add(Collections.singletonList(range(25, 100))), "[25,100)", "[30,40)");
        ranges.add(Collections.singletonList(range(9, 10)));
        List<OffsetsRange> firstResult = ranges.getRangesAndClear();
        Assert.assertEquals(2, firstResult.size());
        Assert.assertEquals(0, firstResult.get(0).getStart());
        Assert.assertEquals(20, firstResult.get(0).getEnd());
        Assert.assertEquals(30, firstResult.get(1).getStart());
        Assert.assertEquals(40, firstResult.get(1).getEnd());

        Assert.assertTrue(ranges.getRangesAndClear().isEmpty());

        ranges.add(Collections.singletonList(range(0, 9)));
        ranges.add(Collections.singletonList(range(10, 19)));
        ranges.add(Collections.singletonList(range(20, 30)));
        List<OffsetsRange> secondResult = ranges.getRangesAndClear();
        Assert.assertEquals(3, secondResult.size());
        Assert.assertEquals(0, secondResult.get(0).getStart());
        Assert.assertEquals(9, secondResult.get(0).getEnd());
        Assert.assertEquals(10, secondResult.get(1).getStart());
        Assert.assertEquals(19, secondResult.get(1).getEnd());
        Assert.assertEquals(20, secondResult.get(2).getStart());
        Assert.assertEquals(30, secondResult.get(2).getEnd());

        ranges.add(Collections.singletonList(range(39, 40)));
        ranges.add(Collections.singletonList(range(30, 39)));
        ranges.add(Collections.singletonList(range(40, 50)));
        List<OffsetsRange> thirdResult = ranges.getRangesAndClear();
        Assert.assertEquals(1, thirdResult.size());
        Assert.assertEquals(30, thirdResult.get(0).getStart());
        Assert.assertEquals(50, thirdResult.get(0).getEnd());

        Assert.assertTrue(ranges.getRangesAndClear().isEmpty());
    }
}
