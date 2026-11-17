package tech.ydb.coordination;

import java.time.Duration;
import java.util.function.BooleanSupplier;

public class AwaitAssert {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(50);

    private Duration timeout = DEFAULT_TIMEOUT;
    private Duration pollInterval = DEFAULT_POLL_INTERVAL;

    public static AwaitAssert await() {
        return new AwaitAssert();
    }

    public AwaitAssert timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public AwaitAssert pollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
        return this;
    }

    public void until(BooleanSupplier condition) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return;
            }

            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Await interrupted", e);
            }
        }

        throw new RuntimeException("Condition not met within " + timeout);
    }
}

