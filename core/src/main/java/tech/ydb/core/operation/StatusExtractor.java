package tech.ydb.core.operation;

import java.util.List;
import java.util.function.Function;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.YdbIssueMessage.IssueMessage;

public class StatusExtractor<R> implements Function<Result<R>, Result<R>> {
    private final Function<R, StatusIds.StatusCode> statusMethod;
    private final Function<R, List<IssueMessage>> issuesMethod;

    private StatusExtractor(Function<R, StatusIds.StatusCode> status, Function<R, List<IssueMessage>> issues) {
        this.statusMethod = status;
        this.issuesMethod = issues;
    }

    @Override
    public Result<R> apply(Result<R> result) {
        if (!result.isSuccess()) {
            return result;
        }

        R resp = result.getValue();
        Status status = Status.of(
                StatusCode.fromProto(statusMethod.apply(resp)),
                result.getStatus().getConsumedRu(),
                Issue.fromPb(issuesMethod.apply(resp))
        );

        return status.isSuccess() ? Result.success(resp, status) : Result.fail(status);
    }

    public static <T> StatusExtractor<T> of(
            Function<T, StatusIds.StatusCode> statusMethod,
            Function<T, List<IssueMessage>> issuerMethod
    ) {
        return new StatusExtractor<>(statusMethod, issuerMethod);
    }
}
