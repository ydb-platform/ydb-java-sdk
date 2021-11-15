package tech.ydb.table.impl.pool;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.core.utils.Async;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 */
public final class FixedAsyncPool<T> implements AsyncPool<T> {
    private static final Logger logger = LoggerFactory.getLogger(FixedAsyncPool.class);
    private static final Logger keepAliveTaskLogger = LoggerFactory.getLogger(FixedAsyncPool.KeepAliveTask.class);

    private final Deque<PooledObject<T>> objects = new LinkedList<>();
    private final AtomicInteger acquiredObjectsCount = new AtomicInteger(0);
    private final Queue<PendingAcquireTask> pendingAcquireTasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingAcquireCount = new AtomicInteger(0);
    private final PooledObjectHandler<T> handler;
    private final KeepAliveTask keepAliveTask;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final int minSize;
    private final int maxSize;
    private final int waitQueueMaxSize;

    public FixedAsyncPool(
        PooledObjectHandler<T> handler,
        int minSize,
        int maxSize,
        int waitQueueMaxSize,
        long keepAliveTimeMillis,
        long maxIdleTimeMillis) {
        this.handler = handler;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.waitQueueMaxSize = waitQueueMaxSize;

        // start keep alive task
        int keepAliveBatchSize = Math.max(2, maxSize / 10);
        this.keepAliveTask = new KeepAliveTask(keepAliveTimeMillis, maxIdleTimeMillis, keepAliveBatchSize);
        this.keepAliveTask.scheduleNext();
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int getAcquiredCount() {
        return acquiredObjectsCount.get();
    }

    @Override
    public int getIdleCount() {
        synchronized (objects) {
            return objects.size();
        }
    }

    @Override
    public int getPendingAcquireCount() {
        return pendingAcquireCount.get();
    }

    @Override
    public CompletableFuture<T> acquire(Duration timeout) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        try {
            if (closed.get()) {
                promise.completeExceptionally(new IllegalStateException("pool was closed"));
                return promise;
            }

            final long timeoutNanos = timeout.toNanos();
            final long deadlineAfter = System.nanoTime() + timeoutNanos;

            int count = acquiredObjectsCount.get();
            while (count < maxSize) {
                if (!acquiredObjectsCount.compareAndSet(count, count + 1)) {
                    count = acquiredObjectsCount.get();
                    continue;
                }
                assert count >= 0;
                doAcquireOrCreate(promise, deadlineAfter);
                logger.debug("Acquiring object, current acquired objects count: {}", acquiredObjectsCount.get());
                return promise;
            }

            if (timeoutNanos <= 0) {
                promise.completeExceptionally(new IllegalStateException("too many acquired objects"));
            } else {
                if (pendingAcquireCount.getAndIncrement() < waitQueueMaxSize) {
                    pendingAcquireTasks.offer(new PendingAcquireTask(promise, timeoutNanos, deadlineAfter));
                    runPendingAcquireTasks();
                } else {
                    pendingAcquireCount.decrementAndGet();
                    promise.completeExceptionally(new IllegalStateException("too many outstanding acquire operations"));
                }
                logger.debug("Acquire: current pending acquire count: {}", pendingAcquireCount.get());
            }
        } catch (Throwable cause) {
            promise.completeExceptionally(cause);
        }
        return promise;
    }

    @Override
    public void release(T object) {
        if (closed.get()) {
            // Since the pool is closed, we have no choice but to close the channel
            logger.debug("Destroy {} because pool already closed", object);
            handler.destroy(object);
            throw new IllegalStateException("pool was closed");
        }

        if (handler.isValid(object)) {
            if (tryToMoveObjectToPendingTask(object)) {
                // fast way for pending task to meet its requirements (a.k.a. randevouze)
                return;
            }
            offerObject(new PooledObject<>(object, System.currentTimeMillis()));
            acquiredObjectsCount.decrementAndGet();
        } else {
            acquiredObjectsCount.decrementAndGet();
            logger.debug("Destroy {} because invalid state", object);
            handler.destroy(object);
        }

        logger.debug("Object released, current acquired objects count: {}", acquiredObjectsCount.get());

        runPendingAcquireTasks();
    }

    void fakeRelease() {
        acquiredObjectsCount.decrementAndGet();
    }

    void offerOrDestroy(T object) {
        if (closed.get()) {
            // Since the pool is closed, we have no choice but to close the channel
            logger.debug("Destroy {} because pool already closed", object);
            handler.destroy(object);
            throw new IllegalStateException("pool was closed");
        }

        // create wrapper outside from synchronized block
        PooledObject<T> po = new PooledObject<>(object, System.currentTimeMillis());
        synchronized (objects) {
            if (acquiredObjectsCount.get() + objects.size() < maxSize) {
                objects.offerLast(po);
                return;
            }
        }
        logger.debug("Destroy {} because max pool size already reached", object);
        handler.destroy(object);
    }

    private PooledObject<T> pollObject() {
        synchronized (objects) {
            return objects.pollLast();
        }
    }

    private void offerObject(PooledObject<T> object) {
        synchronized (objects) {
            objects.offerLast(object);
        }
    }

    private void doAcquireOrCreate(CompletableFuture<T> promise, long deadlineAfter) {
        assert acquiredObjectsCount.get() > 0;
        try {
            final PooledObject<T> object = pollObject();
            if (object != null) {
                onAcquire(promise, object.getValue(), null);
                return;
            }

            // no objects left in the pool, so create new one

            CompletableFuture<T> future = handler.create(deadlineAfter)
                .thenApply(o -> {
                    logger.debug("Created {}", o);
                    return o;
                });
            if (future.isDone() && !future.isCompletedExceptionally()) {
                // faster than subscribing on future
                onAcquire(promise, future.getNow(null), null);
                return;
            }

            future.whenCompleteAsync((o, ex) -> {
                if (ex != null) {
                    acquiredObjectsCount.decrementAndGet();
                    onAcquire(promise, null, ex);
                } else if (!promise.complete(o)) {
                    // do not leak object if promise already completed
                    release(o);
                }
            });
        } catch (Throwable t) {
            acquiredObjectsCount.decrementAndGet();
            onAcquire(promise, null, t);
        }
    }

    private void onAcquire(CompletableFuture<T> promise, T object, Throwable error) {
        // one of them must be null
        assert (object == null) != (error == null);

        if (closed.get()) {
            // destroy object because pool was already closed
            if (error == null) {
                logger.debug("Destroy {} because pool already closed", object);
                handler.destroy(object);
            }
            promise.completeExceptionally(new IllegalStateException("pool was closed"));
        } else if (error == null) {
            promise.complete(object);
        } else {
            promise.completeExceptionally(error);
            runPendingAcquireTasks();
        }
    }

    private boolean tryToMoveObjectToPendingTask(T object) {
        PendingAcquireTask task = pendingAcquireTasks.poll();
        if (task != null && task.timeout.cancel()) {
            pendingAcquireCount.decrementAndGet();
            logger.debug("Move object to pending task: current pending acquire count: {}", pendingAcquireCount.get());
            onAcquire(task.promise, object, null);
            return true;
        }
        return false;
    }

    private void runPendingAcquireTasks() {
        while (true) {
            final int count = acquiredObjectsCount.get();
            if (count >= maxSize) {
                break;
            }
            if (!acquiredObjectsCount.compareAndSet(count, count + 1)) {
                continue;
            }

            PendingAcquireTask task = pendingAcquireTasks.poll();
            if (task != null && task.timeout.cancel()) {
                pendingAcquireCount.decrementAndGet();
                doAcquireOrCreate(task.promise, task.deadlineAfter);
            } else {
                acquiredObjectsCount.decrementAndGet();
                break;
            }
        }

        logger.debug("Run pending: current pending/acquired count: {}/{}",
                pendingAcquireCount.get(), acquiredObjectsCount.get());

        // we should never have a negative values
        assert pendingAcquireCount.get() >= 0;
        assert acquiredObjectsCount.get() >= 0;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        keepAliveTask.stop();

        IllegalStateException ex = new IllegalStateException("pool was closed");
        PendingAcquireTask task;
        while ((task = pendingAcquireTasks.poll()) != null) {
            task.promise.completeExceptionally(ex);
            task.timeout.cancel();
        }

        PooledObject<T> object;
        while ((object = pollObject()) != null) {
            // avoid simultaneous session destruction
            logger.debug("Destroy {} because pool is closed", object);
            CompletableFuture<Void> future = handler.destroy(object.getValue());
            try {
                // do not wait forever to prevent process hangup on exit
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException ignore) {
                // keep going
            } catch (Exception e) {
                throw new RuntimeException("cannot destroy " + object, e);
            }
        }

        acquiredObjectsCount.set(0);
        pendingAcquireCount.set(0);
    }

    /**
     * POOLED OBJECT
     */
    private static final class PooledObject<T> {
        private final T value;
        private final long pooledAt; // time when object was put into this pool
        private volatile long keepAlivedAt; // last time when object was processed by keep alive
        private volatile Boolean needsToBeDestroyed; // Indicates if the object needs to be destroyed on next KeepAliveTask

        PooledObject(T value, long pooledAt) {
            this.value = value;
            this.pooledAt = pooledAt;
            this.keepAlivedAt = pooledAt;
            this.needsToBeDestroyed = false;
        }

        T getValue() {
            return value;
        }

        long getPooledAt() {
            return pooledAt;
        }

        long getKeepAlivedAt() {
            return keepAlivedAt;
        }

        void setKeepAlivedAt(long keepAlivedAt) {
            this.keepAlivedAt = keepAlivedAt;
        }

        Boolean getNeedsToBeDestroyed() {
            return needsToBeDestroyed;
        }

        void setNeedsToBeDestroyed(Boolean needsToBeDestroyed) {
            this.needsToBeDestroyed = needsToBeDestroyed;
        }
    }

    /**
     * PENDING ACQUIRE TASK
     */
    private final class PendingAcquireTask implements TimerTask {
        final CompletableFuture<T> promise;
        final long timeoutNanos;
        final long deadlineAfter;
        final Timeout timeout;

        PendingAcquireTask(CompletableFuture<T> promise, long timeoutNanos, long deadlineAfter) {
            this.promise = promise;
            this.timeoutNanos = timeoutNanos;
            this.deadlineAfter = deadlineAfter;
            this.timeout = Async.runAfter(this, timeoutNanos, TimeUnit.NANOSECONDS);
        }

        @Override
        public void run(Timeout timeout) {
            int count = pendingAcquireCount.decrementAndGet();
            assert count >= 0;
            pendingAcquireTasks.remove(this);

            String msg = "cannot acquire object within " + TimeUnit.NANOSECONDS.toMillis(timeoutNanos) + "ms";
            onAcquire(promise, null, new TimeoutException(msg));
        }
    }

    /**
     * KEEP ALIVE TASK
     */
    private final class KeepAliveTask implements TimerTask {

        private final long keepAliveTimeMillis;
        private final long maxIdleTimeMillis;
        private final int batchSize;
        private Timeout scheduledHandle = null;

        private volatile boolean stoped = false;

        KeepAliveTask(long keepAliveTimeMillis, long maxIdleTimeMillis, int batchSize) {
            this.keepAliveTimeMillis = keepAliveTimeMillis;
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            this.batchSize = batchSize;
        }

        @Override
        public void run(Timeout timeout) {
            if (stoped) {
                return;
            }

            final List<PooledObject<T>> toDestroy = new ArrayList<>();
            final List<PooledObject<T>> toKeepAlive = new ArrayList<>();

            synchronized (objects) {
                final long nowMillis = System.currentTimeMillis();
                if (keepAliveTaskLogger.isDebugEnabled()) {
                    keepAliveTaskLogger.debug("Start objects processing in KeepAliveTask");
                }

                for (Iterator<PooledObject<T>> it = objects.iterator(); it.hasNext(); ) {
                    PooledObject<T> o = it.next();
                    if (o.getNeedsToBeDestroyed()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                    "Destroy {} because it was marked for destruction during previous keep alive task",
                                    o.getValue());
                        }
                        toDestroy.add(o);
                        it.remove();
                    } else {
                        long idleTime = nowMillis - o.getPooledAt();
                        if (idleTime >= maxIdleTimeMillis && objects.size() > minSize) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "Destroy {} because idle time {} >= max idle time {}",
                                        o.getValue(), idleTime, maxIdleTimeMillis);
                            }
                            toDestroy.add(o);
                            it.remove();
                        } else if ((nowMillis - o.getKeepAlivedAt()) >= keepAliveTimeMillis) {
                            toKeepAlive.add(o);
                        }
                    }
                }
                if (keepAliveTaskLogger.isDebugEnabled()) {
                    keepAliveTaskLogger.debug("Finished objects processing in KeepAliveTask in {}ms",
                            System.currentTimeMillis() - nowMillis);
                }
            }

            CompletableFuture<Void> destroy = destroy(toDestroy);
            CompletableFuture<Void> keepAlive = keepAlive(toKeepAlive);

            allOf(destroy, keepAlive)
                .whenComplete((aVoid, throwable) -> {
                    if (!stoped) {
                        scheduleNext();
                    }
                });
        }

        private CompletableFuture<Void> keepAlive(List<PooledObject<T>> objects) {
            if (objects.isEmpty()) {
                return completedFuture(null);
            }

            // get objects that are not keepalived more time
            PooledObject[] objectsArr = objects.toArray(new PooledObject[0]);
            Arrays.sort(objectsArr, Comparator.comparing(PooledObject::getKeepAlivedAt));

            int size = Math.min(objectsArr.length, batchSize);
            CompletableFuture[] futures = new CompletableFuture[size];
            for (int i = 0; i < size; i++) {
                PooledObject pooledObject = objectsArr[i];
                //noinspection unchecked
                futures[i] = handler.keepAlive((T) pooledObject.getValue())
                    .whenComplete((result, throwable) -> {
                        pooledObject.setKeepAlivedAt(System.currentTimeMillis());
                        if (throwable != null) {
                            pooledObject.setNeedsToBeDestroyed(true);
                            logger.warn("Keep alive for " + pooledObject.getValue() +
                                    " failed with exception. Marking it for destruction.", throwable);
                        } else {
                            switch(result.getCode()) {
                                case BAD_SESSION:
                                case SESSION_BUSY:
                                case INTERNAL_ERROR:
                                    logger.debug("Keep alive for " + pooledObject.getValue() +
                                            " failed with code " + result.getCode().toString() +
                                            ". Marking it for destruction.");
                                    pooledObject.setNeedsToBeDestroyed(true);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
            }

            return allOf(futures);
        }

        private CompletableFuture<Void> destroy(List<PooledObject<T>> objects) {
            if (objects.isEmpty()) {
                return completedFuture(null);
            }
            CompletableFuture[] futures = new CompletableFuture[objects.size()];
            for (int i = 0; i < objects.size(); i++) {
                futures[i] = handler.destroy(objects.get(i).getValue());
            }
            return allOf(futures);
        }

        void scheduleNext() {
            long delayMillis = Math.min(1_000, keepAliveTimeMillis / 2);
            scheduledHandle = Async.runAfter(this, delayMillis, TimeUnit.MILLISECONDS);
        }

        void stop() {
            stoped = true;
            if (scheduledHandle != null) {
                scheduledHandle.cancel();
                scheduledHandle = null;
            }
        }
    }
}
