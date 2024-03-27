package tech.ydb.table.settings;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Maksim Zinal
 */
public class RenameTablesSettings extends RequestSettings<RenameTablesSettings> {

    private final List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public RenameTablesSettings addTable(String sourcePath, String destinationPath,
            boolean replaceDestination) {
        items.add(new Item(sourcePath, destinationPath, replaceDestination));
        return this;
    }

    public RenameTablesSettings addTable(String sourcePath, String destinationPath) {
        items.add(new Item(sourcePath, destinationPath));
        return this;
    }

    public static class Item {
        private final String sourcePath;
        private final String destinationPath;
        private final boolean replaceDestination;

        public Item(String sourcePath, String destinationPath, boolean replaceDestination) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
            this.replaceDestination = replaceDestination;
        }

        public Item(String sourcePath, String destinationPath) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
            this.replaceDestination = false;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getDestinationPath() {
            return destinationPath;
        }

        public boolean isReplaceDestination() {
            return replaceDestination;
        }
    }

}
