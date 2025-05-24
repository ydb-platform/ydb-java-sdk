package tech.ydb.table.description;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableOptionDescription {

    private final List<TableProfileDescription> tableProfileDescriptions;
    private final List<StoragePolicyDescription> storagePolicyPresets;
    private final List<CompactionPolicyDescription> compactionPolicyPresets;
    private final List<PartitioningPolicyDescription> partitioningPolicyPresets;
    private final List<ExecutionPolicyDescription> executionPolicyPresets;
    private final List<ReplicationPolicyDescription> replicationPolicyPresets;
    private final List<CachingPolicyDescription> cachingPolicyPresets;

    public TableOptionDescription(Builder builder) {
        this.tableProfileDescriptions = builder.tableProfileDescriptions;
        this.storagePolicyPresets = builder.storagePolicyPresets;
        this.compactionPolicyPresets = builder.compactionPolicyPresets;
        this.partitioningPolicyPresets = builder.partitioningPolicyPresets;
        this.executionPolicyPresets = builder.executionPolicyPresets;
        this.replicationPolicyPresets = builder.replicationPolicyPresets;
        this.cachingPolicyPresets = builder.cachingPolicyPresets;
    }

    public static TableOptionDescription.Builder newBuilder() {
        return new TableOptionDescription.Builder();
    }

    public List<TableProfileDescription> getTableProfileDescriptions() {
        return tableProfileDescriptions;
    }

    public List<StoragePolicyDescription> getStoragePolicyPresets() {
        return storagePolicyPresets;
    }

    public List<CompactionPolicyDescription> getCompactionPolicyPresets() {
        return compactionPolicyPresets;
    }

    public List<PartitioningPolicyDescription> getPartitioningPolicyPresets() {
        return partitioningPolicyPresets;
    }

    public List<ExecutionPolicyDescription> getExecutionPolicyPresets() {
        return executionPolicyPresets;
    }

    public List<ReplicationPolicyDescription> getReplicationPolicyPresets() {
        return replicationPolicyPresets;
    }

    public List<CachingPolicyDescription> getCachingPolicyPresets() {
        return cachingPolicyPresets;
    }

    public static class Builder {
        private List<TableProfileDescription> tableProfileDescriptions;
        private List<StoragePolicyDescription> storagePolicyPresets;
        private List<CompactionPolicyDescription> compactionPolicyPresets;
        private List<PartitioningPolicyDescription> partitioningPolicyPresets;
        private List<ExecutionPolicyDescription> executionPolicyPresets;
        private List<ReplicationPolicyDescription> replicationPolicyPresets;
        private List<CachingPolicyDescription> cachingPolicyPresets;

        public List<TableProfileDescription> getTableProfileDescriptions() {
            return tableProfileDescriptions;
        }

        public void setTableProfileDescriptions(ArrayList<TableProfileDescription> tableProfileDescriptions) {
            this.tableProfileDescriptions = tableProfileDescriptions;
        }

        public List<StoragePolicyDescription> getStoragePolicyPresets() {
            return storagePolicyPresets;
        }

        public void setStoragePolicyPresets(List<StoragePolicyDescription> storagePolicyPresets) {
            this.storagePolicyPresets = storagePolicyPresets;
        }

        public List<CompactionPolicyDescription> getCompactionPolicyPresets() {
            return compactionPolicyPresets;
        }

        public void setCompactionPolicyPresets(List<CompactionPolicyDescription> compactionPolicyPresets) {
            this.compactionPolicyPresets = compactionPolicyPresets;
        }

        public List<PartitioningPolicyDescription> getPartitioningPolicyPresets() {
            return partitioningPolicyPresets;
        }

        public void setPartitioningPolicyPresets(List<PartitioningPolicyDescription> partitioningPolicyPresets) {
            this.partitioningPolicyPresets = partitioningPolicyPresets;
        }

        public List<ExecutionPolicyDescription> getExecutionPolicyPresets() {
            return executionPolicyPresets;
        }

        public void setExecutionPolicyPresets(List<ExecutionPolicyDescription> executionPolicyPresets) {
            this.executionPolicyPresets = executionPolicyPresets;
        }

        public List<ReplicationPolicyDescription> getReplicationPolicyPresets() {
            return replicationPolicyPresets;
        }

        public void setReplicationPolicyPresets(List<ReplicationPolicyDescription> replicationPolicyPresets) {
            this.replicationPolicyPresets = replicationPolicyPresets;
        }

        public List<CachingPolicyDescription> getCachingPolicyPresets() {
            return cachingPolicyPresets;
        }

        public void setCachingPolicyPresets(List<CachingPolicyDescription> cachingPolicyPresets) {
            this.cachingPolicyPresets = cachingPolicyPresets;
        }
    }

    public static class TableProfileDescription {
        private final String name;
        private final Map<String, String> labels;

        private final String defaultStoragePolicy;
        private final String defaultCompactionPolicy;
        private final String defaultPartitioningPolicy;
        private final String defaultExecutionPolicy;
        private final String defaultReplicationPolicy;
        private final String defaultCachingPolicy;

        private final List<String> allowedStoragePolicy;
        private final List<String> allowedCompactionPolicy;
        private final List<String> allowedPartitioningPolicy;
        private final List<String> allowedExecutionPolicy;
        private final List<String> allowedReplicationPolicy;
        private final List<String> allowedCachingPolicy;

        public TableProfileDescription(Builder builder) {
            this.name = builder.name;
            this.labels = ImmutableMap.copyOf(builder.labels);
            this.defaultStoragePolicy = builder.defaultStoragePolicy;
            this.defaultCompactionPolicy = builder.defaultCompactionPolicy;
            this.defaultPartitioningPolicy = builder.defaultPartitioningPolicy;
            this.defaultExecutionPolicy = builder.defaultExecutionPolicy;
            this.defaultReplicationPolicy = builder.defaultReplicationPolicy;
            this.defaultCachingPolicy = builder.defaultCachingPolicy;

            this.allowedStoragePolicy = ImmutableList.copyOf(builder.allowedStoragePolicy);
            this.allowedCompactionPolicy = ImmutableList.copyOf(builder.allowedCompactionPolicy);
            this.allowedPartitioningPolicy = ImmutableList.copyOf(builder.allowedPartitioningPolicy);
            this.allowedExecutionPolicy = ImmutableList.copyOf(builder.allowedExecutionPolicy);
            this.allowedReplicationPolicy = ImmutableList.copyOf(builder.allowedReplicationPolicy);
            this.allowedCachingPolicy = ImmutableList.copyOf(builder.allowedCachingPolicy);
        }

        public static TableProfileDescription.Builder newBuilder() {
            return new TableProfileDescription.Builder();
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public String getDefaultStoragePolicy() {
            return defaultStoragePolicy;
        }

        public String getDefaultCompactionPolicy() {
            return defaultCompactionPolicy;
        }

        public String getDefaultPartitioningPolicy() {
            return defaultPartitioningPolicy;
        }

        public String getDefaultExecutionPolicy() {
            return defaultExecutionPolicy;
        }

        public String getDefaultReplicationPolicy() {
            return defaultReplicationPolicy;
        }

        public String getDefaultCachingPolicy() {
            return defaultCachingPolicy;
        }

        public List<String> getAllowedStoragePolicy() {
            return allowedStoragePolicy;
        }

        public List<String> getAllowedCompactionPolicy() {
            return allowedCompactionPolicy;
        }

        public List<String> getAllowedPartitioningPolicy() {
            return allowedPartitioningPolicy;
        }

        public List<String> getAllowedExecutionPolicy() {
            return allowedExecutionPolicy;
        }

        public List<String> getAllowedReplicationPolicy() {
            return allowedReplicationPolicy;
        }

        public List<String> getAllowedCachingPolicy() {
            return allowedCachingPolicy;
        }

        public static class Builder {
            String name;
            Map<String, String> labels;

            String defaultStoragePolicy;
            String defaultCompactionPolicy;
            String defaultPartitioningPolicy;
            String defaultExecutionPolicy;
            String defaultReplicationPolicy;
            String defaultCachingPolicy;

            List<String> allowedStoragePolicy;
            List<String> allowedCompactionPolicy;
            List<String> allowedPartitioningPolicy;
            List<String> allowedExecutionPolicy;
            List<String> allowedReplicationPolicy;
            List<String> allowedCachingPolicy;

            public TableProfileDescription build() {
                return new TableProfileDescription(this);
            }

            public void setName(String name) {
                this.name = name;
            }

            public void setLabels(Map<String, String> labels) {
                this.labels = labels;
            }

            public void setDefaultStoragePolicy(String defaultStoragePolicy) {
                this.defaultStoragePolicy = defaultStoragePolicy;
            }

            public void setDefaultCompactionPolicy(String defaultCompactionPolicy) {
                this.defaultCompactionPolicy = defaultCompactionPolicy;
            }

            public void setDefaultPartitioningPolicy(String defaultPartitioningPolicy) {
                this.defaultPartitioningPolicy = defaultPartitioningPolicy;
            }

            public void setDefaultExecutionPolicy(String defaultExecutionPolicy) {
                this.defaultExecutionPolicy = defaultExecutionPolicy;
            }

            public void setDefaultReplicationPolicy(String defaultReplicationPolicy) {
                this.defaultReplicationPolicy = defaultReplicationPolicy;
            }

            public void setDefaultCachingPolicy(String defaultCachingPolicy) {
                this.defaultCachingPolicy = defaultCachingPolicy;
            }

            public void setAllowedStoragePolicy(List<String> allowedStoragePolicy) {
                this.allowedStoragePolicy = allowedStoragePolicy;
            }

            public void setAllowedCompactionPolicy(List<String> allowedCompactionPolicy) {
                this.allowedCompactionPolicy = allowedCompactionPolicy;
            }

            public void setAllowedPartitioningPolicy(List<String> allowedPartitioningPolicy) {
                this.allowedPartitioningPolicy = allowedPartitioningPolicy;
            }

            public void setAllowedExecutionPolicy(List<String> allowedExecutionPolicy) {
                this.allowedExecutionPolicy = allowedExecutionPolicy;
            }

            public void setAllowedReplicationPolicy(List<String> allowedReplicationPolicy) {
                this.allowedReplicationPolicy = allowedReplicationPolicy;
            }

            public void setAllowedCachingPolicy(List<String> allowedCachingPolicy) {
                this.allowedCachingPolicy = allowedCachingPolicy;
            }

        }
    }

    public static class StoragePolicyDescription {
        private final String name;
        private final Map<String,String> labels;

        public StoragePolicyDescription(String name, Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }

    public static class CompactionPolicyDescription {
        private final String name;
        private final Map<String,String> labels;

        public CompactionPolicyDescription(String name, Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }

    public static class PartitioningPolicyDescription {
        private final String name;
        private final Map<String,String> labels;

        public PartitioningPolicyDescription(String name, Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }

    public static class ExecutionPolicyDescription {
        private final String name;
        private final Map<String,String> labels;

        public ExecutionPolicyDescription(String name, Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }

    public static class ReplicationPolicyDescription {
        private final String name;
        private final Map<String,String> labels;

        public ReplicationPolicyDescription(String name, Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }

    public static class CachingPolicyDescription {
        private final String name;
        private final Map<String,String> labels;

        public CachingPolicyDescription(String name, Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }
}
