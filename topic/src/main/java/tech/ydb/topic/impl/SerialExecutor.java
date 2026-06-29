package tech.ydb.topic.impl;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean isExecuted = new AtomicBoolean(false);
    private final boolean skipAllowed;

    public SerialExecutor(Executor executor) {
        this(executor, false);
    }

    SerialExecutor(Executor executor, boolean skipAllowed) {
        this.executor = executor;
        this.skipAllowed = skipAllowed;
    }

    @Override
    public void execute(Runnable task) {
        if (skipAllowed && !tasks.isEmpty()) {
            return;
        }

        tasks.offer(task);

        if (isExecuted.compareAndSet(false, true)) {
            try {
                executor.execute(this);
            } catch (RuntimeException ex) {
                logger.error("SerialExecutor cannot execute task", ex);
                isExecuted.set(false);
                throw ex;
            }
        }
    }

    @Override
    public void run() {
        boolean hasMore = true;
        while (hasMore) {
            try {
                Iterator<Runnable> it = tasks.iterator();
                while (it.hasNext()) {
                    Runnable task = it.next();
                    it.remove();
                    task.run();
                }
            } catch (RuntimeException ex) {
                logger.error("SerialExecutor task execution problem", ex);
            } finally {
                isExecuted.set(false);
            }
            // Repeat if new task appears before isExecuted resetting
            hasMore = !tasks.isEmpty() && isExecuted.compareAndSet(false, true);
        }
    }
}
