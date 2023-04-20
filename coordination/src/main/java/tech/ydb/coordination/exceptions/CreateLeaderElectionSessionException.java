package tech.ydb.coordination.exceptions;

import tech.ydb.core.Status;

public class CreateLeaderElectionSessionException extends RuntimeException {
    public CreateLeaderElectionSessionException(Status status) {
        super("Fail with status: " + status);
    }
}
