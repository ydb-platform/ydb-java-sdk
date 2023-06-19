package tech.ydb.core.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbSchedulerFactory {
    private static final long WAIT_FOR_SHUTDOWN_MS = 1000;

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
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
