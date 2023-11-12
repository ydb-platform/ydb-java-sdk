package tech.ydb.query.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryDataReader {
    private final CompositeResultSet[] readers;

    private QueryDataReader(List<QueryResultPart> parts) {
        int lastIdx = -1;
        for (QueryResultPart part: parts) {
            if (part.getResultSetIndex() > lastIdx) {
                lastIdx = (int) part.getResultSetIndex();
            }
        }

        readers = new CompositeResultSet[lastIdx + 1];
        if (readers.length == 0) {
            return;
        }

        for (int idx = 0; idx < readers.length; idx += 1) {
            readers[idx] = new CompositeResultSet();
        }

        for (QueryResultPart part: parts) {
            readers[(int) part.getResultSetIndex()].addResultSet(part.getResultSetReader());
        }
    }

    public int getResultSetCount() {
        return readers.length;
    }

    public ResultSetReader getResultSet(int index) {
        return readers[index];
    }

    public static CompletableFuture<Result<QueryDataReader>> readFrom(GrpcReadStream<QueryResultPart> stream) {
        final List<QueryResultPart> cache = new ArrayList<>();
        CompletableFuture<Status> finish = stream.start(cache::add);
        return finish.thenApply(status ->
                status.isSuccess() ? Result.success(new QueryDataReader(cache), status) : Result.fail(status)
        );
    }

    private class CompositeResultSet implements ResultSetReader {
        private final List<ResultSetReader> readers = new ArrayList<>();

        private int rowCount = 0;
        private int resultSetIndex = -1;

        public void addResultSet(ResultSetReader reader) {
            rowCount += reader.getRowCount();
            resultSetIndex = 0;
            readers.add(reader);
        }

        @Override
        public boolean isTruncated() {
            return false;
        }

        @Override
        public int getColumnCount() {
            if (resultSetIndex < 0) {
                return 0;
            }
            return readers.get(resultSetIndex).getColumnCount();
        }

        @Override
        public String getColumnName(int index) {
            if (resultSetIndex < 0) {
                return null;
            }
            return readers.get(resultSetIndex).getColumnName(index);
        }

        @Override
        public int getColumnIndex(String name) {
            if (resultSetIndex < 0) {
                return -1;
            }
            return readers.get(resultSetIndex).getColumnIndex(name);
        }

        @Override
        public ValueReader getColumn(int index) {
            if (resultSetIndex < 0) {
                return null;
            }
            return readers.get(resultSetIndex).getColumn(index);
        }

        @Override
        public ValueReader getColumn(String name) {
            if (resultSetIndex < 0) {
                return null;
            }
            return readers.get(resultSetIndex).getColumn(name);
        }

        @Override
        public Type getColumnType(int index) {
            if (resultSetIndex < 0) {
                return null;
            }
            return readers.get(resultSetIndex).getColumnType(index);
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public void setRowIndex(int index) {
            resultSetIndex = 0;
            int currentIdx = index;
            while (resultSetIndex < readers.size()) {
                int readerRows = readers.get(resultSetIndex).getRowCount();
                if (currentIdx < readerRows) {
                    readers.get(resultSetIndex).setRowIndex(currentIdx);
                    return;
                }
                readers.get(resultSetIndex).setRowIndex(readerRows - 1);
                currentIdx -= readerRows;
                resultSetIndex++;
            }
        }

        @Override
        public boolean next() {
            if (resultSetIndex < 0) {
                return false;
            }
            boolean res = readers.get(resultSetIndex).next();
            while (!res && resultSetIndex < readers.size() - 1) {
                resultSetIndex++;
                res = readers.get(resultSetIndex).next();
            }
            return res;
        }
    }
}
