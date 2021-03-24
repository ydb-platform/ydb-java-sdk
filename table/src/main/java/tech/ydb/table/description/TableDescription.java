package tech.ydb.table.description;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.Type;

/**
 * @author Sergey Polovko
 */
public class TableDescription {

    private final List<String> primaryKeys;
    private final List<TableColumn> columns;
    private final List<TableIndex> indexes;
    @Nullable
    private final TableStats tableStats;
    private final List<ColumnFamily> columnFamilies;


    private TableDescription(List<String> primaryKeys, List<TableColumn> columns, List<TableIndex> indexes, TableStats tableStats, List<ColumnFamily> columnFamilies) {
        this.primaryKeys = primaryKeys;
        this.columns = columns;
        this.indexes = indexes;
        this.tableStats = tableStats;
        this.columnFamilies = columnFamilies;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public List<TableColumn> getColumns() {
        return columns;
    }

    public List<TableIndex> getIndexes() {
        return indexes;
    }

    @Nullable
    public TableStats getTableStats() {
        return tableStats;
    }

    public List<ColumnFamily> getColumnFamilies() {
        return columnFamilies;
    }

    /**
     * BUILDER
     */
    public static class Builder {

        private List<String> primaryKeys = Collections.emptyList();
        private LinkedHashMap<String, TypeAndFamily> columns = new LinkedHashMap<>();
        private List<TableIndex> indexes = Collections.emptyList();
        private TableStats tableStats;
        private List<ColumnFamily> families = Collections.emptyList();

        public Builder addNonnullColumn(String name, Type type) {
            return addNonnullColumn(name, type, null);
        }

        public Builder addNonnullColumn(String name, Type type, String family) {
            columns.put(name, new TypeAndFamily(type, family));
            return this;
        }

        public Builder addNullableColumn(String name, Type type) {
            return addNullableColumn(name, type, null);
        }
        public Builder addNullableColumn(String name, Type type, String family) {
            columns.put(name, new TypeAndFamily(OptionalType.of(type), family));
            return this;
        }

        public Builder setPrimaryKey(String name) {
            checkColumnKnown(name);
            primaryKeys = ImmutableList.of(name);
            return this;
        }

        public Builder setPrimaryKeys(String... names) {
            if (names.length == 1) {
                return setPrimaryKey(names[0]);
            }

            HashSet<String> primaryKeys = new HashSet<>(names.length);
            for (String name : names) {
                checkColumnKnown(name);
                if (!primaryKeys.add(name)) {
                    throw new IllegalArgumentException("non unique primary column name: " + name);
                }
            }

            this.primaryKeys = ImmutableList.copyOf(names);
            return this;
        }

        public Builder setPrimaryKeys(List<String> names) {
            if (names.size() == 1) {
                return setPrimaryKey(names.get(0));
            }

            HashSet<String> primaryKeys = new HashSet<>(names.size());
            for (String name : names) {
                checkColumnKnown(name);
                if (!primaryKeys.add(name)) {
                    throw new IllegalArgumentException("non unique primary column name: " + name);
                }
            }

            this.primaryKeys = ImmutableList.copyOf(names);
            return this;
        }

        public Builder addGlobalIndex(String name, List<String> columns) {
            if (indexes.isEmpty()) {
                indexes = new ArrayList<>(1);
            }
            indexes.add(new TableIndex(name, columns, TableIndex.Type.GLOBAL));
            return this;
        }

        public Builder tableStats(TableStats tableStats) {
            this.tableStats = tableStats;
            return this;
        }

        public Builder addColumnFamily(ColumnFamily family) {
            if (families.isEmpty()) {
                families = new ArrayList<>();
            }
            this.families.add(family);
            return this;
        }

        public TableDescription build() {
            if (columns.isEmpty()) {
                throw new IllegalStateException("cannot build table description with no columns");
            }

            int i = 0;
            TableColumn[] columns = new TableColumn[this.columns.size()];
            for (Map.Entry<String, TypeAndFamily> e : this.columns.entrySet()) {
                columns[i++] = new TableColumn(e.getKey(), e.getValue().type, e.getValue().family);
            }

            return new TableDescription(primaryKeys, ImmutableList.copyOf(columns), ImmutableList.copyOf(indexes), tableStats, families);
        }

        private void checkColumnKnown(String name) {
            if (!columns.containsKey(name)) {
                throw new IllegalArgumentException("unknown column name: " + name);
            }
        }

        private static class TypeAndFamily {
            private final Type type;
            private final String family;

            public TypeAndFamily(Type type, String family) {
                this.type = type;
                this.family = family;
            }
        }
    }

    public static class TableStats {
        @Nullable
        private final Instant creationTime;
        @Nullable
        private final Instant modificationTime;
        private final long rowsEstimate;
        private final long storeSize;

        public TableStats(@Nullable Instant creationTime, @Nullable Instant modificationTime, long rowsEstimate, long storeSize) {
            this.creationTime = creationTime;
            this.modificationTime = modificationTime;
            this.rowsEstimate = rowsEstimate;
            this.storeSize = storeSize;
        }

        @Nullable
        public Instant getCreationTime() {
            return creationTime;
        }

        @Nullable
        public Instant getModificationTime() {
            return modificationTime;
        }

        public long getRowsEstimate() {
            return rowsEstimate;
        }

        public long getStoreSize() {
            return storeSize;
        }

    }
}
