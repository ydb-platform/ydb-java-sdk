package tech.ydb.query.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

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
    private final List<Issue> issues;
    private final List<ResultSetReader> results;

    private QueryReader(QueryInfo info, List<Issue> issues, List<ResultSetReader> results) {
        this.info = info;
        this.issues = issues;
        this.results = results;
    }

    public QueryInfo getQueryInfo() {
        return this.info;
    }

    public int getResultSetCount() {
        return results.size();
    }

    public ResultSetReader getResultSet(int index) {
        return results.get(index);
    }

    public List<Issue> getIssueList() {
        return this.issues;
    }

    public static CompletableFuture<Result<QueryReader>> readFrom(QueryStream stream) {
        final PartsCollector collector = new PartsCollector();
        return stream.execute(collector).thenApply(res -> res.map(collector::toReader));
    }

    @Override
    public Iterator<ResultSetReader> iterator() {
        return results.iterator();
    }

    private static class PartsCollector implements QueryStream.PartsHandler {
        private final List<Issue> issueList = new ArrayList<>();
        private final SortedMap<Long, List<QueryResultPart>> results = new TreeMap<>();

        QueryReader toReader(QueryInfo info) {
            List<List<QueryResultPart>> ordered = new ArrayList<>();
            long lastInserted = 0;
            for (Map.Entry<Long, List<QueryResultPart>> entry: results.entrySet()) {
                long key = entry.getKey();
                while (lastInserted + 1 < key) {
                    ordered.add(new ArrayList<>()); // add empty result for skipped indexes
                    lastInserted++;
                }
                ordered.add(entry.getValue());
                lastInserted = key;
            }

            List<ResultSetReader> resultsList = new ArrayList<>(ordered.size());
            for (List<QueryResultPart> queryResult: ordered) {
                resultsList.add(new CompositeResultSet(queryResult));
            }

            return new QueryReader(info, issueList, resultsList);
        }

        @Override
        public void onIssues(Issue[] issues) {
            this.issueList.addAll(Arrays.asList(issues));
        }

        @Override
        public void onNextPart(QueryResultPart part) {
            Long index = part.getResultSetIndex();
            if (!results.containsKey(index)) {
                results.put(index, new ArrayList<>());
            }
            results.get(index).add(part);
        }
    }

    private static class CompositeResultSet implements ResultSetReader {
        private final ResultSetReader[] parts;
        private final int rowsCount;
        private int partIndex = -1;

        CompositeResultSet(List<QueryResultPart> list) {
            this.parts = new ResultSetReader[list.size()];
            int count = 0;
            int idx = 0;
            for (QueryResultPart part: list) {
                this.parts[idx++] = part.getResultSetReader();
                count += part.getResultSetRowsCount();
            }
            this.rowsCount = count;
            this.partIndex = list.isEmpty() ? -1 : 0;
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
            return parts[partIndex].getColumnCount();
        }

        @Override
        public String getColumnName(int index) {
            if (partIndex < 0) {
                return null;
            }
            return parts[partIndex].getColumnName(index);
        }

        @Override
        public int getColumnIndex(String name) {
            if (partIndex < 0) {
                return -1;
            }
            return parts[partIndex].getColumnIndex(name);
        }

        @Override
        public ValueReader getColumn(int index) {
            if (partIndex < 0) {
                return null;
            }
            return parts[partIndex].getColumn(index);
        }

        @Override
        public ValueReader getColumn(String name) {
            if (partIndex < 0) {
                return null;
            }
            return parts[partIndex].getColumn(name);
        }

        @Override
        public Type getColumnType(int index) {
            if (partIndex < 0) {
                return null;
            }
            return parts[partIndex].getColumnType(index);
        }

        @Override
        public int getRowCount() {
            return rowsCount;
        }

        @Override
        public void setRowIndex(int index) {
            // TODO: Enable after JDBC fixing
//            if (index < 0 || index >= rowsCount) {
//                throw new IndexOutOfBoundsException(String.format("Index %s out of bounds for length %s",
//                        index, rowsCount));
//            }
//            int currentIdx = index;
            int currentIdx = Math.max(0, index);
            partIndex = 0;
            while (partIndex < parts.length) {
                int readerRows = parts[partIndex].getRowCount();
                if (currentIdx < readerRows) {
                    parts[partIndex].setRowIndex(currentIdx);
                    break;
                }
                parts[partIndex].setRowIndex(readerRows);
                currentIdx -= readerRows;
                partIndex++;
            }

            // TODO: remove after JDBC fixing
            if (partIndex >= parts.length) {
                partIndex = parts.length - 1;
            }

            for (int partStep = partIndex + 1; partStep < parts.length; partStep++) {
                parts[partStep].setRowIndex(0);
            }
        }

        @Override
        public boolean next() {
            if (partIndex < 0) {
                return false;
            }
            boolean res = parts[partIndex].next();
            while (!res && partIndex < parts.length - 1) {
                partIndex++;
                res = parts[partIndex].next();
            }
            return res;
        }
    }
}
