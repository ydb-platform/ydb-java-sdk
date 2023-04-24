package tech.ydb.coordination.exceptions;

import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public class CreateSessionException extends RuntimeException {
    public CreateSessionException(Status status) {
        super("Fail with status: " + status);
    }
}
