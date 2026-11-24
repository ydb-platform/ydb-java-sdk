package tech.ydb.topic.write.impl.multi;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import tech.ydb.topic.settings.MultiWriterSettings;

/** Owned bounded executors, with a deterministic injection seam for tests. */
final class MultiWriterRuntime {
    private static final AtomicInteger IDS = new AtomicInteger();

    final Executor coordinator;
    final Executor topicCallExecutor;
    final Executor completion;
    final Executor controlCompletion;
    final ScheduledExecutorService scheduler;
    final LongSupplier nanoTime;
    private final boolean owned;

    MultiWriterRuntime(MultiWriterSettings settings) {
        int capacity = Math.toIntExact(Math.min(Integer.MAX_VALUE - 1L,
                4L * settings.getBufferMessages() + 16L * settings.getMaxWriters() + 128));
        ThreadFactory threads = task -> {
            Thread thread = new Thread(task, "ydb-multiwriter-" + IDS.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.topicCallExecutor = pool(settings.getIoThreads(), 2 * settings.getMaxWriters() + 16, threads);
        // Admitted messages/barriers bound outstanding completion jobs. Managed blocking compensates
        // for user callbacks joining flush/shutdown futures, without blocking the coordinator or topic call executor.
        this.completion = new ForkJoinPool(2);
        this.controlCompletion = new ForkJoinPool(2);
        this.coordinator = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), threads, new ThreadPoolExecutor.AbortPolicy()) {
            @Override
            protected void terminated() {
                // Accepted commands can still request lower cleanup or complete a flush after the close deadline.
                // Keep those executors alive until the coordinator has drained every accepted command.
                ((ExecutorService) topicCallExecutor).shutdown();
                ((ExecutorService) completion).shutdown();
                ((ExecutorService) controlCompletion).shutdown();
            }
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(threads);
        this.nanoTime = System::nanoTime;
        this.owned = true;
    }

    MultiWriterRuntime(Executor coordinator, Executor topicCallExecutor, Executor completion,
            ScheduledExecutorService scheduler, LongSupplier nanoTime) {
        this.coordinator = coordinator;
        this.topicCallExecutor = topicCallExecutor;
        this.completion = completion;
        this.controlCompletion = completion;
        this.scheduler = scheduler;
        this.nanoTime = nanoTime;
        this.owned = false;
    }

    private static ExecutorService pool(int size, int capacity, ThreadFactory threads) {
        return new ThreadPoolExecutor(size, size, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), threads, new ThreadPoolExecutor.AbortPolicy());
    }

    void shutdown() {
        if (owned) {
            scheduler.shutdownNow();
            ((ExecutorService) coordinator).shutdown();
        }
    }
}
