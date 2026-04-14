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

    SerialExecutor(Executor executor, boolean skipAllowed) {
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
        tryRun();
    }

    private void tryRun() {
        if (!tasks.isEmpty() && isExecuted.compareAndSet(false, true)) {
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
        try {
            while (!tasks.isEmpty()) {
                Iterator<Runnable> it = tasks.iterator();
                while (it.hasNext()) {
                    tasksCount.decrementAndGet();
                    Runnable task = it.next();
                    it.remove();
                    task.run();
                }
            }
        } catch (RuntimeException ex) {
            logger.error("SerialExecutor problem", ex);
            throw ex;
        } finally {
            isExecuted.set(false);

            // Repeat if new task appears before isExecuted resetting
            if (tasksCount.get() > 0) {
                tryRun();
            }
        }
    }
}
