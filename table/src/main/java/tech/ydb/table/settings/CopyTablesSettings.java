package tech.ydb.table.settings;

import java.util.ArrayList;
import java.util.List;

import tech.ydb.core.settings.RequestSettings;

/**
 *
 * @author Maksim Zinal
 */
public class CopyTablesSettings extends RequestSettings<CopyTablesSettings> {

    private final List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public CopyTablesSettings addTable(String sourcePath, String destinationPath,
            boolean omitIndexes) {
        items.add(new Item(sourcePath, destinationPath, omitIndexes));
        return this;
    }

    public CopyTablesSettings addTable(String sourcePath, String destinationPath) {
        items.add(new Item(sourcePath, destinationPath));
        return this;
    }

    public static class Item {
        private final String sourcePath;
        private final String destinationPath;
        private final boolean omitIndexes;

        public Item(String sourcePath, String destinationPath, boolean omitIndexes) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
            this.omitIndexes = omitIndexes;
        }

        public Item(String sourcePath, String destinationPath) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
            this.omitIndexes = true;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getDestinationPath() {
            return destinationPath;
        }

        public boolean isOmitIndexes() {
            return omitIndexes;
        }
    }

}
