package tech.ydb.scheme.description;

import java.util.List;
import java.util.stream.Collectors;

import tech.ydb.proto.scheme.SchemeOperationProtos;

/**
 * @author Sergey Polovko
 */
public class ListDirectoryResult extends DescribePathResult {

    private final List<SchemeOperationProtos.Entry> children;
    private final List<Entry> entryChildren;

    public ListDirectoryResult(SchemeOperationProtos.ListDirectoryResult result) {
        super(result.getSelf());
        this.children = result.getChildrenList();
        this.entryChildren = result.getChildrenList().stream().map(Entry::new).collect(Collectors.toList());
    }

    @Deprecated
    public List<SchemeOperationProtos.Entry> getChildren() {
        return children;
    }

    public List<Entry> getEntryChildren() {
        return entryChildren;
    }
}
