package tech.ydb.query.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tech.ydb.core.Result;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResponsePart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryReader implements Iterable<ResultSetReader> {
    private final QueryInfo info;
    private final List<ResultSetParts> results;

    private QueryReader(QueryInfo info, List<ResultSetParts> results) {
        this.info = info;
        this.results = results;
    }

    public QueryInfo getQueryInfo() {
        return this.info;
    }

    public int getResultSetCount() {
        return results.size();
    }

    public ResultSetReader getResultSet(int index) {
        return new CompositeResultSet(results.get(index).getPartsStream());
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
            return new CompositeResultSet(iter.next().getPartsStream());
        }
    }

    private static class PartsCollector implements QueryStream.PartsHandler {
        private final SortedMap<Long, ResultSetParts> results = new TreeMap<>();

        QueryReader toReader(QueryInfo info) {
            List<ResultSetParts> ordered = new ArrayList<>();
            long lastInserted = 0;
            for (Map.Entry<Long, ResultSetParts> entry: results.entrySet()) {
                long key = entry.getKey();
                while (lastInserted < key) {
                    ordered.add(new ResultSetParts(lastInserted));
                    lastInserted++;
                }
                ordered.add(entry.getValue());
                lastInserted = key;
            }

            return new QueryReader(info, ordered);
        }

        @Override
        public void onPart(QueryResponsePart part) {
            Long index = part.getResultSetIndex();
            if (!results.containsKey(index)) {
                results.put(index, new ResultSetParts(index));
            }
            results.get(index).addPart(part);
        }
    }

    private static class ResultSetParts {
        private final long resultSetIndex;
        private final List<QueryResponsePart> parts = new ArrayList<>();

        public ResultSetParts(long index) {
            this.resultSetIndex = index;
        }

        public void addPart(QueryResponsePart part) {
            parts.add(part);
        }

        public long getIndex() {
            return resultSetIndex;
        }

        public Stream<QueryResponsePart> getPartsStream() {
            return parts.stream();
        }
    }

    private static class CompositeResultSet implements ResultSetReader {
        private final List<ResultSetReader> parts;
        private final int rowsCount;
        private int partIndex = -1;

        public CompositeResultSet(Stream<QueryResponsePart> stream) {
            this.parts = stream.map(QueryResponsePart::getResultSetReader).collect(Collectors.toList());
            this.rowsCount = stream.mapToInt(QueryResponsePart::getResultSetRowsCount).sum();
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
                    return;
                }
                parts.get(partIndex).setRowIndex(readerRows - 1);
                currentIdx -= readerRows;
                partIndex++;
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