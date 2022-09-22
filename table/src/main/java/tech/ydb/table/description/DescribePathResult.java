package tech.ydb.table.description;

import tech.ydb.scheme.SchemeOperationProtos;


/**
 * @author Sergey Polovko
 */
public class DescribePathResult {
    private final SchemeOperationProtos.Entry self;

    protected DescribePathResult(SchemeOperationProtos.Entry self) {
        this.self = self;
    }

    public DescribePathResult(SchemeOperationProtos.DescribePathResult result) {
        this(result.getSelf());
    }

    public SchemeOperationProtos.Entry getSelf() {
        return self;
    }
}
