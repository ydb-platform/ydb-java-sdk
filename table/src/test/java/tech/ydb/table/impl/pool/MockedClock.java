package tech.ydb.table.impl.pool;

import com.google.common.truth.Truth;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedClock extends Clock {
    private final ZoneId zoneID;
    private final MockedInstant mock;
    
    private MockedClock(ZoneId zoneId, MockedInstant instant) {
        this.zoneID = zoneId;
        this.mock = instant;
    }
    
    @Override
    public ZoneId getZone() {
        return zoneID;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MockedClock(zone, mock);
    }

    @Override
    public Instant instant() {
        return mock.instant();
    }
    
    public void goToFuture(Instant future) {
        mock.goToFuture(future);
    }
    
    public void reset(Instant now) {
        mock.reset(now);
    }
    
    public static MockedClock create(ZoneId zoneId) {
        return new MockedClock(zoneId, new MockedInstant(Instant.now()));
    }
    
    private static class MockedInstant {
        private volatile Instant now;
        private volatile long millis;

        public MockedInstant(Instant now) {
            reset(now);
        }

        private void reset(Instant now) {
            this.now = now;
            this.millis = System.currentTimeMillis();
        }

        private Instant instant() {
            long diff = System.currentTimeMillis() - millis;
            return now.plusMillis(diff);
        }
        
        private void goToFuture(Instant future) {
            Truth.assertThat(future).isGreaterThan(now);
            this.now = future;
            this.millis = System.currentTimeMillis();
        }
    }
}
