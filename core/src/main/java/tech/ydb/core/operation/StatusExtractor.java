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
    private final Function<R, StatusIds.StatusCode> statusFunctor;
    private final Function<R, List<IssueMessage>> issuesFunctor;

    private StatusExtractor(Function<R, StatusIds.StatusCode> statusFunc, Function<R, List<IssueMessage>> issuesFun) {
        this.statusFunctor = statusFunc;
        this.issuesFunctor = issuesFun;
    }

    @Override
    public Result<R> apply(Result<R> result) {
        if (!result.isSuccess()) {
            return result;
        }

        R resp = result.getValue();
        Status status = Status.of(
                StatusCode.fromProto(statusFunctor.apply(resp)),
                result.getStatus().getConsumedRu(),
                Issue.fromPb(issuesFunctor.apply(resp))
        );

        return status.isSuccess() ? Result.success(resp, status) : Result.fail(status);
    }

    public static <T> StatusExtractor<T> of(
            Function<T, StatusIds.StatusCode> statusFunctor,
            Function<T, List<IssueMessage>> issuesFunctor
    ) {
        return new StatusExtractor<>(statusFunctor, issuesFunctor);
    }
}
