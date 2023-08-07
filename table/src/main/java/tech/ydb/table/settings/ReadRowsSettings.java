package tech.ydb.table.settings;

import java.time.Duration;
import java.util.List;

public class ReadRowsSettings {
    private final List<String> columns;
    private final Duration timeout;

    public ReadRowsSettings(List<String> columns, Duration timeout) {
        this.columns = columns;
        this.timeout = timeout;
    }

    public void addColumn(String column) {
        columns.add(column);
    }

    public List<String> getColumns() {
        return columns;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
