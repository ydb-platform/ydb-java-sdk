package tech.ydb.topic.impl;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SerialExecutor implements Executor, Runnable {
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isExecuted = new AtomicBoolean(false);
    private final Executor executor;
    private final boolean skipAllowed;

    public SerialExecutor(Executor executor, boolean skipAllowed) {
        this.executor = executor;
        this.skipAllowed = skipAllowed;
    }

    @Override
    public void execute(Runnable task) {
        if (skipAllowed && isExecuted.get()) {
            return;
        }

        tasks.offer(task);
        if (isExecuted.compareAndExchange(false, true)) {
            executor.execute(this);
        }
    }

    @Override
    public void run() {
        while (!tasks.isEmpty()) {
            Iterator<Runnable> it = tasks.iterator();
            while (it.hasNext()) {
                it.next().run();
                it.remove();
            }
        }

        isExecuted.set(false);

        // Repeat if new task appear before reset isExecuted
        if (!tasks.isEmpty() && isExecuted.compareAndExchange(false, true)) {
            executor.execute(this);
        }
    }
}
