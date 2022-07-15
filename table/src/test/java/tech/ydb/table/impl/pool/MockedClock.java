package tech.ydb.table.impl.pool;

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

        public MockedInstant(Instant now) {
            reset(now);
        }

        private void reset(Instant now) {
            this.now = now;
        }

        private Instant instant() {
            return now;
        }
        
        private void goToFuture(Instant future) {
            if (future.isAfter(now)) {
                this.now = future;
            }
        }
    }
}
