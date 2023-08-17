package tech.ydb.core;

import java.util.List;
import java.util.function.Function;

import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.YdbIssueMessage.IssueMessage;

public class StatusExtractor<R> implements Function<Result<R>, Result<R>> {
    private final Function<R, StatusIds.StatusCode> statusCodeExtractor;
    private final Function<R, List<IssueMessage>> issueListExtractor;

    public StatusExtractor(Function<R, StatusIds.StatusCode> statusCodeExtractor,
                           Function<R, List<IssueMessage>> issueListExtractor) {
        this.statusCodeExtractor = statusCodeExtractor;
        this.issueListExtractor = issueListExtractor;
    }

    public Function<R, StatusIds.StatusCode> getStatusCodeExtractor() {
        return statusCodeExtractor;
    }

    public Function<R, List<IssueMessage>> getIssueListExtractor() {
        return issueListExtractor;
    }

    @Override
    public Result<R> apply(Result<R> result) {
        if (!result.isSuccess()) {
            return result;
        }
        final Status status = Status.of(
            StatusCode.fromProto(statusCodeExtractor.apply(result.getValue())),
                result.getStatus().getConsumedRu(),
                Issue.fromPb(issueListExtractor.apply(result.getValue()))
        );
        return Result.success(result.getValue(), status);
    }
}
