package tech.ydb.core.operation;

import java.util.concurrent.ScheduledExecutorService;

/**
 *
 * @author Aleksandr Gorshenin
 */
interface AsyncOperation<T> extends Operation<T> {
    ScheduledExecutorService getScheduler();
}
