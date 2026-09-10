package tech.ydb.topic.read.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class LazyExecutor implements Executor, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LazyExecutor.class);
    private static final int MAX_EXECUTOR_THREADS_COUNT = 4;

    private final String name;
    private final Executor custom;
    private final AtomicInteger threadsCount = new AtomicInteger();
    private final AtomicReference<ExecutorService> service = new AtomicReference<>();
    private volatile boolean isStopped = false;

    public LazyExecutor(String name, Executor custom) {
        this.name = name;
        this.custom = custom;
    }

    @Override
    public void execute(Runnable command) {
        if (isStopped) {
            return;
        }

        if (custom != null) {
            custom.execute(command);
            return;
        }

        ExecutorService local = service.get();
        while (!isStopped && local == null) {
            ThreadFactory factory = r -> new Thread(r, name + "-" + threadsCount.incrementAndGet());
            ExecutorService pool = Executors.newFixedThreadPool(MAX_EXECUTOR_THREADS_COUNT, factory);
            if (!service.compareAndSet(local, pool)) {
                pool.shutdown();
            }
            local = service.get();
        }

        if (!isStopped) {
            local.execute(command);
        } else {
            shutdown(service.getAndSet(null));
        }
    }

    @Override
    public void close() {
        isStopped = true;
        shutdown(service.getAndSet(null));
    }

    private void shutdown(ExecutorService service) {
        if (service == null) {
            return;
        }

        try {
            service.shutdown();
            if (!service.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("executor {} shutdown interrupted", name, e);
            Thread.currentThread().interrupt();
        }
    }
}
