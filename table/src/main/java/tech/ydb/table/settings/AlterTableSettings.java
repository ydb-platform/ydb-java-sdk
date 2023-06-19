package tech.ydb.table.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import tech.ydb.table.values.Type;


/**
 * @author Sergey Polovko
 */
public class AlterTableSettings extends RequestSettings<AlterTableSettings> {

    private LinkedHashMap<String, Type> addColumns = new LinkedHashMap<>();
    private HashSet<String> dropColumns = new HashSet<>();
    @Nullable
    private TtlSettings ttlSettings;
    @Nullable
    private PartitioningSettings partitioningSettings;

    private List<Changefeed> addChangefeeds = new ArrayList<>();
    private List<String> dropChangefeeds = new ArrayList<>();

    public AlterTableSettings() {
    }

    public AlterTableSettings addColumn(String name, Type type) {
        addColumns.put(name, type);
        return this;
    }

    public AlterTableSettings dropColumn(String name) {
        dropColumns.add(name);
        return this;
    }

    public AlterTableSettings addChangefeed(Changefeed changefeed) {
        addChangefeeds.add(changefeed);
        return this;
    }

    public AlterTableSettings dropChangefeed(String changefeed) {
        dropChangefeeds.add(changefeed);
        return this;
    }

    public void forEachAddColumn(BiConsumer<String, Type> fn) {
        addColumns.forEach(fn);
    }

    public void forEachDropColumn(Consumer<String> fn) {
        dropColumns.forEach(fn);
    }

    public void forEachAddChangefeed(Consumer<Changefeed> fn) {
        addChangefeeds.forEach(fn);
    }

    public void forEachDropChangefeed(Consumer<String> fn) {
        dropChangefeeds.forEach(fn);
    }

    @Nullable
    public TtlSettings getTtlSettings() {
        return ttlSettings;
    }

    @Nullable
    public PartitioningSettings getPartitioningSettings() {
        return partitioningSettings;
    }

    public void setTtlSettings(@Nullable TtlSettings ttlSettings) {
        this.ttlSettings = ttlSettings;
    }

    public void setPartitioningSettings(@Nullable PartitioningSettings partitioningSettings) {
        this.partitioningSettings = partitioningSettings;
    }
}
