package ru.yandex.ydb.table.utils;

/**
 * @author Sergey Polovko
 */
public final class Arrays2 {
    private Arrays2() {}

    /**
     * Check if array is sorted.
     *
     * @return {@code true} iff array sorted
     */
    public static <T extends Comparable<T>> boolean isSorted(T[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1].compareTo(a[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * In-place sort both arrays using comparability of elements of the first array.
     */
    public static <T extends Comparable<T>, U> void sortBothByFirst(T[] first, U[] second) {
        assert first.length == second.length;

        // Insertion sort, because almost always we will work with sorted arrays and in this case
        // it has O(n) complexity.
        //
        // Here used improvement version of insertion sort: larger entries are moved to the right
        // one position rather than doing full exchanges (thus cutting the number of array accesses
        // in half).
        //
        // See Algorithms, 4th Edition by Robert Sedgewick and Kevin Wayne

        for (int i = 1; i < first.length; i++) {
            T firstIth = first[i];
            U secondsIth = second[i];

            int j = i;
            for (; j > 0 && first[j - 1].compareTo(firstIth) > 0; j--) {
                first[j] = first[j - 1];
                second[j] = second[j - 1];
            }

            if (i != j) {
                first[j] = firstIth;
                second[j] = secondsIth;
            }
        }
    }
}
