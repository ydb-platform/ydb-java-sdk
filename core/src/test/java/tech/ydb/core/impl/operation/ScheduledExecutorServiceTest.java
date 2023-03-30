package tech.ydb.core.impl.operation;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.config.CronScheduledFuture;

public class ScheduledExecutorServiceTest extends ScheduledThreadPoolExecutor {
    private Runnable command;

    public ScheduledExecutorServiceTest() {
        super(0);
    }

    public ScheduledExecutorServiceTest(int corePoolSize) {
        super(corePoolSize);
    }

    public ScheduledExecutorServiceTest(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public ScheduledExecutorServiceTest(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public ScheduledExecutorServiceTest(int corePoolSize, ThreadFactory threadFactory,
                                        RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        this.command = command;

        // unused scheduled future
        return new CronScheduledFuture<>(null, null);
    }

    public void execCommand() {
        command.run();
    }
}
