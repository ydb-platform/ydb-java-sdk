package tech.ydb.topic.impl;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SerialRunnable implements Runnable {
    private final Runnable task;
    private final SerialExecutor executor;

    public SerialRunnable(Runnable task) {
        this.task = task;
        this.executor = new SerialExecutor(Runnable::run, true);
    }

    @Override
    public void run() {
        executor.execute(task);
    }
}
