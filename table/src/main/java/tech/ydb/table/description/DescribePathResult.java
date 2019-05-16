package tech.ydb.table.description;

import tech.ydb.scheme.SchemeOperationProtos.Entry;


/**
 * @author Sergey Polovko
 */
public class DescribePathResult {

    private final Entry self;

    public DescribePathResult(Entry self) {
        this.self = self;
    }

    public Entry getSelf() {
        return self;
    }
}
