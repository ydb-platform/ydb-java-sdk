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
import com.google.common.collect.Sets;

import tech.ydb.table.Session;
import tech.ydb.table.description.TableTtl.TtlMode;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.PartitioningSettings;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;

/**
 * @author Sergey Polovko
 */
public class TableDescription {
    public enum StoreType {
        ROW,
        COLUMN
    }

    private final StoreType storeType;
    private final List<String> primaryKeys;
    private final List<TableColumn> columns;
    private final List<TableIndex> indexes;
    private final List<ColumnFamily> columnFamilies;
    private final List<KeyRange> keyRanges;
    private final List<ChangefeedDescription> changefeeds;

    @Nullable
    private final TableStats tableStats;
    @Nullable
    private final PartitioningSettings partitioningSettings;

    private final List<PartitionStats> partitionStats;

    private final TableTtl tableTtl;

    private TableDescription(Builder builder) {
        this.storeType = builder.storeType;
        this.primaryKeys = ImmutableList.copyOf(builder.primaryKeys);
        this.columns = builder.buildColumns();
        this.indexes = ImmutableList.copyOf(builder.indexes);
        this.columnFamilies = ImmutableList.copyOf(builder.families);
        this.keyRanges = ImmutableList.copyOf(builder.keyRanges);

        this.tableStats = builder.tableStats;
        this.partitioningSettings = builder.partitioningSettings;
        this.partitionStats = ImmutableList.copyOf(builder.partitionStats);
        this.tableTtl = builder.ttlSettings;
        this.changefeeds = builder.changefeeds;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public StoreType getStoreType() {
        return storeType;
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

    public List<ChangefeedDescription> getChangefeeds() {
        return changefeeds;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private StoreType storeType = StoreType.ROW;
        private List<String> primaryKeys = Collections.emptyList();
        private final Map<String, TableColumn> columns = new LinkedHashMap<>();
        private final List<TableIndex> indexes = new ArrayList<>();
        private final List<ColumnFamily> families = new ArrayList<>();
        private final List<KeyRange> keyRanges = new ArrayList<>();

        private TableStats tableStats = null;
        private PartitioningSettings partitioningSettings = null;
        private final List<PartitionStats> partitionStats = new ArrayList<>();
        private TableTtl ttlSettings = TableTtl.notSet();
        private final List<ChangefeedDescription> changefeeds = new ArrayList<>();

        public Builder setStoreType(StoreType storeType) {
            this.storeType = storeType;
            return this;
        }

        public Builder addColumn(TableColumn column) {
            columns.put(column.getName(), column);
            return this;
        }

        public Builder addNonnullColumn(String name, Type type) {
            return addNonnullColumn(name, type, (String) null);
        }

        public Builder addNonnullColumn(String name, Type type, String family) {
            columns.put(name, new TableColumn(name, type, family));
            return this;
        }

        public Builder addColumn(String name, Type type, PrimitiveValue defaultValue) {
            columns.put(name, new TableColumn(name, type, null, defaultValue));
            return this;
        }

        public Builder addColumn(String name, Type type, String family, PrimitiveValue defaultValue) {
            columns.put(name, new TableColumn(name, type, family, defaultValue));
            return this;
        }

        public Builder addSequenceColumn(String name, Type type) {
            return addSequenceColumn(name, type, null, SequenceDescription.newBuilder().build());
        }

        public Builder addSequenceColumn(String name, Type type, String family) {
            return addSequenceColumn(name, type, family, SequenceDescription.newBuilder().build());
        }

        public Builder addSequenceColumn(
                String name,
                Type type,
                String family,
                SequenceDescription sequenceDescription
        ) {
            if (type instanceof PrimitiveType) {
                PrimitiveType primitiveType = (PrimitiveType) type;

                switch (primitiveType) {
                    case Int16:
                        columns.put(name, new TableColumn(name, PrimitiveType.Int16, family, sequenceDescription));
                        return this;
                    case Int32:
                        columns.put(name, new TableColumn(name, PrimitiveType.Int32, family, sequenceDescription));
                        return this;
                    case Int64:
                        columns.put(name, new TableColumn(name, PrimitiveType.Int64, family, sequenceDescription));
                        return this;
                    default:
                }
            }

            throw new IllegalArgumentException("Type " + type + " cannot be used as a sequence column");
        }

        public Builder addSmallSerialColumn(String name) {
            return addSmallSerialColumn(name, null, SequenceDescription.newBuilder().build());
        }

        public Builder addSerialColumn(String name) {
            return addSerialColumn(name, null, SequenceDescription.newBuilder().build());
        }

        public Builder addBigSerialColumn(String name) {
            return addBigSerialColumn(name, null, SequenceDescription.newBuilder().build());
        }

        public Builder addSmallSerialColumn(String name, SequenceDescription sequenceDescription) {
            return addSmallSerialColumn(name, null, sequenceDescription);
        }

        public Builder addSerialColumn(String name, SequenceDescription sequenceDescription) {
            return addSerialColumn(name, null, sequenceDescription);
        }

        public Builder addBigSerialColumn(String name, SequenceDescription sequenceDescription) {
            return addBigSerialColumn(name, null, sequenceDescription);
        }

        public Builder addSmallSerialColumn(String name, String family, SequenceDescription sequenceDescription) {
            return addSequenceColumn(name, PrimitiveType.Int16, family, sequenceDescription);
        }

        public Builder addSerialColumn(String name, String family, SequenceDescription sequenceDescription) {
            return addSequenceColumn(name, PrimitiveType.Int32, family, sequenceDescription);
        }

        public Builder addBigSerialColumn(String name, String family, SequenceDescription sequenceDescription) {
            return addSequenceColumn(name, PrimitiveType.Int64, family, sequenceDescription);
        }

        public Builder addKeyRange(KeyRange value) {
            keyRanges.add(value);
            return this;
        }

        public Builder addNullableColumn(String name, Type type) {
            return addNullableColumn(name, type, null);
        }

        public Builder addNullableColumn(String name, Type type, String family) {
            columns.put(name, new TableColumn(name, OptionalType.of(type), family));
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

            HashSet<String> keys = Sets.newHashSetWithExpectedSize(names.length);
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

            HashSet<String> keys = Sets.newHashSetWithExpectedSize(names.size());
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

        public Builder addGlobalUniqueIndex(String name, List<String> columns) {
            indexes.add(new TableIndex(name, columns, TableIndex.Type.GLOBAL_UNIQUE));
            return this;
        }

        public Builder addGlobalUniqueIndex(String name, List<String> columns, List<String> dataColumns) {
            indexes.add(new TableIndex(name, columns, dataColumns, TableIndex.Type.GLOBAL_UNIQUE));
            return this;
        }

        public Builder addGlobalAsyncIndex(String name, List<String> columns) {
            indexes.add(new TableIndex(name, columns, TableIndex.Type.GLOBAL_ASYNC));
            return this;
        }

        public Builder addGlobalAsyncIndex(String name, List<String> columns, List<String> dataColumns) {
            indexes.add(new TableIndex(name, columns, dataColumns, TableIndex.Type.GLOBAL_ASYNC));
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

        /**
         * Adds changefeed information to table description<br>
         * <b>NOTICE:</b> Changefeed description cannot be used for changefeed creating.<br>
         * It is impossible to create changefeed in one time with table creating. For adding a new changefeed use method
         * {@link Session#alterTable(java.lang.String, tech.ydb.table.settings.AlterTableSettings)} and
         * {@link AlterTableSettings#addChangefeed(tech.ydb.table.settings.Changefeed)}
         *
         * @param changefeed changefeed description
         * @return table description builder
         */
        public Builder addChangefeed(ChangefeedDescription changefeed) {
            this.changefeeds.add(changefeed);
            return this;
        }

        @Deprecated
        public Builder setTtlSettings(int ttlModeCase, String columnName, int expireAfterSeconds) {
            this.ttlSettings = new TableTtl(TtlMode.forCase(ttlModeCase), columnName, expireAfterSeconds);
            return this;
        }

        public Builder setTtlSettings(TableTtl ttl) {
            this.ttlSettings = ttl;
            return this;
        }

        private List<TableColumn> buildColumns() {
            if (columns.isEmpty()) {
                throw new IllegalStateException("cannot build table description with no columns");
            }

            int i = 0;
            TableColumn[] array = new TableColumn[this.columns.size()];
            for (Map.Entry<String, TableColumn> e : this.columns.entrySet()) {
                array[i++] = e.getValue();
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
