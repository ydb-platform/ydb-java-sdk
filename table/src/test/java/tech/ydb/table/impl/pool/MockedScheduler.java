package tech.ydb.table.impl.pool;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedScheduler implements ScheduledExecutorService {
    private final MockedClock clock;
    private final PriorityBlockingQueue<MockedTask<?>> tasks = new PriorityBlockingQueue<>();
    private volatile boolean stopped = false;

    public MockedScheduler(MockedClock clock) {
        this.clock = clock;
    }
    
    public int runTasksTo(Instant timestamp) {
        int count = 0;
        MockedTask<?> next = tasks.poll();
        while (next != null && next.time.isBefore(timestamp)) {
            tasks.remove(next);

            clock.goToFuture(timestamp);
            next.run();
            count += 1;

            if (next.time != null) {
                tasks.add(next);
            }

            next = tasks.poll();
        }
        clock.goToFuture(timestamp);

        return count;
    }
    
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Instant time = clock.instant().plusNanos(unit.toNanos(delay));
        MockedTask<?> task = new MockedTask<Void>(command, null, time);
        tasks.add(task);
        return task;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        Instant time = clock.instant().plusNanos(unit.toNanos(delay));
        MockedTask<V> task = new MockedTask<>(callable, time);
        tasks.add(task);
        return task;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Instant time = clock.instant().plusNanos(unit.toNanos(initialDelay));
        MockedTask<?> task = new MockedTask<Void>(command, null, time, unit.toMillis(initialDelay));
        tasks.add(task);
        return task;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Instant time = clock.instant().plusNanos(unit.toNanos(initialDelay));
        MockedTask<?> task = new MockedTask<Void>(command, null, time, -unit.toMillis(initialDelay));
        tasks.add(task);
        return task;
    }

    @Override
    public void shutdown() {
        stopped = true;
    }

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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return schedule(Executors.callable(task, result), 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void execute(Runnable command) {
        throw new UnsupportedOperationException("Not supported yet."); 
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
        public long getDelay(TimeUnit unit) {
            return unit.convert(time.toEpochMilli() - clock.millis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
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
                if (runAndReset()) {
                    setNextRunTime();
                }
            } else {
                run();
                time = null;
            }
        }
    }
}
