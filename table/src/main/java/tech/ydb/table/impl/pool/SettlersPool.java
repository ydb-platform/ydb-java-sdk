package tech.ydb.table.impl.pool;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import tech.ydb.core.utils.Async;
import tech.ydb.table.Session;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Polovko
 */
public class SettlersPool<T> {
    private static final Logger logger = LoggerFactory.getLogger(SettlersPool.class);

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
        this.keepAliveTask.scheduleNext();
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
            logger.debug("Destroy {} because pool closed", po.object);
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
        private Timeout scheduledHandle = null;

        void stop() {
            stopped = true;
            if (scheduledHandle != null) {
                scheduledHandle.cancel();
                scheduledHandle = null;
            }
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
                scheduleNext();
                return;
            }

            PooledObject<T> po = it.next();
            if (po.incKeepAliveCount() > maxKeepAliveCount) {
                try {
                    it.remove();
                    size.decrementAndGet();
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "Destroy {} because {} keep alive iterations in settlers pool, max {}",
                            po.object, po.keepAliveCount, maxKeepAliveCount);
                    }
                    handler.destroy(po.object); // do not await object to be destroyed
                } catch (Exception ignore) {
                }
                checkNextObject(it);
                return;
            }

            handler.keepAlive(po.object)
                .whenCompleteAsync((result, throwable) -> {
                    try {
                        if (throwable != null) {
                            logger.warn("Keep alive for " + po.object + " failed", throwable);
                        } else {
                            if (result.isSuccess()) {
                                Session.State status = result.expect("cannot keep alive session: " + po.object);
                                if (status == Session.State.READY) {
                                    it.remove();
                                    size.decrementAndGet();
                                    mainPool.offerOrDestroy(po.object);
                                }
                            } else {
                                switch(result.getCode()) {
                                    case BAD_SESSION:
                                    case SESSION_BUSY:
                                    case INTERNAL_ERROR:
                                        it.remove();
                                        size.decrementAndGet();
                                        if (logger.isDebugEnabled()) {
                                            logger.debug(
                                                    "Destroy {} because keep alive got {} status code",
                                                    po.object, result.getCode());
                                        }
                                        handler.destroy(po.object); // do not await object to be destroyed
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    } catch (Exception ignore) {
                    }

                    checkNextObject(it);
                });
        }

        void scheduleNext() {
            scheduledHandle = Async.runAfter(this, keepAliveTimeMillis, TimeUnit.MILLISECONDS);
        }
    }
}
