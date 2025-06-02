package tech.ydb.query.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryReader implements Iterable<ResultSetReader> {
    private final QueryInfo info;
    private final List<Issue> isssues;
    private final List<ResultSetParts> results;

    QueryReader(QueryInfo info, List<Issue> issues, List<ResultSetParts> results) {
        this.info = info;
        this.isssues = issues;
        this.results = results;
    }

    public QueryInfo getQueryInfo() {
        return this.info;
    }

    public int getResultSetCount() {
        return results.size();
    }

    public ResultSetReader getResultSet(int index) {
        return new CompositeResultSet(results.get(index).getParts());
    }

    public List<Issue> getIssueList() {
        return this.isssues;
    }

    public static CompletableFuture<Result<QueryReader>> readFrom(QueryStream stream) {
        final PartsCollector collector = new PartsCollector();
        return stream.execute(collector).thenApply(res -> res.map(collector::toReader));
    }

    @Override
    public Iterator<ResultSetReader> iterator() {
        return new IteratorImpl(results.iterator());
    }

    private class IteratorImpl implements Iterator<ResultSetReader> {
        private final Iterator<ResultSetParts> iter;

        IteratorImpl(Iterator<ResultSetParts> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public ResultSetReader next() {
            return new CompositeResultSet(iter.next().getParts());
        }
    }

    private static class PartsCollector implements QueryStream.PartsHandler {
        private final List<Issue> issueList = new ArrayList<>();
        private final SortedMap<Long, ResultSetParts> results = new TreeMap<>();

        QueryReader toReader(QueryInfo info) {
            List<ResultSetParts> ordered = new ArrayList<>();
            long lastInserted = 0;
            for (Map.Entry<Long, ResultSetParts> entry: results.entrySet()) {
                long key = entry.getKey();
                while (lastInserted + 1 < key) {
                    ordered.add(new ResultSetParts(lastInserted));
                    lastInserted++;
                }
                ordered.add(entry.getValue());
                lastInserted = key;
            }

            return new QueryReader(info, issueList, ordered);
        }

        @Override
        public void onIssues(Issue[] issues) {
            this.issueList.addAll(Arrays.asList(issues));
        }

        @Override
        public void onNextPart(QueryResultPart part) {
            Long index = part.getResultSetIndex();
            if (!results.containsKey(index)) {
                results.put(index, new ResultSetParts(index));
            }
            results.get(index).addPart(part);
        }
    }

    static class ResultSetParts {
        private final long resultSetIndex;
        private final List<QueryResultPart> parts = new ArrayList<>();

        ResultSetParts(long index) {
            this.resultSetIndex = index;
        }

        public void addPart(QueryResultPart part) {
            parts.add(part);
        }

        public long getIndex() {
            return resultSetIndex;
        }

        public List<QueryResultPart> getParts() {
            return parts;
        }
    }

    private static class CompositeResultSet implements ResultSetReader {
        private final List<ResultSetReader> parts;
        private final int rowsCount;
        private int partIndex = -1;

        CompositeResultSet(List<QueryResultPart> list) {
            this.parts = list.stream().map(QueryResultPart::getResultSetReader).collect(Collectors.toList());
            this.rowsCount = list.stream().mapToInt(QueryResultPart::getResultSetRowsCount).sum();
            this.partIndex = parts.isEmpty() ? -1 : 0;
        }

        @Override
        public boolean isTruncated() {
            return false;
        }

        @Override
        public int getColumnCount() {
            if (partIndex < 0) {
                return 0;
            }
            return parts.get(partIndex).getColumnCount();
        }

        @Override
        public String getColumnName(int index) {
            if (partIndex < 0) {
                return null;
            }
            return parts.get(partIndex).getColumnName(index);
        }

        @Override
        public int getColumnIndex(String name) {
            if (partIndex < 0) {
                return -1;
            }
            return parts.get(partIndex).getColumnIndex(name);
        }

        @Override
        public ValueReader getColumn(int index) {
            if (partIndex < 0) {
                return null;
            }
            return parts.get(partIndex).getColumn(index);
        }

        @Override
        public ValueReader getColumn(String name) {
            if (partIndex < 0) {
                return null;
            }
            return parts.get(partIndex).getColumn(name);
        }

        @Override
        public Type getColumnType(int index) {
            if (partIndex < 0) {
                return null;
            }
            return parts.get(partIndex).getColumnType(index);
        }

        @Override
        public int getRowCount() {
            return rowsCount;
        }

        @Override
        public void setRowIndex(int index) {
            partIndex = 0;
            int currentIdx = index;
            while (partIndex < parts.size()) {
                int readerRows = parts.get(partIndex).getRowCount();
                if (currentIdx < readerRows) {
                    parts.get(partIndex).setRowIndex(currentIdx);
                    break;
                }
                parts.get(partIndex).setRowIndex(readerRows - 1);
                currentIdx -= readerRows;
                partIndex++;
            }
            for (int partStep = partIndex + 1; partStep < parts.size(); partStep++) {
                parts.get(partStep).setRowIndex(0);
            }
        }

        @Override
        public boolean next() {
            if (partIndex < 0) {
                return false;
            }
            boolean res = parts.get(partIndex).next();
            while (!res && partIndex < parts.size() - 1) {
                partIndex++;
                res = parts.get(partIndex).next();
            }
            return res;
        }
    }
}
