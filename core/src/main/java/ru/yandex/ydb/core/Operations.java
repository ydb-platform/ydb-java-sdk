package ru.yandex.ydb.core;

import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import ru.yandex.ydb.OperationProtos.Operation;
import ru.yandex.ydb.StatusCodesProtos.StatusIds;
import ru.yandex.ydb.YdbIssueMessage.IssueMessage;
import ru.yandex.yql.proto.IssueSeverity.TSeverityIds;


/**
 * @author Sergey Polovko
 */
public final class Operations {
    private Operations() {}

    public static Status status(Operation operation) {
        if (operation.getStatus() == StatusIds.StatusCode.SUCCESS) {
            return Status.SUCCESS;
        }
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        return Status.of(code, issues(operation.getIssuesList()));
    }

    public static Issue[] issues(List<IssueMessage> issuesList) {
        if (issuesList.isEmpty()) {
            return Issue.EMPTY_ARRAY;
        }
        Issue[] issues = new Issue[issuesList.size()];
        for (int i = 0; i < issuesList.size(); i++) {
            issues[i] = issueFromMessage(issuesList.get(i));
        }
        return issues;
    }

    public static <M extends Message> M unpackResult(Operation operation, Class<M> clazz) {
        try {
            return operation.getResult().unpack(clazz);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("cannot unpack result of operation: " + operation.getId(), e);
        }
    }

    private static Issue issueFromMessage(IssueMessage message) {
        return Issue.of(
            message.hasPosition() ? positionFromMessage(message.getPosition()) : Issue.Position.EMPTY,
            message.hasEndPosition() ? positionFromMessage(message.getEndPosition()) : Issue.Position.EMPTY,
            message.getIssueCode(),
            message.getMessage(),
            TSeverityIds.ESeverityId.forNumber(message.getSeverity()),
            issues(message.getIssuesList()));
    }

    private static Issue.Position positionFromMessage(IssueMessage.Position m) {
        return Issue.Position.of(m.getColumn(), m.getRow(), m.getFile());
    }
}
