package tech.ydb.table.impl.pool;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

import tech.ydb.table.utils.Async;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

/**
 * @author Sergey Polovko
 */
public class SettlersPool<T> {
    private static final Logger logger = Logger.getLogger(SettlersPool.class.getName());

    private final PooledObjectHandler<T> handler;
    private final FixedAsyncPool<T> mainPool;
    private final int maxKeepAliveCount;
    private final long keepAliveTimeMillis;

    private final ConcurrentLinkedDeque<PooledObject<T>> pool = new ConcurrentLinkedDeque<>();
    private final KeepAliveTask keepAliveTask = new KeepAliveTask();
    private final AtomicInteger size = new AtomicInteger(0);

    public SettlersPool(
        PooledObjectHandler<T> handler,
        FixedAsyncPool<T> mainPool,
        int maxKeepAliveCount,
        int keepAliveTimeMillis) {
        this.handler = handler;
        this.mainPool = mainPool;
        this.maxKeepAliveCount = maxKeepAliveCount;
        this.keepAliveTimeMillis = keepAliveTimeMillis;

        // delay first run
        Async.runAfter(keepAliveTask, keepAliveTimeMillis, TimeUnit.MILLISECONDS);
    }

    public boolean offerIfHaveSpace(T object) {
        final int s = size.incrementAndGet();
        if (s > mainPool.getMaxSize()) {
            size.decrementAndGet();
            return false;
        }
        pool.offerLast(new PooledObject<>(object));
        mainPool.fakeRelease();
        return true;
    }

    public void close() {
        keepAliveTask.stop();
        for (PooledObject<T> po : pool) {
            // avoid simultaneous session destruction
            logger.log(Level.FINE, "Destroy {0} because pool closed", po.object);
            handler.destroy(po.object).join();
        }
    }

    public int size() {
        return size.get();
    }

    /**
     * POOLED OBJECT
     */
    private static final class PooledObject<U> {
        private static final AtomicIntegerFieldUpdater<PooledObject> keepAliveCountUpdater =
            AtomicIntegerFieldUpdater.newUpdater(PooledObject.class, "keepAliveCount");

        private final U object;
        private volatile int keepAliveCount = 0;

        PooledObject(U object) {
            this.object = object;
        }

        int incKeepAliveCount() {
            return keepAliveCountUpdater.incrementAndGet(this);
        }
    }

    /**
     * KEEP ALIVE TASK
     */
    private final class KeepAliveTask implements TimerTask {
        private volatile boolean stopped = false;

        void stop() {
            stopped = true;
        }

        @Override
        public void run(Timeout timeout) {
            checkNextObject(pool.iterator());
        }

        private void checkNextObject(Iterator<PooledObject<T>> it) {
            if (stopped) {
                return;
            }

            if (!it.hasNext()) {
                Async.runAfter(keepAliveTask, keepAliveTimeMillis, TimeUnit.MILLISECONDS);
                return;
            }

            PooledObject<T> po = it.next();
            if (po.incKeepAliveCount() > maxKeepAliveCount) {
                try {
                    it.remove();
                    size.decrementAndGet();
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                            "Destroy {0} because {1} keep alive iterations in settlers pool, max {2}",
                            new Object[]{po.object, po.keepAliveCount, maxKeepAliveCount});
                    }
                    handler.destroy(po.object); // do not await object to be destroyed
                } catch (Exception ignore) {
                }
                checkNextObject(it);
                return;
            }

            handler.keepAlive(po.object)
                .whenCompleteAsync((ready, throwable) -> {

                    try {
                        if (throwable != null) {
                            logger.log(Level.WARNING, "Keep alive for " + po.object + " failed", throwable);
                        } else if (ready) {
                            it.remove();
                            size.decrementAndGet();
                            mainPool.offerOrDestroy(po.object);
                        }
                    } catch (Exception ignore) {
                    }

                    checkNextObject(it);
                });
        }
    }
}
