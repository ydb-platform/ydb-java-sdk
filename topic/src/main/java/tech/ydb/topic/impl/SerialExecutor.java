package tech.ydb.topic.impl;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SerialExecutor implements Executor, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SerialExecutor.class);

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final Executor executor;
    private final AtomicInteger tasksCount = new AtomicInteger(0);
    private final AtomicBoolean isExecuted = new AtomicBoolean(false);
    private final boolean skipAllowed;

    public SerialExecutor(Executor executor) {
        this(executor, false);
    }

    public SerialExecutor(Executor executor, boolean skipAllowed) {
        this.executor = executor;
        this.skipAllowed = skipAllowed;
    }

    @Override
    public void execute(Runnable task) {
        if (skipAllowed && tasksCount.get() > 0) {
            return;
        }

        tasksCount.incrementAndGet();
        tasks.offer(task);
        if (isExecuted.compareAndSet(false, true)) {
            executor.execute(this);
        }
    }

    @Override
    public void run() {
        while (!tasks.isEmpty()) {
            Iterator<Runnable> it = tasks.iterator();
            while (it.hasNext()) {
                tasksCount.decrementAndGet();
                Runnable task = it.next();
                it.remove();
                try {
                    task.run();
                } catch (RuntimeException ex) {
                    logger.error("SerialExecutor problem", ex);
                }
            }
        }

        isExecuted.set(false);

        // Repeat if new task appears before isExecuted reseting
        if (tasksCount.get() > 0 && isExecuted.compareAndSet(false, true)) {
            executor.execute(this);
        }
    }
}
