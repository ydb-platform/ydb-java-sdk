package tech.ydb.export.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import tech.ydb.core.settings.LongOperationSettings;

/**
 * @author Kirill Kurdyukov
 */
public class ExportToYtSettings extends LongOperationSettings {

    private final Integer port;
    private final List<Item> itemList;
    private final String description;
    private final Integer numberOfRetries;
    private final Boolean useTypeV3;

    public ExportToYtSettings(
            Builder b
    ) {
        super(b);
        this.port = b.port;
        this.itemList = b.itemList;
        this.description = b.description;
        this.numberOfRetries = b.numberOfRetries;
        this.useTypeV3 = b.useTypeV3;
    }

    public Integer getPort() {
        return port;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public String getDescription() {
        return description;
    }

    public Integer getNumberOfRetries() {
        return numberOfRetries;
    }

    public Boolean getUseTypeV3() {
        return useTypeV3;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends LongOperationBuilder<Builder> {
        private Integer port = null;
        private final List<Item> itemList = new ArrayList<>();
        private String description = null;
        private Integer numberOfRetries = null;
        private Boolean useTypeV3 = null;

        public Builder setPort(Integer port) {
            this.port = port;

            return this;
        }

        public Builder addItem(String sourcePath, String destinationPath) {
            Preconditions.checkArgument(
                    sourcePath != null && destinationPath != null,
                    "Item params aren't null!"
            );
            itemList.add(new Item(sourcePath, destinationPath));

            return this;
        }

        public Builder setDescription(String description) {
            Preconditions.checkArgument(
                    description != null && description.length() <= 128,
                    "From proto (length).le = 128"
            );
            this.description = description;

            return this;
        }

        public Builder setNumberOfRetries(Integer numberOfRetries) {
            this.numberOfRetries = numberOfRetries;

            return this;
        }

        public Builder setUseTypeV3(Boolean useTypeV3) {
            this.useTypeV3 = useTypeV3;

            return this;
        }

        public ExportToYtSettings build() {
            Preconditions.checkArgument(!itemList.isEmpty(), "Item list must be not empty!");

            return new ExportToYtSettings(this);
        }
    }

    public static class Item {
        /**
         * Database path to a table to be exported
         */
        private final String sourcePath;
        private final String destinationPath;


        private Item(String sourcePath, String destinationPrefix) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPrefix;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getDestinationPath() {
            return destinationPath;
        }
    }
}
