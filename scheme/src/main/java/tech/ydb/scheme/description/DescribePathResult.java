package tech.ydb.scheme.description;

import tech.ydb.proto.scheme.SchemeOperationProtos;


/**
 * @author Sergey Polovko
 */
public class DescribePathResult {
    private final SchemeOperationProtos.Entry self;
    private final Entry entry;

    protected DescribePathResult(SchemeOperationProtos.Entry self) {
        this.self = self;
        this.entry = new Entry(self);
    }

    public DescribePathResult(SchemeOperationProtos.DescribePathResult result) {
        this(result.getSelf());
    }

    @Deprecated
    public SchemeOperationProtos.Entry getSelf() {
        return self;
    }

    public Entry getEntry() {
        return this.entry;
    }
}
