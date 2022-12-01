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

import tech.ydb.table.description.TableTtl.TtlMode;
import tech.ydb.table.settings.PartitioningSettings;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.Type;

/**
 * @author Sergey Polovko
 */
public class TableDescription {

    private final List<String> primaryKeys;
    private final List<TableColumn> columns;
    private final List<TableIndex> indexes;
    private final List<ColumnFamily> columnFamilies;
    private final List<KeyRange> keyRanges;

    @Nullable
    private final TableStats tableStats;
    @Nullable
    private final PartitioningSettings partitioningSettings;

    private final List<PartitionStats> partitionStats;

    private final TableTtl tableTtl;

    private TableDescription(Builder builder) {
        this.primaryKeys = ImmutableList.copyOf(builder.primaryKeys);
        this.columns = builder.buildColumns();
        this.indexes = ImmutableList.copyOf(builder.indexes);
        this.columnFamilies = ImmutableList.copyOf(builder.families);
        this.keyRanges = ImmutableList.copyOf(builder.keyRanges);

        this.tableStats = builder.tableStats;
        this.partitioningSettings = builder.partitioningSettings;
        this.partitionStats = ImmutableList.copyOf(builder.partitionStats);
        this.tableTtl = builder.ttlSettings;
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
    public PartitioningSettings getPartitioningSettings() {
        return partitioningSettings;
    }

    @Nullable
    public TableStats getTableStats() {
        return tableStats;
    }

    public List<PartitionStats> getPartitionStats() {
        return partitionStats;
    }

    public List<ColumnFamily> getColumnFamilies() {
        return columnFamilies;
    }

    public List<KeyRange> getKeyRanges() {
        return keyRanges;
    }

    public TableTtl getTableTtl() {
        return tableTtl;
    }

    /**
     * BUILDER
     */
    public static class Builder {

        private List<String> primaryKeys = Collections.emptyList();
        private final LinkedHashMap<String, TypeAndFamily> columns = new LinkedHashMap<>();
        private final List<TableIndex> indexes = new ArrayList<>();
        private final List<ColumnFamily> families = new ArrayList<>();
        private final List<KeyRange> keyRanges = new ArrayList<>();

        private TableStats tableStats = null;
        private PartitioningSettings partitioningSettings = null;
        private final List<PartitionStats> partitionStats = new ArrayList<>();
        private TableTtl ttlSettings = new TableTtl();

        public Builder addNonnullColumn(String name, Type type) {
            return addNonnullColumn(name, type, null);
        }

        public Builder addNonnullColumn(String name, Type type, String family) {
            columns.put(name, new TypeAndFamily(type, family));
            return this;
        }

        public Builder addKeyRange(KeyRange value) {
            keyRanges.add(value);
            return this;
        }

        public Builder addNullableColumn(String name, Type type) {
            return addNullableColumn(name, type, null);
        }

        public Builder addNullableColumn(String name, Type type, String family) {
            columns.put(name, new TypeAndFamily(OptionalType.of(type), family));
            return this;
        }

        public Builder addNotNullColumn(String name, Type type) {
            return addNotNullColumn(name, type, null);
        }

        public Builder addNotNullColumn(String name, Type type, String family) {
            columns.put(name, new TypeAndFamily(type, family));
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

            HashSet<String> keys = new HashSet<>(names.length);
            for (String name : names) {
                checkColumnKnown(name);
                if (!keys.add(name)) {
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

            HashSet<String> keys = new HashSet<>(names.size());
            for (String name : names) {
                checkColumnKnown(name);
                if (!keys.add(name)) {
                    throw new IllegalArgumentException("non unique primary column name: " + name);
                }
            }

            this.primaryKeys = ImmutableList.copyOf(names);
            return this;
        }

        public Builder addGlobalIndex(String name, List<String> columns) {
            indexes.add(new TableIndex(name, columns, TableIndex.Type.GLOBAL));
            return this;
        }

        public Builder addGlobalIndex(String name, List<String> columns, List<String> dataColumns) {
            indexes.add(new TableIndex(name, columns, dataColumns, TableIndex.Type.GLOBAL));
            return this;
        }

        public Builder setTableStats(TableStats tableStats) {
            this.tableStats = tableStats;
            return this;
        }

        public Builder setPartitioningSettings(PartitioningSettings partitioningSettings) {
            this.partitioningSettings = partitioningSettings;
            return this;
        }

        public Builder addColumnFamily(ColumnFamily family) {
            this.families.add(family);
            return this;
        }

        public Builder addPartitionStat(long rows, long size) {
            this.partitionStats.add(new PartitionStats(rows, size));
            return this;
        }

        public Builder setTtlSettings(int ttlModeCase, String columnName, int expireAfterSeconds) {
            this.ttlSettings = new TableTtl(TtlMode.forCase(ttlModeCase), columnName, expireAfterSeconds);
            return this;
        }

        private List<TableColumn> buildColumns() {
            if (columns.isEmpty()) {
                throw new IllegalStateException("cannot build table description with no columns");
            }

            int i = 0;
            TableColumn[] array = new TableColumn[this.columns.size()];
            for (Map.Entry<String, Builder.TypeAndFamily> e : this.columns.entrySet()) {
                array[i++] = new TableColumn(e.getKey(), e.getValue().type, e.getValue().family);
            }

            return ImmutableList.copyOf(array);
        }


        public TableDescription build() {
            return new TableDescription(this);
        }

        private void checkColumnKnown(String name) {
            if (!columns.containsKey(name)) {
                throw new IllegalArgumentException("unknown column name: " + name);
            }
        }

        private static class TypeAndFamily {
            private final Type type;
            private final String family;

            TypeAndFamily(Type type, String family) {
                this.type = type;
                this.family = family;
            }
        }
    }

    public static class PartitionStats {
        private final long rowsEstimate;
        private final long storeSize;

        public PartitionStats(long rowsEstimate, long storeSize) {
            this.rowsEstimate = rowsEstimate;
            this.storeSize = storeSize;
        }

        public long rowsEstimate() {
            return this.rowsEstimate;
        }

        public long storeSize() {
            return this.storeSize;
        }
    }

    public static class TableStats {
        @Nullable
        private final Instant creationTime;
        @Nullable
        private final Instant modificationTime;
        private final long rowsEstimate;
        private final long storeSize;

        public TableStats(@Nullable Instant creationTime, @Nullable Instant modificationTime,
                long rowsEstimate, long storeSize) {
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
