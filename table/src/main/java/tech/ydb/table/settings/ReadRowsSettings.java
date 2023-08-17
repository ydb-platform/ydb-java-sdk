package tech.ydb.table.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.table.values.StructValue;

public class ReadRowsSettings extends BaseRequestSettings {
    private final List<String> columns;
    private final List<StructValue> keys;

    protected ReadRowsSettings(ReadRowsSettingsBuilder builder) {
        super(builder);
        this.columns = builder.columns;
        this.keys = builder.keys;
    }

    public static ReadRowsSettingsBuilder newBuilder() {
        return new ReadRowsSettingsBuilder();
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<StructValue> getKeys() {
        return keys;
    }

    public static class ReadRowsSettingsBuilder extends BaseBuilder<ReadRowsSettingsBuilder> {
        private final List<String> columns = new ArrayList<>();
        private final List<StructValue> keys = new ArrayList<>();

        protected ReadRowsSettingsBuilder() {
        }

        public ReadRowsSettingsBuilder addColumns(List<String> columns) {
            this.columns.addAll(Objects.requireNonNull(columns, "null is unsupported value in" +
                    " `withColumns(List<String> columns)`"));
            return self();
        }

        public ReadRowsSettingsBuilder addColumns(String... columns) {
            return addColumns(Arrays.asList(columns));
        }

        public ReadRowsSettingsBuilder addColumn(String column) {
            columns.add(Objects.requireNonNull(column, "null is unsupported value in" +
                    " `addColumn(String column)`"));
            return self();
        }

        /**
         *      Keys must be a list of structs where each struct is a primary key
         *      for one requested row and should contain all key columns
         */
        public ReadRowsSettingsBuilder addKeys(List<StructValue> keys) {
            this.keys.addAll(Objects.requireNonNull(keys, "null is unsupported value in" +
                    " `withKeys(List<StructValue> keys)`"));
            return self();
        }

        /**
         *      Keys must be a list of structs where each struct is a primary key
         *      for one requested row and should contain all key columns
         */
        public ReadRowsSettingsBuilder addKeys(StructValue... keys) {
            return addKeys(Arrays.asList(keys));
        }

        /**
         *      Key should contain all primary key columns
         */
        public ReadRowsSettingsBuilder addKey(StructValue key) {
            keys.add(Objects.requireNonNull(key, "null is unsupported value in" +
                    " `addKey(StructValue key)`"));
            return self();
        }

        @Override
        public ReadRowsSettings build() {
            return new ReadRowsSettings(this);
        }
    }
}
