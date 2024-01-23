package tech.ydb.topic.impl;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.impl.DisjointOffsetRangeSet;
import tech.ydb.topic.read.impl.OffsetsRangeImpl;

/**
 * @author Nikolay Perfilov
 */
public class DisjointOffsetRangeSetTest {

    @Test
    public void testRangesSimple() {
        DisjointOffsetRangeSet ranges = new DisjointOffsetRangeSet();
        ranges.add(new OffsetsRangeImpl(0, 1));
        ranges.add(new OffsetsRangeImpl(1, 2));
        ranges.add(new OffsetsRangeImpl(3, 4));
        List<OffsetsRange> rangesResult = ranges.getRangesAndClear();
        Assert.assertEquals(2, rangesResult.size());
        Assert.assertEquals(0, rangesResult.get(0).getStart());
        Assert.assertEquals(2, rangesResult.get(0).getEnd());
        Assert.assertEquals(3, rangesResult.get(1).getStart());
        Assert.assertEquals(4, rangesResult.get(1).getEnd());
    }

    @Test
    public void testReuseRangeSet() {
        DisjointOffsetRangeSet ranges = new DisjointOffsetRangeSet();

        ranges.add(new OffsetsRangeImpl(30, 40));
        ranges.add(new OffsetsRangeImpl(10, 20));
        ranges.add(new OffsetsRangeImpl(0, 9));
        Assert.assertThrows(RuntimeException.class, () -> ranges.add(new OffsetsRangeImpl(8, 11)));
        Assert.assertThrows(RuntimeException.class, () -> ranges.add(new OffsetsRangeImpl(8, 10)));
        Assert.assertThrows(RuntimeException.class, () -> ranges.add(new OffsetsRangeImpl(9, 11)));
        Assert.assertThrows(RuntimeException.class, () -> ranges.add(new OffsetsRangeImpl(25, 31)));
        Assert.assertThrows(RuntimeException.class, () -> ranges.add(new OffsetsRangeImpl(31, 100)));
        Assert.assertThrows(RuntimeException.class, () -> ranges.add(new OffsetsRangeImpl(25, 100)));
        ranges.add(new OffsetsRangeImpl(9, 10));
        List<OffsetsRange> firstResult = ranges.getRangesAndClear();
        Assert.assertEquals(2, firstResult.size());
        Assert.assertEquals(0, firstResult.get(0).getStart());
        Assert.assertEquals(20, firstResult.get(0).getEnd());
        Assert.assertEquals(30, firstResult.get(1).getStart());
        Assert.assertEquals(40, firstResult.get(1).getEnd());

        Assert.assertTrue(ranges.getRangesAndClear().isEmpty());

        ranges.add(new OffsetsRangeImpl(0, 9));
        ranges.add(new OffsetsRangeImpl(10, 19));
        ranges.add(new OffsetsRangeImpl(20, 30));
        List<OffsetsRange> secondResult = ranges.getRangesAndClear();
        Assert.assertEquals(3, secondResult.size());
        Assert.assertEquals(0, secondResult.get(0).getStart());
        Assert.assertEquals(9, secondResult.get(0).getEnd());
        Assert.assertEquals(10, secondResult.get(1).getStart());
        Assert.assertEquals(19, secondResult.get(1).getEnd());
        Assert.assertEquals(20, secondResult.get(2).getStart());
        Assert.assertEquals(30, secondResult.get(2).getEnd());

        ranges.add(new OffsetsRangeImpl(39, 40));
        ranges.add(new OffsetsRangeImpl(30, 39));
        ranges.add(new OffsetsRangeImpl(40, 50));
        List<OffsetsRange> thirdResult = ranges.getRangesAndClear();
        Assert.assertEquals(1, thirdResult.size());
        Assert.assertEquals(30, thirdResult.get(0).getStart());
        Assert.assertEquals(50, thirdResult.get(0).getEnd());

        Assert.assertTrue(ranges.getRangesAndClear().isEmpty());
    }
}
