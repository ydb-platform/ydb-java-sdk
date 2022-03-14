package tech.ydb.core;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import tech.ydb.OperationProtos.Operation;
import tech.ydb.StatusCodesProtos.StatusIds;


/**
 * @author Sergey Polovko
 */
public final class Operations {
    private Operations() {}

    public static Status status(Operation operation) {
        if (operation.getStatus() == StatusIds.StatusCode.SUCCESS && operation.getIssuesCount() == 0) {
            return Status.SUCCESS;
        }
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        return Status.of(code, Issue.fromPb(operation.getIssuesList()));
    }

    public static <M extends Message> M unpackResult(Operation operation, Class<M> clazz) {
        try {
            return operation.getResult().unpack(clazz);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("cannot unpack result of operation: " + operation.getId(), e);
        }
    }
}
