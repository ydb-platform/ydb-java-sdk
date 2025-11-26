package tech.ydb.core.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import org.junit.Assert;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedScheduler implements ScheduledExecutorService {
    private final MockedClock clock;
    private final Lock nextTaskLock = new ReentrantLock();
    private final Queue<MockedTask<?>> tasks = new ConcurrentLinkedQueue<>();

    private volatile boolean queueIsBlocked = false;
    private volatile boolean stopped = false;

    public MockedScheduler(MockedClock clock) {
        this.clock = clock;
    }

    public MockedScheduler hasNoTasks() {
        Assert.assertTrue("Scheduler hasn't tasks", tasks.isEmpty());
        return this;
    }

    public MockedScheduler hasTasksCount(int count) {
        Assert.assertEquals("Scheduler has invalid count of tasks", count, tasks.size());
        return this;
    }

    public MockedScheduler runNextTask() {
        nextTaskLock.lock();
        try {
            queueIsBlocked = true;
            MockedTask<?> next = tasks.poll();
            Assert.assertNotNull("Scheduler's queue is empty", next);
            clock.goToFuture(next.time);
            next.run();
            if (next.time != null) {
                tasks.add(next);
            }

            queueIsBlocked = false;
            return this;
        } finally {
            nextTaskLock.unlock();
        }
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        Instant time = clock.instant().plusNanos(unit.toNanos(delay));
        MockedTask<?> task = new MockedTask<Void>(command, null, time);
        tasks.add(task);
        return task;
    }

    @Nonnull
    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        Instant time = clock.instant().plusNanos(unit.toNanos(delay));
        MockedTask<V> task = new MockedTask<>(callable, time);
        tasks.add(task);
        return task;
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            @Nonnull Runnable command,
            long initialDelay,
            long period,
            @Nonnull TimeUnit unit
    ) {
        Instant time = clock.instant().plusNanos(unit.toNanos(initialDelay));
        MockedTask<?> task = new MockedTask<Void>(command, null, time, unit.toMillis(period));
        tasks.add(task);
        return task;
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            @Nonnull Runnable command,
            long initialDelay,
            long delay,
            @Nonnull TimeUnit unit
    ) {
        Instant time = clock.instant().plusNanos(unit.toNanos(initialDelay));
        MockedTask<?> task = new MockedTask<Void>(command, null, time, -unit.toMillis(initialDelay));
        tasks.add(task);
        return task;
    }

    @Override
    public void shutdown() {
        stopped = true;
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        stopped = true;
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return stopped;
    }

    @Override
    public boolean isTerminated() {
        return stopped;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) {
        return true;
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        if (queueIsBlocked) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
        return schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        if (queueIsBlocked) {
            task.run();
            return CompletableFuture.completedFuture(result);
        }
        return schedule(Executors.callable(task, result), 0, TimeUnit.MILLISECONDS);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        if (queueIsBlocked) {
            CompletableFuture<T> future = new CompletableFuture<>();
            try {
                future.complete(task.call());
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
            return future;
        }
        return schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        if (queueIsBlocked) {
            command.run();
            return;
        }
        schedule(command, 0, TimeUnit.MILLISECONDS);
    }

    private class MockedTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
        private volatile Instant time;
        private final long period;

        /**
         * Creates a one-shot action with given trigger time.
         */
        MockedTask(Runnable r, V result, Instant triggerTime) {
            super(r, result);
            this.time = triggerTime;
            this.period = 0;
        }

        MockedTask(Runnable r, V result, Instant triggerTime,
                            long period) {
            super(r, result);
            this.time = triggerTime;
            this.period = period;
        }

        MockedTask(Callable<V> callable, Instant triggerTime) {
            super(callable);
            this.time = triggerTime;
            this.period = 0;
        }

        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            return unit.convert(time.toEpochMilli() - clock.millis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@Nonnull Delayed other) {
            if (other == this) // compare zero if same object
                return 0;
            if (other instanceof MockedTask) {
                MockedTask<?> x = (MockedTask<?>)other;
                return time.compareTo(x.time);
            }

            @SuppressWarnings("null")
            long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        @Override
        public boolean isPeriodic() {
            return period != 0;
        }

        private void setNextRunTime() {
            time = period > 0 ? time.plusMillis(period) : clock.instant().plusMillis(-period);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled) {
                tasks.remove(this);
            }
            return cancelled;
        }

        @Override
        public void run() {
            if (stopped) {
                cancel(false);
                return;
            }

            if (isPeriodic()) {
                if (super.runAndReset()) {
                    setNextRunTime();
                }
            } else {
                super.run();
                time = null;
            }
        }
    }
}
