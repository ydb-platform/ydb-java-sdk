package ru.yandex.ydb.table.settings;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ru.yandex.ydb.table.types.Type;


/**
 * @author Sergey Polovko
 */
public class AlterTableSettings extends RequestSettings<AlterTableSettings> {

    private LinkedHashMap<String, Type> addColumns = new LinkedHashMap<>();
    private HashSet<String> dropColumns = new HashSet<>();

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

    public void forEachAddColumn(BiConsumer<String, Type> fn) {
        for (Map.Entry<String, Type> e : addColumns.entrySet()) {
            fn.accept(e.getKey(), e.getValue());
        }
    }

    public void forEachDropColumn(Consumer<String> fn) {
        dropColumns.forEach(fn);
    }
}
