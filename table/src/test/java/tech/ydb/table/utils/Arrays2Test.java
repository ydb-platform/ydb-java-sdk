package tech.ydb.table.utils;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class Arrays2Test {

    @Test
    public void isSorted() {
        Assert.assertTrue(Arrays2.isSorted(array()));
        Assert.assertTrue(Arrays2.isSorted(array(1)));
        Assert.assertTrue(Arrays2.isSorted(array(1, 2)));
        Assert.assertTrue(Arrays2.isSorted(array(1, 2, 3)));

        Assert.assertFalse(Arrays2.isSorted(array(2, 1)));
        Assert.assertFalse(Arrays2.isSorted(array(3, 2, 1)));
    }

    @Test
    public void sortBothByFirst() {
        // should not throw
        Arrays2.sortBothByFirst(array(), array());
        Arrays2.sortBothByFirst(array(1), array("a"));

        // keep sorted array untouched
        {
            Integer[] a = array(1, 2);
            String[] b = array("a", "b");

            Arrays2.sortBothByFirst(a, b);
            Assert.assertTrue(Arrays2.isSorted(a));
        }

        {
            Integer[] a = array(2, 1);
            String[] b = array("b", "a");

            Arrays2.sortBothByFirst(a, b);
            Assert.assertTrue(Arrays.toString(a), Arrays2.isSorted(a));
            Assert.assertArrayEquals(new Integer[] { 1, 2 }, a);
            Assert.assertArrayEquals(new String[] { "a", "b" }, b);
        }

        {
            Integer[] a = array(2, 1, 3);
            String[] b = array("b", "a", "c");

            Arrays2.sortBothByFirst(a, b);
            Assert.assertTrue(Arrays.toString(a), Arrays2.isSorted(a));
            Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, a);
            Assert.assertArrayEquals(new String[] { "a", "b", "c" }, b);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] array() {
        return (T[]) new Comparable[0];
    }

    // sugar
    @SuppressWarnings("unchecked")
    private static <T> T[] array(T... array) {
        return array;
    }
}
