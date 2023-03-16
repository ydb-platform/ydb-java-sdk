package tech.ydb.core.timer;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.base.Ticker;

/**
 * @author Kirill Kurdyukov
 */
public class TestTicker extends Ticker {
    private final Iterator<Integer> iterator;

    public TestTicker(Integer... pings) {
        this.iterator = Arrays.stream(pings).iterator();
    }

    @Override
    public long read() {
        return iterator.next();
    }
}
