package tech.ydb.table.impl.pool;

import java.time.Duration;

import io.netty.util.HashedWheelTimer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Sergey Polovko
 */
public class SettlersPoolTest {

    private final int keepAliveTimeMillis = 100;
    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final ResourceHandler handler = new ResourceHandler();

    private FixedAsyncPool<Resource> mainPool;
    private SettlersPool<Resource> settlersPool;

    @Before
    public void setUp() {
        mainPool = new FixedAsyncPool<>(handler, timer, 0, 2, 100, keepAliveTimeMillis, 30_000);
        assertEquals(0, mainPool.getAcquiredCount());

        settlersPool = new SettlersPool<>(handler, mainPool, timer, 1, keepAliveTimeMillis);
        assertEquals(0, settlersPool.size());
    }

    @Test
    public void offerAndRestore() throws InterruptedException {
        // (1) acquire resource as always
        Resource r = mainPool.acquire(Duration.ofMillis(10)).join();
        assertEquals(1, r.getId());
        assertEquals(1, mainPool.getAcquiredCount());

        // (2) return it to settlers pool instead of main pool
        assertTrue(settlersPool.offerIfHaveSpace(r));
        assertEquals(1, settlersPool.size());
        assertEquals(0, mainPool.getAcquiredCount());

        // (3) after successful keep alive resource must be returned into main pool
        Thread.sleep(keepAliveTimeMillis * 10);
        assertEquals(0, settlersPool.size());

        Resource r1 = mainPool.acquire(Duration.ofMillis(10)).join();
        assertSame(r, r1);
    }

    @Test
    public void offerIfHaveSpace() {
        Resource r1 = handler.create(0).join();
        Resource r2 = handler.create(0).join();
        Resource r3 = handler.create(0).join();
        assertTrue(settlersPool.offerIfHaveSpace(r1));
        assertTrue(settlersPool.offerIfHaveSpace(r2));
        assertFalse(settlersPool.offerIfHaveSpace(r3));
    }
}
