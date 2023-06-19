package tech.ydb.table.impl.pool;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <T> type of objects in queue
*/
@ThreadSafe
public class WaitingQueue<T> implements AutoCloseable {
    public interface Handler<T> {
        CompletableFuture<T> create();
        void destroy(T object);
    }

    /** Limit of waiting requests = maxSize * constant */
    @VisibleForTesting
    static final int WAITINGS_LIMIT_FACTOR = 10;

    private final int maxSize;
    private final int waitingsLimit;

    private final Handler<T> handler;
    private volatile boolean stopped = false;

    /** Deque of idle objects */
    private final ConcurrentLinkedDeque<T> idle = new ConcurrentLinkedDeque<>();
    /** Non idle objects managed by WaitingQueue */
    private final Map<T, T> used = new ConcurrentHashMap<>();
    /** Set of pending object creations */
    private final Map<CompletableFuture<T>, CompletableFuture<T>> pendingRequests = new ConcurrentHashMap<>();

    /** Summary size of queue = idle.size() + used.size() + pendingRequests.size() */
    private final AtomicInteger queueSize = new AtomicInteger(0);

    /** Queue of waiting acquire requests */
    private final Queue<CompletableFuture<T>> waitingAcquires = new ConcurrentLinkedQueue<>();
    /** Size of waiting acquires queue */
    private final AtomicInteger waitingAcqueireCount = new AtomicInteger(0);

    @VisibleForTesting
    WaitingQueue(Handler<T> handler, int maxSize, int waitingsLimit) {
        Preconditions.checkArgument(maxSize > 0, "WaitingQueue max size (%d) must be positive", maxSize);
        Preconditions.checkArgument(handler != null, "WaitingQueue handler must be not null");

        this.handler = handler;
        this.maxSize = maxSize;
        this.waitingsLimit = waitingsLimit;
    }

    public WaitingQueue(Handler<T> handler, int maxSize) {
        this(handler, maxSize, maxSize * WAITINGS_LIMIT_FACTOR);
    }

    public void acquire(CompletableFuture<T> acquire) {
        if (stopped) {
             acquire.completeExceptionally(new IllegalStateException("Queue is already closed"));
             return;
        }

        boolean ok = tryToPollIdle(acquire)
                || tryToCreateNewPending(acquire)
                || tryToCreateNewWaiting(acquire);

        if (!ok) {
            acquire.completeExceptionally(new UnexpectedResultException(
                    "Objects limit exceeded", Status.of(StatusCode.CLIENT_RESOURCE_EXHAUSTED)
            ));
        }
    }

    public void release(T object) {
        if (!used.remove(object, object)) {
            return;
        }

        // Try to complete waiting request
        if (!tryToCompleteWaiting(object)) {
            // Put object to idle deque as hottest object
            idle.offerFirst(object); // ConcurrentLinkedDeque always return true
            if (stopped) {
                clear();
            }
        }
    }

    public void delete(T object) {
        if (!used.remove(object, object)) {
            return;
        }
        queueSize.decrementAndGet();
        handler.destroy(object);

        // After deleting one object we can try to create new pending if it needed
        checkNextWaitingAcquire();
    }

    @Override
    public void close() {
        stopped = true;
        clear();
    }

    public Iterator<T> coldIterator() {
        return new ColdIterator(idle.descendingIterator());
    }

    public int getIdleCount() {
        return idle.size();
    }

    public int getUsedCount() {
        return used.size();
    }

    public int getTotalCount() {
        return queueSize.get();
    }

    public int getPendingCount() {
        return pendingRequests.size();
    }

    public int getWaitingCount() {
        return waitingAcqueireCount.get();
    }

    public int getTotalLimit() {
        return maxSize;
    }

    public int getWaitingLimit() {
        return waitingsLimit;
    }

    private void safeAcquireObject(CompletableFuture<T> acquire, T object) {
        used.put(object, object);
        if (stopped) {
            acquire.completeExceptionally(new CancellationException("Queue is already closed"));
            if (used.remove(object, object)) {
                queueSize.decrementAndGet();
                handler.destroy(object);
            }
        } else {
            acquire.complete(object);
        }
    }

    private boolean tryToPollIdle(CompletableFuture<T> acquire) {
        // Try to poll hottest element
        T next = idle.pollFirst();
        if (next != null) {
            safeAcquireObject(acquire, next);
            return true;
        }
        return false;
    }

    private boolean tryToCreateNewPending(CompletableFuture<T> acquire) {
        int count = queueSize.get();
        while (count < maxSize) {
            if (!queueSize.compareAndSet(count, count + 1)) {
                count = queueSize.get();
                continue;
            }

            CompletableFuture<T> pending = handler.create();
            pendingRequests.put(pending, pending);
            pending.whenComplete(new PendingHandler(acquire, pending));
            return true;
        }

        return false;
    }

    private boolean tryToCreateNewWaiting(CompletableFuture<T> acquire) {
        int waitingsCount = waitingAcqueireCount.get();
        while (waitingsCount < waitingsLimit) {
            if (!waitingAcqueireCount.compareAndSet(waitingsCount, waitingsCount + 1)) {
                waitingsCount = waitingAcqueireCount.get();
                continue;
            }

            waitingAcquires.offer(acquire); // ConcurrentLinkedQueue always return true
            return true;
        }

        return false;
    }

    private boolean tryToCompleteWaiting(T object) {
        if (stopped) {
            return false;
        }

        CompletableFuture<T> next = waitingAcquires.poll();
        while (next != null) {
            waitingAcqueireCount.decrementAndGet();

            if (!next.isDone() && !next.isCancelled()) {
                safeAcquireObject(next, object);
                return true;
            }
            next = waitingAcquires.poll();
        }

        return false;
    }

    private void checkNextWaitingAcquire() {
        if (stopped || waitingAcquires.isEmpty()) {
            return;
        }

        // Try to create new pending request
        CompletableFuture<T> pending = new CompletableFuture<>();
        if (tryToCreateNewPending(pending)) {
            pending.whenComplete((object, th) -> {
                if (th != null) {
                    checkNextWaitingAcquire();
                }
                if (object != null) {
                    if (!tryToCompleteWaiting(object)) {
                        release(object);
                    }
                }
            });
        }
    }

    private void clear() {
        for (CompletableFuture<T> key : pendingRequests.keySet()) {
            if (pendingRequests.remove(key, key)) {
                queueSize.decrementAndGet();
            }
        }

        CompletableFuture<T> waiting = waitingAcquires.poll();
        while (waiting != null) {
            waiting.completeExceptionally(new CancellationException("Queue is already closed"));
            waiting = waitingAcquires.poll();
        }

        T nextIdle = idle.poll();
        while (nextIdle != null) {
            queueSize.decrementAndGet();
            handler.destroy(nextIdle);
            nextIdle = idle.poll();
        }
    }

    private class PendingHandler implements BiConsumer<T, Throwable> {
        private final CompletableFuture<T> acquire;
        private final CompletableFuture<T> pending;

        PendingHandler(CompletableFuture<T> acquire, CompletableFuture<T> pending) {
            this.acquire = acquire;
            this.pending = pending;
        }

        @Override
        public void accept(T object, Throwable th) {
            boolean ready = !acquire.isDone() && !acquire.isCancelled();

            // If pool is already closed and clean
            if (!pendingRequests.remove(pending, pending)) {
                if (ready) {
                    acquire.completeExceptionally(new CancellationException("Queue is already closed"));
                }
                if (object != null) {
                    handler.destroy(object);
                }
                return;
            }

            // The implementation of CompletableFuture
            // guarantees that if object is null then th is not null
            if (th != null) {
                queueSize.decrementAndGet();
                if (ready) {
                    acquire.completeExceptionally(th);
                }
                return;
            }

            if (ready) {
                safeAcquireObject(acquire, object);
                return;
            }

            // If acquire future is already canceled, put new object to hot queue
            idle.offerFirst(object); // ConcurrentLinkedQueue always return true
            if (stopped) {
                clear();
            }
        }
    }

    /** Iterator with custom remove action */
    private class ColdIterator implements Iterator<T> {
        private final Iterator<T> iter;
        private volatile T lastRet = null;

        ColdIterator(Iterator<T> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return this.iter.hasNext();
        }

        @Override
        public void remove() {
            if (lastRet == null) {
                return;
            }
            if (idle.removeLastOccurrence(lastRet)) {
                handler.destroy(lastRet);
                lastRet = null;
                queueSize.decrementAndGet();
                checkNextWaitingAcquire();
            }
        }

        @Override
        public T next() {
            lastRet = iter.next();
            return lastRet;
        }
    }
}
