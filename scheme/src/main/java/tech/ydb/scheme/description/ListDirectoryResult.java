package tech.ydb.scheme.description;

import java.util.List;

import tech.ydb.scheme.SchemeOperationProtos;

/**
 * @author Sergey Polovko
 */
public class ListDirectoryResult extends DescribePathResult {

    private final List<SchemeOperationProtos.Entry> children;

    public ListDirectoryResult(SchemeOperationProtos.ListDirectoryResult result) {
        super(result.getSelf());
        this.children = result.getChildrenList();
    }

    public List<SchemeOperationProtos.Entry> getChildren() {
        return children;
    }
}
