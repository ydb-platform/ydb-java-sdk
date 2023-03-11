package tech.ydb.core.utils;

/**
 * Wrapper System.nanoTime(). For use mockito clock - pattern.
 * MockitoException when trying to mock java.lang.System.
 *
 * @author Kirill Kurdyukov
 */
public class Timer {

    private Timer() {
    }

    public static long nanoTime() {
        return System.nanoTime();
    }
}
