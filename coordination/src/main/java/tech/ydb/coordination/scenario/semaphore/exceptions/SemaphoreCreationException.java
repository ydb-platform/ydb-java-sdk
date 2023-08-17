package tech.ydb.coordination.scenario.semaphore.exceptions;

import tech.ydb.proto.coordination.SemaphoreDescription;

public class SemaphoreCreationException extends IllegalArgumentException {
    private String name;
    private long count;
    private long limit;
    private boolean ephemeral;

    public SemaphoreCreationException() {
        super();
    }

    public SemaphoreCreationException(String message) {
        super(message);
    }

    public SemaphoreCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SemaphoreCreationException(Throwable cause) {
        super(cause);
    }

    public SemaphoreCreationException(String message, SemaphoreDescription semaphoreDescription) {
        super(message);
        this.name = semaphoreDescription.getName();
        this.count = semaphoreDescription.getCount();
        this.limit = semaphoreDescription.getLimit();
        this.ephemeral = semaphoreDescription.getEphemeral();
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return count;
    }

    public long getLimit() {
        return limit;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }
}
