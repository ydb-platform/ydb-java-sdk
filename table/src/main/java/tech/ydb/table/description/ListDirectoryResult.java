package tech.ydb.table.description;

import java.util.List;

import tech.ydb.scheme.SchemeOperationProtos.Entry;


/**
 * @author Sergey Polovko
 */
public class ListDirectoryResult extends DescribePathResult {

    private final List<Entry> children;

    public ListDirectoryResult(Entry self, List<Entry> children) {
        super(self);
        this.children = children;
    }

    public List<Entry> getChildren() {
        return children;
    }
}
