package tech.ydb.topic.impl;

import java.util.concurrent.Executor;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SerialRunnable implements Runnable {
    private final Runnable task;
    private final SerialExecutor serial;

    public SerialRunnable(Runnable task) {
        this(Runnable::run, task);
    }

    public SerialRunnable(Executor executor, Runnable task) {
        this.task = task;
        this.serial = new SerialExecutor(executor, true);
    }

    @Override
    public void run() {
        serial.execute(task);
    }
}
