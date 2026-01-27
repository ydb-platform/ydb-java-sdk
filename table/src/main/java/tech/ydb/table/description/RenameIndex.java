package tech.ydb.table.description;

/**
 * @author Kirill Kurdyukov
 */
public class RenameIndex {

    private final String sourceName;
    private final String destinationName;
    private final boolean replaceDestination;

    public RenameIndex(String sourceName, String destinationName, boolean replaceDestination) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.replaceDestination = replaceDestination;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public boolean isReplaceDestination() {
        return replaceDestination;
    }
}
