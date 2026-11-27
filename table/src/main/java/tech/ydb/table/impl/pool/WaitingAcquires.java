package tech.ydb.table.impl.pool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final class WaitingAcquires<T> {
    private final AtomicInteger count = new AtomicInteger();

    private Waiter<T> first;
    private Waiter<T> last;

    int count() {
        return count.get();
    }

    boolean compareAndReserve(int expectedCount) {
        return count.compareAndSet(expectedCount, expectedCount + 1);
    }

    void releaseReservation() {
        count.decrementAndGet();
    }

    Waiter<T> newWaiter(CompletableFuture<T> acquire) {
        return new Waiter<>(this, acquire);
    }

    synchronized void offerLast(Waiter<T> waiter) {
        if (waiter.linked) {
            unlink(waiter);
        }
        waiter.previous = last;
        waiter.next = null;
        waiter.linked = true;
        if (last == null) {
            first = waiter;
        } else {
            last.next = waiter;
        }
        last = waiter;
    }

    synchronized void offerFirst(Waiter<T> waiter) {
        if (waiter.linked) {
            unlink(waiter);
        }
        waiter.previous = null;
        waiter.next = first;
        waiter.linked = true;
        if (first == null) {
            last = waiter;
        } else {
            first.previous = waiter;
        }
        first = waiter;
    }

    synchronized Waiter<T> pollFirst() {
        if (first == null) {
            return null;
        }
        Waiter<T> waiter = first;
        unlink(waiter);
        // Polling only unlinks; callers decide when to release the reservation.
        return waiter;
    }

    synchronized boolean remove(Waiter<T> waiter) {
        if (!waiter.linked) {
            return false;
        }
        unlink(waiter);
        count.decrementAndGet();
        return true;
    }

    private void unlink(Waiter<T> waiter) {
        Waiter<T> previous = waiter.previous;
        Waiter<T> next = waiter.next;
        if (previous == null) {
            first = next;
        } else {
            previous.next = next;
        }
        if (next == null) {
            last = previous;
        } else {
            next.previous = previous;
        }
        waiter.previous = null;
        waiter.next = null;
        waiter.linked = false;
    }

    static final class Waiter<T> implements BiConsumer<T, Throwable> {
        private final WaitingAcquires<T> owner;
        final CompletableFuture<T> acquire;
        private Waiter<T> previous;
        private Waiter<T> next;
        private boolean linked;

        private Waiter(WaitingAcquires<T> owner, CompletableFuture<T> acquire) {
            this.owner = owner;
            this.acquire = acquire;
        }

        @Override
        public void accept(T object, Throwable error) {
            owner.remove(this);
        }
    }
}
