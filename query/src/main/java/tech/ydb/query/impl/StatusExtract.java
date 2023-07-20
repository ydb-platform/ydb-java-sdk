package tech.ydb.query.impl;

import java.util.List;
import java.util.function.Function;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.YdbIssueMessage;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <T> result class
 */
public class StatusExtract<T> implements Function<Result<T>, Result<T>> {
    private final Function<T, StatusCodesProtos.StatusIds.StatusCode> statusFunctor;
    private final Function<T, List<YdbIssueMessage.IssueMessage>> issuesFunctor;

    private StatusExtract(
            Function<T, StatusCodesProtos.StatusIds.StatusCode> statusFunctor,
            Function<T, List<YdbIssueMessage.IssueMessage>> issuesFunctor) {
        this.statusFunctor = statusFunctor;
        this.issuesFunctor = issuesFunctor;
    }

    @Override
    public Result<T> apply(Result<T> result) {
        if (!result.isSuccess()) {
            return result;
        }

        T resp = result.getValue();
        Status status = Status.of(
                StatusCode.fromProto(statusFunctor.apply(resp)),
                result.getStatus().getConsumedRu(),
                Issue.fromPb(issuesFunctor.apply(resp))
        );
        return Result.success(resp, status);
    }

    public static <T> StatusExtract<T> of(
            Function<T, StatusCodesProtos.StatusIds.StatusCode> statusFunctor,
            Function<T, List<YdbIssueMessage.IssueMessage>> issuesFunctor) {
        return new StatusExtract<>(statusFunctor, issuesFunctor);
    }
}
