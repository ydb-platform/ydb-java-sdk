package tech.ydb.export.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import tech.ydb.core.operation.OperationMode;
import tech.ydb.core.settings.OperationSettings;
import tech.ydb.export.YdbExport;

/**
 * @author Kirill Kurdyukov
 */
public class ExportToS3Settings extends OperationSettings {

    private final Schema schema;
    private final Integer numberOfRetries;
    private final StorageClass storageClass;
    private final String compression;
    private final String region;
    private final String description;
    private final List<Item> itemList;
    private final OperationMode operationMode;

    private ExportToS3Settings(
            Builder builder
    ) {
        super(builder);
        this.schema = builder.schema;
        this.numberOfRetries = builder.numberOfRetries;
        this.storageClass = builder.storageClass;
        this.compression = builder.compression;
        this.region = builder.region;
        this.description = builder.description;
        this.itemList = builder.itemList;
        this.operationMode = builder.operationMode;
    }

    public Schema getSchema() {
        return schema;
    }

    public Integer getNumberOfRetries() {
        return numberOfRetries;
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public String getCompression() {
        return compression;
    }

    public String getRegion() {
        return region;
    }

    public String getDescription() {
        return description;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public OperationMode getOperationMode() {
        return operationMode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        private Schema schema = null;
        private Integer numberOfRetries = null;
        private StorageClass storageClass = null;
        private String compression = null;
        private String region = null;
        private String description = null;
        private final List<Item> itemList = new ArrayList<>();
        private OperationMode operationMode = OperationMode.ASYNC;

        public Builder setSchema(Schema schema) {
            this.schema = schema;

            return this;
        }

        public Builder setNumberOfRetries(Integer numberOfRetries) {
            this.numberOfRetries = numberOfRetries;

            return this;
        }

        public Builder setStorageClass(StorageClass storageClass) {
            this.storageClass = storageClass;

            return this;
        }

        public Builder setCompression(String compression) {
            this.compression = compression;

            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;

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

        public Builder addItem(String sourcePath, String destinationPrefix) {
            Preconditions.checkArgument(
                    sourcePath != null && destinationPrefix != null,
                    "Item params aren't null!"
            );
            itemList.add(new Item(sourcePath, destinationPrefix));

            return this;
        }

        public Builder setMode(OperationMode operationMode) {
            this.operationMode = operationMode;

            return this;
        }

        @Override
        public ExportToS3Settings build() {
            Preconditions.checkArgument(!itemList.isEmpty(), "Item list must be not empty!");

            return new ExportToS3Settings(this);
        }
    }

    public enum Schema {
        HTTP,
        HTTPS;

        public YdbExport.ExportToS3Settings.Scheme toProto() {
            switch (this) {
                case HTTP:
                    return YdbExport.ExportToS3Settings.Scheme.HTTP;
                case HTTPS:
                    return YdbExport.ExportToS3Settings.Scheme.HTTPS;
                default:
                    throw new RuntimeException("Unsupported schema!");
            }
        }
    }

    public enum StorageClass {
        STANDARD,
        REDUCED_REDUNDANCY,
        STANDARD_IA,
        ONEZONE_IA,
        INTELLIGENT_TIERING,
        GLACIER,
        DEEP_ARCHIVE,
        OUTPOSTS;

        public YdbExport.ExportToS3Settings.StorageClass toProto() {
            switch (this) {
                case STANDARD:
                    return YdbExport.ExportToS3Settings.StorageClass.STANDARD;
                case REDUCED_REDUNDANCY:
                    return YdbExport.ExportToS3Settings.StorageClass.REDUCED_REDUNDANCY;
                case STANDARD_IA:
                    return YdbExport.ExportToS3Settings.StorageClass.STANDARD_IA;
                case ONEZONE_IA:
                    return YdbExport.ExportToS3Settings.StorageClass.ONEZONE_IA;
                case INTELLIGENT_TIERING:
                    return YdbExport.ExportToS3Settings.StorageClass.INTELLIGENT_TIERING;
                case GLACIER:
                    return YdbExport.ExportToS3Settings.StorageClass.GLACIER;
                case DEEP_ARCHIVE:
                    return YdbExport.ExportToS3Settings.StorageClass.DEEP_ARCHIVE;
                case OUTPOSTS:
                    return YdbExport.ExportToS3Settings.StorageClass.OUTPOSTS;
                default:
                    throw new RuntimeException("Unsupported storage class!");
            }
        }
    }

    public static class Item {
        /**
         * Database path to a table to be exported
         */
        private final String sourcePath;

        /**
         * Tables are exported to one or more S3 objects.
         * The object name begins with 'destination_prefix'.
         * This prefix will be followed by '/data_PartNumber', where 'PartNumber'
         * represents the index of the part, starting at zero.
         */
        private final String destinationPrefix;


        private Item(String sourcePath, String destinationPrefix) {
            this.sourcePath = sourcePath;
            this.destinationPrefix = destinationPrefix;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getDestinationPrefix() {
            return destinationPrefix;
        }
    }
}
