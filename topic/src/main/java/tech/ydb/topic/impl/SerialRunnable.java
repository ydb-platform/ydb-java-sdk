package tech.ydb.topic.impl;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SerialRunnable implements Runnable {
    private final Runnable task;
    private final SerialExecutor serial;

    public SerialRunnable(Runnable task) {
        this.task = task;
        this.serial = new SerialExecutor(Runnable::run, true);
    }

    @Override
    public void run() {
        serial.execute(task);
    }
}
