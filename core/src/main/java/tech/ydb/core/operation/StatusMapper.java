package tech.ydb.core.operation;

import java.util.List;
import java.util.function.Function;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.YdbIssueMessage.IssueMessage;

public class StatusMapper<R> implements Function<Result<R>, Status> {
    private final Function<R, StatusIds.StatusCode> statusMethod;
    private final Function<R, List<IssueMessage>> issuesMethod;

    private StatusMapper(Function<R, StatusIds.StatusCode> status, Function<R, List<IssueMessage>> issues) {
        this.statusMethod = status;
        this.issuesMethod = issues;
    }

    @Override
    public Status apply(Result<R> result) {
        if (!result.isSuccess()) {
            return result.getStatus();
        }

        R resp = result.getValue();
        return Status.of(
                StatusCode.fromProto(statusMethod.apply(resp)),
                result.getStatus().getConsumedRu(),
                Issue.fromPb(issuesMethod.apply(resp))
        );
    }

    public static <T> StatusMapper<T> of(
            Function<T, StatusIds.StatusCode> statusMethod,
            Function<T, List<IssueMessage>> issuesMethod
    ) {
        return new StatusMapper<>(statusMethod, issuesMethod);
    }
}
