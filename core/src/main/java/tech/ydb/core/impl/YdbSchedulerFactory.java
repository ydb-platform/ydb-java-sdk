package tech.ydb.core.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbSchedulerFactory {
    /** Scheduler waits for closing of channels so this timeout must be greater that GrpcChannel.WAIT_FOR_CLOSING_MS */
    private static final long WAIT_FOR_SHUTDOWN_MS = 2 * 5000;

    private static final Logger logger = LoggerFactory.getLogger(YdbSchedulerFactory.class);

    private YdbSchedulerFactory() { }

    public static ScheduledExecutorService createScheduler() {
        // default size of ydb shared scheduler
        int threads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
        return Executors.newScheduledThreadPool(threads, new YdbThreadFactory());
    }

    public static boolean shutdownScheduler(ScheduledExecutorService scheduler) {
        try {
            scheduler.shutdown();
            boolean closed = scheduler.awaitTermination(WAIT_FOR_SHUTDOWN_MS, TimeUnit.MILLISECONDS);
            if (!closed) {
                logger.warn("ydb scheduler shutdown timeout exceeded, terminate");
                for (Runnable task: scheduler.shutdownNow()) {
                    logger.warn("   task {} is terminated", task);
                }
                closed = scheduler.awaitTermination(WAIT_FOR_SHUTDOWN_MS, TimeUnit.MILLISECONDS);
                if (!closed) {
                    logger.warn("ydb scheduler shutdown problem");
                }
            }

            return closed;
        } catch (InterruptedException e) {
            logger.warn("ydb scheduler shutdown interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static ScheduledExecutorService wrapExternal(ScheduledExecutorService scheduler) {
        return new NonStoppableScheduler(scheduler);
    }

    private static class YdbThreadFactory implements ThreadFactory {
        private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(1);

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        YdbThreadFactory() {
            namePrefix = "ydb-scheduler-" +
                          INSTANCE_COUNT.getAndIncrement() +
                         "-thread-";
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }


    private static class NonStoppableScheduler implements ScheduledExecutorService {
        private final ScheduledExecutorService origin;

        NonStoppableScheduler(ScheduledExecutorService service) {
            this.origin = service;
        }

        @Override
        public void shutdown() {
            // Nothing
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return origin.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return origin.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return origin.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            return origin.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return origin.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return origin.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return origin.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return origin.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return origin.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
                ExecutionException {
            return origin.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return origin.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            origin.execute(command);
        }
    }
}
