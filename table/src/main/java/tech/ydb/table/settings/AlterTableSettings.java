package tech.ydb.table.settings;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.Type;


/**
 * @author Sergey Polovko
 */
public class AlterTableSettings extends RequestSettings<AlterTableSettings> {

    private final Map<String, TableColumn> addColumns = new HashMap<>();
    private final Map<String, Changefeed> addChangefeeds = new HashMap<>();
    private final Map<String, TableIndex> addIndexes = new HashMap<>();

    private final Set<String> dropColumns = new HashSet<>();
    private final Set<String> dropChangefeeds = new HashSet<>();
    private final Set<String> dropIndexes = new HashSet<>();

    @Nullable
    private TtlSettings ttlSettings;
    @Nullable
    private PartitioningSettings partitioningSettings;


    public AlterTableSettings() {
    }

    @Deprecated
    public AlterTableSettings addColumn(String name, Type type) {
        addColumns.put(name, new TableColumn(name, type));
        return this;
    }

    public AlterTableSettings addNonnullColumn(String name, Type type) {
        addColumns.put(name, new TableColumn(name, type));
        return this;
    }

    public AlterTableSettings addNonnullColumn(String name, Type type, String family) {
        addColumns.put(name, new TableColumn(name, type, family));
        return this;
    }

    public AlterTableSettings addNullableColumn(String name, Type type) {
        addColumns.put(name, new TableColumn(name, OptionalType.of(type)));
        return this;
    }

    public AlterTableSettings addNullableColumn(String name, Type type, String family) {
        addColumns.put(name, new TableColumn(name, OptionalType.of(type), family));
        return this;
    }

    public AlterTableSettings dropColumn(String name) {
        dropColumns.add(name);
        return this;
    }

    public AlterTableSettings addChangefeed(Changefeed changefeed) {
        addChangefeeds.put(changefeed.getName(), changefeed);
        return this;
    }

    public AlterTableSettings dropChangefeed(String changefeed) {
        dropChangefeeds.add(changefeed);
        return this;
    }

    public AlterTableSettings addGlobalIndex(String name, List<String> columns) {
        addIndexes.put(name, new TableIndex(name, columns, TableIndex.Type.GLOBAL));
        return this;
    }

    public AlterTableSettings addGlobalIndex(String name, List<String> columns, List<String> dataColumns) {
        addIndexes.put(name, new TableIndex(name, columns, dataColumns, TableIndex.Type.GLOBAL));
        return this;
    }

    public AlterTableSettings addGlobalAsyncIndex(String name, List<String> columns) {
        addIndexes.put(name, new TableIndex(name, columns, TableIndex.Type.GLOBAL_ASYNC));
        return this;
    }

    public AlterTableSettings addGlobalAsyncIndex(String name, List<String> columns, List<String> dataColumns) {
        addIndexes.put(name, new TableIndex(name, columns, dataColumns, TableIndex.Type.GLOBAL_ASYNC));
        return this;
    }

    public AlterTableSettings dropIndex(String index) {
        dropIndexes.add(index);
        return this;
    }

    public AlterTableSettings setTtlSettings(@Nullable TtlSettings ttlSettings) {
        this.ttlSettings = ttlSettings;
        return this;
    }

    public AlterTableSettings setPartitioningSettings(@Nullable PartitioningSettings partitioningSettings) {
        this.partitioningSettings = partitioningSettings;
        return this;
    }

    public Collection<TableColumn> getAddColumns() {
        return addColumns.values();
    }

    public Collection<Changefeed> getAddChangefeeds() {
        return addChangefeeds.values();
    }

    public Collection<TableIndex> getAddIndexes() {
        return addIndexes.values();
    }

    public Collection<String> getDropColumns() {
        return dropColumns;
    }

    public Collection<String> getDropChangefeeds() {
        return dropChangefeeds;
    }

    public Collection<String> getDropIndexes() {
        return dropIndexes;
    }

    @Nullable
    public TtlSettings getTtlSettings() {
        return ttlSettings;
    }

    @Nullable
    public PartitioningSettings getPartitioningSettings() {
        return partitioningSettings;
    }

    @Deprecated
    public void forEachAddColumn(BiConsumer<String, Type> fn) {
        addColumns.values().forEach(c -> fn.accept(c.getName(), c.getType()));
    }

    @Deprecated
    public void forEachDropColumn(Consumer<String> fn) {
        dropColumns.forEach(fn);
    }

    @Deprecated
    public void forEachAddChangefeed(Consumer<Changefeed> fn) {
        addChangefeeds.values().forEach(fn);
    }

    @Deprecated
    public void forEachDropChangefeed(Consumer<String> fn) {
        dropChangefeeds.forEach(fn);
    }
}
