package tech.ydb.table.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.impl.YdbTransactionImpl;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.core.utils.URITools;
import tech.ydb.core.utils.UpdatableOptional;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.ValueProtos.TypedValue;
import tech.ydb.proto.YdbIssueMessage;
import tech.ydb.proto.common.CommonProtos;
import tech.ydb.proto.scheme.SchemeOperationProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.Session;
import tech.ydb.table.description.ChangefeedDescription;
import tech.ydb.table.description.ColumnFamily;
import tech.ydb.table.description.KeyBound;
import tech.ydb.table.description.KeyRange;
import tech.ydb.table.description.RenameIndex;
import tech.ydb.table.description.SequenceDescription;
import tech.ydb.table.description.StoragePool;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.description.TableOptionDescription;
import tech.ydb.table.description.TableTtl;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.ReadRowsResult;
import tech.ydb.table.query.ReadTablePart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.AutoPartitioningPolicy;
import tech.ydb.table.settings.BeginTxSettings;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.Changefeed;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.CopyTableSettings;
import tech.ydb.table.settings.CopyTablesSettings;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DeleteSessionSettings;
import tech.ydb.table.settings.DescribeTableOptionsSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.DropTableSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.settings.ExplainDataQuerySettings;
import tech.ydb.table.settings.KeepAliveSessionSettings;
import tech.ydb.table.settings.PartitioningPolicy;
import tech.ydb.table.settings.PartitioningSettings;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadRowsSettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.settings.RenameTablesSettings;
import tech.ydb.table.settings.ReplicationPolicy;
import tech.ydb.table.settings.RequestSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.settings.StoragePolicy;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;

/**
 * @author Sergey Polovko
 * @author Alexandr Gorshenin
 * @author Nikolay Perfilov
 */
@ThreadSafe
public abstract class BaseSession implements Session {
    private static final String SERVER_BALANCER_HINT = "session-balancer";
    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private final String id;
    private final Integer preferredNodeID;
    private final TableRpc rpc;
    private final ShutdownHandler shutdownHandler;
    private final boolean keepQueryText;

    protected BaseSession(String id, TableRpc tableRpc, boolean keepQueryText) {
        this.id = id;
        this.rpc = tableRpc;
        this.keepQueryText = keepQueryText;
        this.preferredNodeID = getNodeBySessionId(id);
        this.shutdownHandler = new ShutdownHandler();
    }

    private static Integer getNodeBySessionId(String sessionId) {
        try {
            Map<String, List<String>> params = URITools.splitQuery(new URI(sessionId));
            List<String> nodeParam = params.get("node_id");
            if (nodeParam != null && !nodeParam.isEmpty()) {
                return Integer.parseUnsignedInt(nodeParam.get(0));
            }
        } catch (URISyntaxException | RuntimeException e) {
            logger.debug("Failed to parse session_id for node_id: {}", e.toString());
        }
        return null;
    }

    private static GrpcRequestSettings.Builder makeBaseOptions(Duration timeout, String traceId) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(timeout)
                .withTraceId(traceId == null ? UUID.randomUUID().toString() : traceId);
    }

    private GrpcRequestSettings.Builder makeOptions(RequestSettings<?> settings) {
        return makeBaseOptions(settings.getTimeoutDuration(), settings.getTraceId())
                .withPreferredNodeID(preferredNodeID)
                .withTrailersHandler(shutdownHandler);
    }

    private GrpcRequestSettings.Builder makeOptions(BaseRequestSettings settings) {
        return makeBaseOptions(settings.getRequestTimeout(), settings.getTraceId())
                .withPreferredNodeID(preferredNodeID)
                .withTrailersHandler(shutdownHandler);
    }

    @Override
    public String getId() {
        return id;
    }

    public static CompletableFuture<Result<String>> createSessionId(TableRpc rpc, CreateSessionSettings settings,
                                                                    boolean useServerBalancer) {
        YdbTable.CreateSessionRequest request = YdbTable.CreateSessionRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        AtomicBoolean pessimizationHook = new AtomicBoolean(false);

        GrpcRequestSettings.Builder options = makeBaseOptions(settings.getTimeoutDuration(), settings.getTraceId())
                .withPessimizationHook(pessimizationHook::get);

        if (useServerBalancer) {
            options.addClientCapability(SERVER_BALANCER_HINT);
        }

        return rpc.createSession(request, options.build()).thenApply(result -> {
            pessimizationHook.set(result.getStatus().getCode() == StatusCode.OVERLOADED);
            return result.map(YdbTable.CreateSessionResult::getSessionId);
        });
    }

    private static YdbTable.PartitioningSettings buildPartitioningSettings(PartitioningSettings partitioningSettings) {
        YdbTable.PartitioningSettings.Builder builder = YdbTable.PartitioningSettings.newBuilder();
        if (partitioningSettings.getPartitioningByLoad() != null) {
            builder.setPartitioningByLoad(
                    partitioningSettings.getPartitioningByLoad()
                            ? CommonProtos.FeatureFlag.Status.ENABLED
                            : CommonProtos.FeatureFlag.Status.DISABLED
            );
        }
        if (partitioningSettings.getPartitioningBySize() != null) {
            builder.setPartitioningBySize(
                    partitioningSettings.getPartitioningBySize()
                            ? CommonProtos.FeatureFlag.Status.ENABLED
                            : CommonProtos.FeatureFlag.Status.DISABLED
            );
        }
        if (partitioningSettings.getPartitionSizeMb() != null) {
            builder.setPartitionSizeMb(partitioningSettings.getPartitionSizeMb());
        }
        if (partitioningSettings.getMinPartitionsCount() != null) {
            builder.setMinPartitionsCount(partitioningSettings.getMinPartitionsCount());
        }
        if (partitioningSettings.getMaxPartitionsCount() != null) {
            builder.setMaxPartitionsCount(partitioningSettings.getMaxPartitionsCount());
        }

        return builder.build();
    }

    private static YdbTable.ColumnMeta buildColumnMeta(TableColumn column) {
        YdbTable.ColumnMeta.Builder builder = YdbTable.ColumnMeta.newBuilder()
                .setName(column.getName())
                .setType(column.getType().toPb());
        if (column.getFamily() != null) {
            builder.setFamily(column.getFamily());
        }
        if (column.getType().getKind() != Type.Kind.OPTIONAL) {
            builder.setNotNull(true);
        }
        if (column.hasDefaultValue()) {
            if (column.getLiteralDefaultValue() != null) {
                builder.setFromLiteral(ValueProtos.TypedValue.newBuilder()
                        .setType(column.getType().toPb())
                        .setValue(column.getLiteralDefaultValue().toPb()).build()
                );
            }

            if (column.getSequenceDescription() != null) {
                SequenceDescription sequenceDescription = column.getSequenceDescription();
                YdbTable.SequenceDescription.Builder sequenceDescriptionBuilder = YdbTable.SequenceDescription
                        .newBuilder()
                        .setName(sequenceDescription.getName());

                if (sequenceDescription.getMinValue() != null) {
                    sequenceDescriptionBuilder.setMinValue(sequenceDescription.getMinValue());
                }

                if (sequenceDescription.getMaxValue() != null) {
                    sequenceDescriptionBuilder.setMaxValue(sequenceDescription.getMaxValue());
                }

                if (sequenceDescription.getStartValue() != null) {
                    sequenceDescriptionBuilder.setStartValue(sequenceDescription.getStartValue());
                }

                if (sequenceDescription.getCache() != null) {
                    sequenceDescriptionBuilder.setCache(sequenceDescription.getCache());
                }

                if (sequenceDescription.getIncrement() != null) {
                    sequenceDescriptionBuilder.setIncrement(sequenceDescription.getIncrement());
                }

                if (sequenceDescription.getCycle() != null) {
                    sequenceDescriptionBuilder.setCycle(sequenceDescription.getCycle());
                }

                builder.setFromSequence(sequenceDescriptionBuilder.build());
            }
        }

        return builder.build();
    }

    public static YdbTable.ChangefeedFormat.Format buildChangefeedFormat(Changefeed.Format format) {
        switch (format) {
            case JSON:
                return YdbTable.ChangefeedFormat.Format.FORMAT_JSON;
            case DYNAMODB_STREAMS_JSON:
                return YdbTable.ChangefeedFormat.Format.FORMAT_DYNAMODB_STREAMS_JSON;
            case DEBEZIUM_JSON:
                return YdbTable.ChangefeedFormat.Format.FORMAT_DEBEZIUM_JSON;
            default:
                return YdbTable.ChangefeedFormat.Format.FORMAT_UNSPECIFIED;
        }
    }

    public static YdbTable.ChangefeedMode.Mode buildChangefeedMode(Changefeed.Mode mode) {
        switch (mode) {
            case KEYS_ONLY:
                return YdbTable.ChangefeedMode.Mode.MODE_KEYS_ONLY;
            case UPDATES:
                return YdbTable.ChangefeedMode.Mode.MODE_UPDATES;
            case NEW_IMAGE:
                return YdbTable.ChangefeedMode.Mode.MODE_NEW_IMAGE;
            case OLD_IMAGE:
                return YdbTable.ChangefeedMode.Mode.MODE_OLD_IMAGE;
            case NEW_AND_OLD_IMAGES:
                return YdbTable.ChangefeedMode.Mode.MODE_NEW_AND_OLD_IMAGES;
            default:
                return YdbTable.ChangefeedMode.Mode.MODE_UNSPECIFIED;
        }
    }

    public static YdbTable.Changefeed buildChangefeed(Changefeed changefeed) {
        YdbTable.Changefeed.Builder builder = YdbTable.Changefeed.newBuilder()
                .setName(changefeed.getName())
                .setFormat(buildChangefeedFormat(changefeed.getFormat()))
                .setMode(buildChangefeedMode(changefeed.getMode()))
                .setVirtualTimestamps(changefeed.hasVirtualTimestamps())
                .setInitialScan(changefeed.hasInitialScan());

        Duration retentionPeriod = changefeed.getRetentionPeriod();
        if (retentionPeriod != null) {
            builder.setRetentionPeriod(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(retentionPeriod.getSeconds())
                    .setNanos(retentionPeriod.getNano())
                    .build());
        }

        Duration resolvedTimestampsInterval = changefeed.getResolvedTimestampsInterval();
        if (resolvedTimestampsInterval != null) {
            builder.setResolvedTimestampsInterval(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(resolvedTimestampsInterval.getSeconds())
                    .setNanos(resolvedTimestampsInterval.getNano())
                    .build());
        }

        return builder.build();
    }

    private static YdbTable.TableIndex buildIndex(TableIndex index) {
        YdbTable.TableIndex.Builder builder = YdbTable.TableIndex.newBuilder();
        builder.setName(index.getName());
        builder.addAllIndexColumns(index.getColumns());
        builder.addAllDataColumns(index.getDataColumns());
        switch (index.getType()) {
            case GLOBAL_UNIQUE:
                builder.setGlobalUniqueIndex(YdbTable.GlobalUniqueIndex.getDefaultInstance());
                break;
            case GLOBAL_ASYNC:
                builder.setGlobalAsyncIndex(YdbTable.GlobalAsyncIndex.getDefaultInstance());
                break;
            case GLOBAL:
            default:
                builder.setGlobalIndex(YdbTable.GlobalIndex.getDefaultInstance());
                break;
        }
        return builder.build();
    }

    private static YdbTable.ColumnFamily buildColumnFamily(ColumnFamily family) {
        YdbTable.ColumnFamily.Compression compression;
        switch (family.getCompression()) {
            case COMPRESSION_NONE:
                compression = YdbTable.ColumnFamily.Compression.COMPRESSION_NONE;
                break;
            case COMPRESSION_LZ4:
                compression = YdbTable.ColumnFamily.Compression.COMPRESSION_LZ4;
                break;
            default:
                compression = YdbTable.ColumnFamily.Compression.COMPRESSION_UNSPECIFIED;
        }

        return YdbTable.ColumnFamily.newBuilder()
                .setCompression(compression)
                .setData(YdbTable.StoragePool.newBuilder().setMedia(family.getData().getMedia()))
                .setName(family.getName())
                .build();
    }

    private static YdbTable.TtlSettings buildTtlSettings(TableTtl ttl) {
        if (ttl == null || ttl.getTtlMode() == TableTtl.TtlMode.NOT_SET) {
            return null;
        }

        YdbTable.TtlSettings.Builder tb = YdbTable.TtlSettings.newBuilder();

        if (ttl.getTtlMode() == TableTtl.TtlMode.DATE_TYPE_COLUMN) {
            tb.setDateTypeColumn(YdbTable.DateTypeColumnModeSettings.newBuilder()
                    .setColumnName(ttl.getDateTimeColumn())
                    .setExpireAfterSeconds(ttl.getExpireAfterSeconds())
                    .build());
        }

        if (ttl.getTtlMode() == TableTtl.TtlMode.VALUE_SINCE_UNIX_EPOCH) {
            YdbTable.ValueSinceUnixEpochModeSettings.Unit unit;
            switch (ttl.getTtlUnit()) {
                case SECONDS:
                    unit = YdbTable.ValueSinceUnixEpochModeSettings.Unit.UNIT_SECONDS;
                    break;
                case MILLISECONDS:
                    unit = YdbTable.ValueSinceUnixEpochModeSettings.Unit.UNIT_MILLISECONDS;
                    break;
                case MICROSECONDS:
                    unit = YdbTable.ValueSinceUnixEpochModeSettings.Unit.UNIT_MICROSECONDS;
                    break;
                case NANOSECONDS:
                    unit = YdbTable.ValueSinceUnixEpochModeSettings.Unit.UNIT_NANOSECONDS;
                    break;
                case UNSPECIFIED:
                default:
                    unit = YdbTable.ValueSinceUnixEpochModeSettings.Unit.UNIT_UNSPECIFIED;
                    break;
            }

            tb.setValueSinceUnixEpoch(YdbTable.ValueSinceUnixEpochModeSettings.newBuilder()
                    .setColumnName(ttl.getDateTimeColumn())
                    .setColumnUnit(unit)
                    .setExpireAfterSeconds(ttl.getExpireAfterSeconds())
                    .build());
        }

        if (ttl.getRunIntervalSeconds() != null) {
            tb.setRunIntervalSeconds(ttl.getRunIntervalSeconds());
        }

        return tb.build();
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<Status> createTable(
            String path,
            TableDescription description,
            CreateTableSettings settings
    ) {
        YdbTable.CreateTableRequest.Builder request = YdbTable.CreateTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .addAllPrimaryKey(description.getPrimaryKeys());

        switch (description.getStoreType()) {
            case ROW:
                request.setStoreType(YdbTable.StoreType.STORE_TYPE_ROW);
                break;
            case COLUMN:
                request.setStoreType(YdbTable.StoreType.STORE_TYPE_COLUMN);
                break;
            default:
                break;
        }

        for (ColumnFamily family : description.getColumnFamilies()) {
            request.addColumnFamilies(buildColumnFamily(family));
        }

        for (TableColumn column : description.getColumns()) {
            request.addColumns(buildColumnMeta(column));
        }

        for (TableIndex index : description.getIndexes()) {
            request.addIndexes(buildIndex(index));
        }

        if (description.getTableTtl() != null) {
            YdbTable.TtlSettings ttl = buildTtlSettings(description.getTableTtl());
            if (ttl != null) {
                request.setTtlSettings(ttl);
            }
        }
        // deprecated variant has high priority
        tech.ydb.table.settings.TtlSettings deprecatedTTL = settings.getTtlSettings();
        if (deprecatedTTL != null) {
            YdbTable.TtlSettings ttl = YdbTable.TtlSettings.newBuilder()
                    .setDateTypeColumn(YdbTable.DateTypeColumnModeSettings.newBuilder()
                            .setColumnName(deprecatedTTL.getDateTimeColumn())
                            .setExpireAfterSeconds(deprecatedTTL.getExpireAfterSeconds())
                            .build())
                    .build();
            request.setTtlSettings(ttl);
        }

        if (description.getPartitioningSettings() != null) {
            request.setPartitioningSettings(buildPartitioningSettings(description.getPartitioningSettings()));
        }

        if (settings.getPresetName() != null) {
            request.getProfileBuilder().setPresetName(settings.getPresetName());
        }

        if (settings.getExecutionPolicy() != null) {
            request.getProfileBuilder().getExecutionPolicyBuilder().setPresetName(settings.getExecutionPolicy());
        }

        if (settings.getCompactionPolicy() != null) {
            request.getProfileBuilder().getCompactionPolicyBuilder().setPresetName(settings.getCompactionPolicy());
        }

        PartitioningPolicy partitioningPolicy = settings.getPartitioningPolicy();
        if (partitioningPolicy != null) {
            YdbTable.PartitioningPolicy.Builder policyProto = request.getProfileBuilder()
                    .getPartitioningPolicyBuilder();
            if (partitioningPolicy.getPresetName() != null) {
                policyProto.setPresetName(partitioningPolicy.getPresetName());
            }
            if (partitioningPolicy.getAutoPartitioning() != null) {
                policyProto.setAutoPartitioning(toPb(partitioningPolicy.getAutoPartitioning()));
            }

            if (partitioningPolicy.getUniformPartitions() > 0) {
                policyProto.setUniformPartitions(partitioningPolicy.getUniformPartitions());
            } else {
                List<TupleValue> points = partitioningPolicy.getExplicitPartitioningPoints();
                if (points != null) {
                    YdbTable.ExplicitPartitions.Builder b = policyProto.getExplicitPartitionsBuilder();
                    for (Value<?> p : points) {
                        b.addSplitPoints(ProtoValue.toTypedValue(p));
                    }
                }
            }
        }

        StoragePolicy storagePolicy = settings.getStoragePolicy();
        if (storagePolicy != null) {
            YdbTable.StoragePolicy.Builder policyProto = request.getProfileBuilder()
                    .getStoragePolicyBuilder();
            if (storagePolicy.getPresetName() != null) {
                policyProto.setPresetName(storagePolicy.getPresetName());
            }
            if (storagePolicy.getSysLog() != null) {
                policyProto.getSyslogBuilder().setMedia(storagePolicy.getSysLog());
            }
            if (storagePolicy.getLog() != null) {
                policyProto.getLogBuilder().setMedia(storagePolicy.getLog());
            }
            if (storagePolicy.getData() != null) {
                policyProto.getDataBuilder().setMedia(storagePolicy.getData());
            }
            if (storagePolicy.getExternal() != null) {
                policyProto.getExternalBuilder().setMedia(storagePolicy.getExternal());
            }
        }

        ReplicationPolicy replicationPolicy = settings.getReplicationPolicy();
        if (replicationPolicy != null) {
            YdbTable.ReplicationPolicy.Builder replicationPolicyProto =
                    request.getProfileBuilder().getReplicationPolicyBuilder();
            if (replicationPolicy.getPresetName() != null) {
                replicationPolicyProto.setPresetName(replicationPolicy.getPresetName());
            }
            replicationPolicyProto.setReplicasCount(replicationPolicy.getReplicasCount());
            replicationPolicyProto.setCreatePerAvailabilityZone(replicationPolicy.isCreatePerAvailabilityZone() ?
                    CommonProtos.FeatureFlag.Status.ENABLED : CommonProtos.FeatureFlag.Status.DISABLED);
            replicationPolicyProto.setAllowPromotion(replicationPolicy.isAllowPromotion() ?
                    CommonProtos.FeatureFlag.Status.ENABLED : CommonProtos.FeatureFlag.Status.DISABLED);
        }

        return rpc.createTable(request.build(), makeOptions(settings).build());
    }

    private static YdbTable.PartitioningPolicy.AutoPartitioningPolicy toPb(AutoPartitioningPolicy policy) {
        switch (policy) {
            case AUTO_SPLIT:
                return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT;
            case AUTO_SPLIT_MERGE:
                return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT_MERGE;
            case DISABLED:
                return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.DISABLED;
            default:
                throw new IllegalArgumentException("unknown AutoPartitioningPolicy: " + policy);
        }
    }

    @Override
    public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
        YdbTable.DropTableRequest request = YdbTable.DropTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        return rpc.dropTable(request, makeOptions(settings).build());
    }

    @Override
    public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
        YdbTable.AlterTableRequest.Builder builder = YdbTable.AlterTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()));

        for (TableColumn addColumn : settings.getAddColumns()) {
            builder.addAddColumns(buildColumnMeta(addColumn));
        }

        for (Changefeed addChangefeed : settings.getAddChangefeeds()) {
            builder.addAddChangefeeds(buildChangefeed(addChangefeed));
        }

        for (TableIndex index : settings.getAddIndexes()) {
            builder.addAddIndexes(buildIndex(index));
        }

        if (settings.getTableTTL() != null) {
            YdbTable.TtlSettings ttl = buildTtlSettings(settings.getTableTTL());
            if (ttl != null) {
                builder.setSetTtlSettings(ttl);
            } else {
                builder.setDropTtlSettings(Empty.getDefaultInstance());
            }
        }

        if (settings.getPartitioningSettings() != null) {
            builder.setAlterPartitioningSettings(buildPartitioningSettings(settings.getPartitioningSettings()));
        }

        for (String dropColumn : settings.getDropColumns()) {
            builder.addDropColumns(dropColumn);
        }

        for (String dropChangefeed : settings.getDropChangefeeds()) {
            builder.addDropChangefeeds(dropChangefeed);
        }

        for (String dropIndex : settings.getDropIndexes()) {
            builder.addDropIndexes(dropIndex);
        }

        for (RenameIndex renameIndex : settings.getRenameIndexes()) {
            builder.addRenameIndexes(YdbTable.RenameIndexItem.newBuilder()
                    .setSourceName(renameIndex.getSourceName())
                    .setDestinationName(renameIndex.getDestinationName())
                    .setReplaceDestination(renameIndex.isReplaceDestination()).build());
        }

        return rpc.alterTable(builder.build(), makeOptions(settings).build());
    }

    @Override
    public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
        YdbTable.CopyTableRequest request = YdbTable.CopyTableRequest.newBuilder()
                .setSessionId(id)
                .setSourcePath(src)
                .setDestinationPath(dst)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        return rpc.copyTable(request, makeOptions(settings).build());
    }

    @Override
    public CompletableFuture<Status> copyTables(CopyTablesSettings settings) {
        YdbTable.CopyTablesRequest request = YdbTable.CopyTablesRequest.newBuilder()
                .setSessionId(id)
                .addAllTables(convertCopyTableItems(settings))
                .build();

        return rpc.copyTables(request, makeOptions(settings).build());
    }

    private List<YdbTable.CopyTableItem> convertCopyTableItems(CopyTablesSettings cts) {
        final String dbpath = rpc.getDatabase();
        return cts.getItems().stream().map(t -> {
            String sp = t.getSourcePath();
            if (!sp.startsWith("/")) {
                sp = dbpath + "/" + sp;
            }
            String dp = t.getDestinationPath();
            if (!dp.startsWith("/")) {
                dp = dbpath + "/" + dp;
            }
            return YdbTable.CopyTableItem.newBuilder()
                    .setSourcePath(sp)
                    .setDestinationPath(dp)
                    .setOmitIndexes(t.isOmitIndexes())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Status> renameTables(RenameTablesSettings settings) {
        YdbTable.RenameTablesRequest request = YdbTable.RenameTablesRequest.newBuilder()
                .setSessionId(id)
                .addAllTables(convertRenameTableItems(settings))
                .build();

        return rpc.renameTables(request, makeOptions(settings).build());
    }

    private List<YdbTable.RenameTableItem> convertRenameTableItems(RenameTablesSettings cts) {
        final String dbpath = rpc.getDatabase();
        return cts.getItems().stream().map(t -> {
            String sp = t.getSourcePath();
            if (!sp.startsWith("/")) {
                sp = dbpath + "/" + sp;
            }
            String dp = t.getDestinationPath();
            if (!dp.startsWith("/")) {
                dp = dbpath + "/" + dp;
            }
            return YdbTable.RenameTableItem.newBuilder()
                    .setSourcePath(sp)
                    .setDestinationPath(dp)
                    .setReplaceDestination(t.isReplaceDestination())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings) {
        YdbTable.DescribeTableRequest request = YdbTable.DescribeTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setIncludeTableStats(settings.isIncludeTableStats())
                .setIncludeShardKeyBounds(settings.isIncludeShardKeyBounds())
                .setIncludePartitionStats(settings.isIncludePartitionStats())
                .build();

        return rpc.describeTable(request, makeOptions(settings).build())
                .thenApply(res -> mapDescribeTable(res, settings));
    }

    @Override
    public CompletableFuture<Result<TableOptionDescription>> describeTableOptions(
            DescribeTableOptionsSettings settings) {
        YdbTable.DescribeTableOptionsRequest request = YdbTable.DescribeTableOptionsRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        return rpc.describeTableOptions(request, makeOptions(settings).build())
                .thenApply(BaseSession::mapDescribeTableOptions);
    }

    private static TableTtl mapTtlSettings(YdbTable.TtlSettings ttl) {
        switch (ttl.getModeCase()) {
            case DATE_TYPE_COLUMN:
                YdbTable.DateTypeColumnModeSettings dc = ttl.getDateTypeColumn();
                return TableTtl
                        .dateTimeColumn(dc.getColumnName(), dc.getExpireAfterSeconds())
                        .withRunIntervalSeconds(ttl.getRunIntervalSeconds());
            case VALUE_SINCE_UNIX_EPOCH:
                YdbTable.ValueSinceUnixEpochModeSettings vs = ttl.getValueSinceUnixEpoch();
                TableTtl.TtlUnit unit;
                switch (vs.getColumnUnit()) {
                    case UNIT_SECONDS:
                        unit = TableTtl.TtlUnit.SECONDS;
                        break;
                    case UNIT_MILLISECONDS:
                        unit = TableTtl.TtlUnit.MILLISECONDS;
                        break;
                    case UNIT_MICROSECONDS:
                        unit = TableTtl.TtlUnit.MICROSECONDS;
                        break;
                    case UNIT_NANOSECONDS:
                        unit = TableTtl.TtlUnit.NANOSECONDS;
                        break;
                    case UNIT_UNSPECIFIED:
                    case UNRECOGNIZED:
                    default:
                        unit = TableTtl.TtlUnit.UNSPECIFIED;
                        break;
                }
                return TableTtl
                        .valueSinceUnixEpoch(vs.getColumnName(), unit, vs.getExpireAfterSeconds())
                        .withRunIntervalSeconds(ttl.getRunIntervalSeconds());
            case MODE_NOT_SET:
            default:
                return TableTtl.notSet();
        }
    }

    private static Changefeed.Format mapChangefeedFormat(YdbTable.ChangefeedFormat.Format pb) {
        switch (pb) {
            case FORMAT_JSON:
                return Changefeed.Format.JSON;
            case FORMAT_DYNAMODB_STREAMS_JSON:
                return Changefeed.Format.DYNAMODB_STREAMS_JSON;
            case FORMAT_DEBEZIUM_JSON:
                return Changefeed.Format.DEBEZIUM_JSON;
            default:
                return null;
        }
    }

    private static Changefeed.Mode mapChangefeedMode(YdbTable.ChangefeedMode.Mode pb) {
        switch (pb) {
            case MODE_KEYS_ONLY:
                return Changefeed.Mode.KEYS_ONLY;
            case MODE_NEW_IMAGE:
                return Changefeed.Mode.NEW_IMAGE;
            case MODE_OLD_IMAGE:
                return Changefeed.Mode.OLD_IMAGE;
            case MODE_NEW_AND_OLD_IMAGES:
                return Changefeed.Mode.NEW_AND_OLD_IMAGES;
            case MODE_UPDATES:
                return Changefeed.Mode.UPDATES;
            default:
                return null;
        }
    }

    private static ChangefeedDescription.State mapChangefeedState(YdbTable.ChangefeedDescription.State pb) {
        switch (pb) {
            case STATE_ENABLED:
                return ChangefeedDescription.State.ENABLED;
            case STATE_DISABLED:
                return ChangefeedDescription.State.DISABLED;
            case STATE_INITIAL_SCAN:
                return ChangefeedDescription.State.INITIAL_SCAN;
            default:
                return null;
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private static Result<TableDescription> mapDescribeTable(Result<YdbTable.DescribeTableResult> describeResult,
                                                             DescribeTableSettings settings) {
        if (!describeResult.isSuccess()) {
            return describeResult.map(r -> null);
        }
        YdbTable.DescribeTableResult desc = describeResult.getValue();
        SchemeOperationProtos.Entry.Type entryType = desc.getSelf().getType();

        if (entryType != SchemeOperationProtos.Entry.Type.TABLE &&
                entryType != SchemeOperationProtos.Entry.Type.COLUMN_TABLE) {
            String errorMsg = "Entry " + desc.getSelf().getName() + " with type " + entryType + " is not a table";
            return Result.fail(Status.of(StatusCode.SCHEME_ERROR).withIssues(Issue.of(errorMsg, Issue.Severity.ERROR)));
        }

        TableDescription.Builder description = TableDescription.newBuilder();
        switch (desc.getStoreType()) {
            case STORE_TYPE_ROW:
                description = description.setStoreType(TableDescription.StoreType.ROW);
                break;
            case STORE_TYPE_COLUMN:
                description = description.setStoreType(TableDescription.StoreType.COLUMN);
                break;
            case UNRECOGNIZED:
            case STORE_TYPE_UNSPECIFIED:
            default:
                break;
        }

        for (int i = 0; i < desc.getColumnsCount(); i++) {
            YdbTable.ColumnMeta column = desc.getColumns(i);
            Type type = ProtoType.fromPb(column.getType());

            if (column.hasFromSequence()) {
                YdbTable.SequenceDescription pbSeq = column.getFromSequence();
                SequenceDescription.Builder sequenceDescriptionBuilder = new SequenceDescription.Builder();

                if (pbSeq.hasName()) {
                    sequenceDescriptionBuilder.setName(pbSeq.getName());
                }
                if (pbSeq.hasMinValue()) {
                    sequenceDescriptionBuilder.setMinValue(pbSeq.getMinValue());
                }
                if (pbSeq.hasMaxValue()) {
                    sequenceDescriptionBuilder.setMaxValue(pbSeq.getMaxValue());
                }
                if (pbSeq.hasStartValue()) {
                    sequenceDescriptionBuilder.setStartValue(pbSeq.getStartValue());
                }
                if (pbSeq.hasCache()) {
                    sequenceDescriptionBuilder.setCache(pbSeq.getCache());
                }
                if (pbSeq.hasIncrement()) {
                    sequenceDescriptionBuilder.setIncrement(pbSeq.getIncrement());
                }
                if (pbSeq.hasCycle()) {
                    sequenceDescriptionBuilder.setCycle(pbSeq.getCycle());
                }

                description.addColumn(new TableColumn(column.getName(), type, column.getFamily(),
                        sequenceDescriptionBuilder.build()));

                continue;
            }

            description.addColumn(new TableColumn(column.getName(), type, column.getFamily(),
                    column.hasFromLiteral() ? (PrimitiveValue)
                            ProtoValue.fromPb(type, column.getFromLiteral().getValue()) : null)
            );
        }
        description.setPrimaryKeys(desc.getPrimaryKeyList());
        for (int i = 0; i < desc.getIndexesCount(); i++) {
            YdbTable.TableIndexDescription idx = desc.getIndexes(i);

            if (idx.hasGlobalIndex()) {
                description.addGlobalIndex(idx.getName(), idx.getIndexColumnsList(), idx.getDataColumnsList());
            }

            if (idx.hasGlobalAsyncIndex()) {
                description.addGlobalAsyncIndex(idx.getName(), idx.getIndexColumnsList(), idx.getDataColumnsList());
            }

            if (idx.hasGlobalUniqueIndex()) {
                description.addGlobalUniqueIndex(idx.getName(), idx.getIndexColumnsList(), idx.getDataColumnsList());
            }
        }
        YdbTable.TableStats tableStats = desc.getTableStats();
        if (settings.isIncludeTableStats()) {
            Timestamp creationTime = tableStats.getCreationTime();
            Instant createdAt = Instant.ofEpochSecond(creationTime.getSeconds(), creationTime.getNanos());
            Timestamp modificationTime = tableStats.getCreationTime();
            Instant modifiedAt = Instant.ofEpochSecond(modificationTime.getSeconds(), modificationTime.getNanos());
            TableDescription.TableStats stats = new TableDescription.TableStats(
                    createdAt, modifiedAt, tableStats.getRowsEstimate(), tableStats.getStoreSize());
            description.setTableStats(stats);

            List<YdbTable.PartitionStats> partitionStats = tableStats.getPartitionStatsList();
            if (settings.isIncludePartitionStats()) {
                for (YdbTable.PartitionStats stat : partitionStats) {
                    description.addPartitionStat(stat.getRowsEstimate(), stat.getStoreSize());
                }
            }
        }
        YdbTable.PartitioningSettings protoPs = desc.getPartitioningSettings();
        PartitioningSettings ps = new PartitioningSettings();
        ps.setPartitionSize(protoPs.getPartitionSizeMb());
        ps.setMinPartitionsCount(protoPs.getMinPartitionsCount());
        ps.setMaxPartitionsCount(protoPs.getMaxPartitionsCount());
        ps.setPartitioningByLoad(protoPs.getPartitioningByLoad() == CommonProtos.FeatureFlag.Status.ENABLED);
        ps.setPartitioningBySize(protoPs.getPartitioningBySize() == CommonProtos.FeatureFlag.Status.ENABLED);
        description.setPartitioningSettings(ps);

        List<YdbTable.ColumnFamily> columnFamiliesList = desc.getColumnFamiliesList();
        for (YdbTable.ColumnFamily family : columnFamiliesList) {
            ColumnFamily.Compression compression;
            switch (family.getCompression()) {
                case COMPRESSION_LZ4:
                    compression = ColumnFamily.Compression.COMPRESSION_LZ4;
                    break;
                default:
                    compression = ColumnFamily.Compression.COMPRESSION_NONE;
            }
            description.addColumnFamily(
                    new ColumnFamily(family.getName(), new StoragePool(family.getData().getMedia()), compression)
            );
        }
        if (settings.isIncludeShardKeyBounds()) {
            List<ValueProtos.TypedValue> shardKeyBoundsList = desc.getShardKeyBoundsList();
            Optional<Value<?>> leftValue = Optional.empty();
            for (TypedValue typedValue : shardKeyBoundsList) {
                Optional<KeyBound> fromBound = leftValue.map(KeyBound::inclusive);
                Value<?> value = ProtoValue.fromPb(
                        ProtoType.fromPb(typedValue.getType()),
                        typedValue.getValue()
                );
                Optional<KeyBound> toBound = Optional.of(KeyBound.exclusive(value));
                description.addKeyRange(new KeyRange(fromBound, toBound));
                leftValue = Optional.of(value);
            }
            description.addKeyRange(
                    new KeyRange(leftValue.map(KeyBound::inclusive), Optional.empty())
            );
        }

        description.setTtlSettings(mapTtlSettings(desc.getTtlSettings()));
        for (YdbTable.ChangefeedDescription pb : desc.getChangefeedsList()) {
            description.addChangefeed(new ChangefeedDescription(
                    pb.getName(),
                    mapChangefeedMode(pb.getMode()),
                    mapChangefeedFormat(pb.getFormat()),
                    mapChangefeedState(pb.getState()),
                    pb.getVirtualTimestamps(),
                    ProtobufUtils.protoToDuration(pb.getResolvedTimestampsInterval())
            ));
        }

        return Result.success(description.build());
    }

    private static YdbTable.TransactionSettings txSettings(Transaction.Mode transactionMode) {
        YdbTable.TransactionSettings.Builder settings = YdbTable.TransactionSettings.newBuilder();
        if (transactionMode != null) {
            switch (transactionMode) {
                case SERIALIZABLE_READ_WRITE:
                    settings.setSerializableReadWrite(YdbTable.SerializableModeSettings.getDefaultInstance());
                    break;
                case ONLINE_READ_ONLY:
                    settings.setOnlineReadOnly(YdbTable.OnlineModeSettings.getDefaultInstance());
                    break;
                case STALE_READ_ONLY:
                    settings.setStaleReadOnly(YdbTable.StaleModeSettings.getDefaultInstance());
                    break;
                case SNAPSHOT_READ_ONLY:
                    settings.setSnapshotReadOnly(YdbTable.SnapshotModeSettings.getDefaultInstance());
                    break;
                default:
                    break;
            }
        }
        return settings.build();
    }

    private static Result<TableOptionDescription> mapDescribeTableOptions(
            Result<YdbTable.DescribeTableOptionsResult> describeTableOptionsResult) {
        if (!describeTableOptionsResult.isSuccess()) {
            return describeTableOptionsResult.map(r -> null);
        }

        YdbTable.DescribeTableOptionsResult describeResult = describeTableOptionsResult.getValue();

        TableOptionDescription.Builder builder = TableOptionDescription.newBuilder();

        List<TableOptionDescription.TableProfileDescription> tableProfileDescriptions = new ArrayList<>();
        builder.setTableProfileDescriptions(tableProfileDescriptions);
        for (YdbTable.TableProfileDescription tableProfileDescription : describeResult.getTableProfilePresetsList()) {
            TableOptionDescription.TableProfileDescription.Builder descBuilder =
                    getDescBuilder(tableProfileDescription);

            TableOptionDescription.TableProfileDescription description = descBuilder.build();
            tableProfileDescriptions.add(description);
        }

        List<TableOptionDescription.StoragePolicyDescription> storagePolicyDescription = new ArrayList<>();
        builder.setStoragePolicyPresets(storagePolicyDescription);
        for (YdbTable.StoragePolicyDescription iter : describeResult.getStoragePolicyPresetsList()) {
            storagePolicyDescription.add(
                    new TableOptionDescription.StoragePolicyDescription(iter.getName(), iter.getLabelsMap()));
        }

        List<TableOptionDescription.CompactionPolicyDescription> compactionPolicyDescription = new ArrayList<>();
        builder.setCompactionPolicyPresets(compactionPolicyDescription);
        for (YdbTable.CompactionPolicyDescription iter : describeResult.getCompactionPolicyPresetsList()) {
            compactionPolicyDescription.add(
                    new TableOptionDescription.CompactionPolicyDescription(iter.getName(), iter.getLabelsMap()));
        }

        List<TableOptionDescription.PartitioningPolicyDescription> partitioningPolicyPresets = new ArrayList<>();
        builder.setPartitioningPolicyPresets(partitioningPolicyPresets);
        for (YdbTable.PartitioningPolicyDescription iter : describeResult.getPartitioningPolicyPresetsList()) {
            partitioningPolicyPresets.add(
                    new TableOptionDescription.PartitioningPolicyDescription(iter.getName(), iter.getLabelsMap()));
        }

        List<TableOptionDescription.ExecutionPolicyDescription> executionPolicyDescriptions = new ArrayList<>();
        builder.setExecutionPolicyPresets(executionPolicyDescriptions);
        for (YdbTable.ExecutionPolicyDescription iter : describeResult.getExecutionPolicyPresetsList()) {
            executionPolicyDescriptions.add(
                    new TableOptionDescription.ExecutionPolicyDescription(iter.getName(), iter.getLabelsMap()));
        }

        List<TableOptionDescription.ReplicationPolicyDescription> replicationPolicyPresets = new ArrayList<>();
        builder.setReplicationPolicyPresets(replicationPolicyPresets);
        for (YdbTable.ReplicationPolicyDescription iter : describeResult.getReplicationPolicyPresetsList()) {
            replicationPolicyPresets.add(
                    new TableOptionDescription.ReplicationPolicyDescription(iter.getName(), iter.getLabelsMap()));
        }

        List<TableOptionDescription.CachingPolicyDescription> cachingPolicyPresets = new ArrayList<>();
        builder.setCachingPolicyPresets(cachingPolicyPresets);
        for (YdbTable.CachingPolicyDescription iter : describeResult.getCachingPolicyPresetsList()) {
            cachingPolicyPresets.add(
                    new TableOptionDescription.CachingPolicyDescription(iter.getName(), iter.getLabelsMap()));
        }

        return Result.success(new TableOptionDescription(builder));
    }

    private static TableOptionDescription.TableProfileDescription.Builder getDescBuilder(
            YdbTable.TableProfileDescription tableProfileDescription) {
        TableOptionDescription.TableProfileDescription.Builder descBuilder =
                TableOptionDescription.TableProfileDescription.newBuilder();
        descBuilder.setName(tableProfileDescription.getName());
        descBuilder.setLabels(tableProfileDescription.getLabelsMap());

        descBuilder.setDefaultStoragePolicy(tableProfileDescription.getDefaultStoragePolicy());
        descBuilder.setDefaultCompactionPolicy(tableProfileDescription.getDefaultCompactionPolicy());
        descBuilder.setDefaultPartitioningPolicy(tableProfileDescription.getDefaultPartitioningPolicy());
        descBuilder.setDefaultExecutionPolicy(tableProfileDescription.getDefaultExecutionPolicy());
        descBuilder.setDefaultReplicationPolicy(tableProfileDescription.getDefaultReplicationPolicy());
        descBuilder.setDefaultCachingPolicy(tableProfileDescription.getDefaultCachingPolicy());

        descBuilder.setAllowedStoragePolicy(tableProfileDescription.getAllowedStoragePoliciesList());
        descBuilder.setAllowedCompactionPolicy(tableProfileDescription.getAllowedCompactionPoliciesList());
        descBuilder.setAllowedPartitioningPolicy(tableProfileDescription.getAllowedPartitioningPoliciesList());
        descBuilder.setAllowedExecutionPolicy(tableProfileDescription.getAllowedExecutionPoliciesList());
        descBuilder.setAllowedReplicationPolicy(tableProfileDescription.getAllowedReplicationPoliciesList());
        descBuilder.setAllowedCachingPolicy(tableProfileDescription.getAllowedCachingPoliciesList());
        return descBuilder;
    }


    protected CompletableFuture<Result<DataQueryResult>> executeDataQueryInternal(
            String query, YdbTable.TransactionControl txControl, Params params, ExecuteDataQuerySettings settings) {
        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxControl(txControl)
                .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
                .setCollectStats(settings.collectStats().toPb())
                .putAllParameters(params.toPb());

        final boolean keepInServerQueryCache = settings.isKeepInQueryCache();
        if (keepInServerQueryCache) {
            request.getQueryCachePolicyBuilder()
                    .setKeepInCache(true);
        }

        String msg = "query";
        if (logger.isDebugEnabled() && keepQueryText) {
            StringBuilder sb = new StringBuilder(query.replaceAll("\\s", " "));
            if (!params.isEmpty()) {
                sb.append(" [");
                boolean one = true;
                for (Map.Entry<String, Value<?>> entry : params.values().entrySet()) {
                    if (!one) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    one = false;
                }
                sb.append("]");
            }
            msg = sb.toString();
        }

        return interceptResultWithLog(msg, rpc.executeDataQuery(request.build(), makeOptions(settings).build()))
                .thenApply(result -> result.map(DataQueryResult::new));
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
            String query, TxControl<?> txControl, Params params, ExecuteDataQuerySettings settings) {
        return executeDataQueryInternal(query, txControl.toPb(), params, settings);
    }

    @Override
    public CompletableFuture<Result<ReadRowsResult>> readRows(String pathToTable, ReadRowsSettings settings) {
        YdbTable.ReadRowsRequest.Builder requestBuilder = YdbTable.ReadRowsRequest.newBuilder()
                .setSessionId(id)
                .setPath(pathToTable)
                .addAllColumns(settings.getColumns())
                .setKeys(settings.getKeys().isEmpty() ? TypedValue.newBuilder().build() :
                        ValueProtos.TypedValue.newBuilder()
                                .setType(ListType.of(settings.getKeys().get(0).getType()).toPb())
                                .setValue(ValueProtos.Value.newBuilder()
                                        .addAllItems(settings.getKeys().stream().map(StructValue::toPb)
                                                .collect(Collectors.toList())))
                                .build());
        return interceptResult(rpc.readRows(requestBuilder.build(), makeOptions(settings).build()))
                .thenApply(result -> result.map(ReadRowsResult::new));
    }

    CompletableFuture<Result<DataQueryResult>> executePreparedDataQuery(String queryId, @Nullable String queryText,
                                                                        TxControl<?> txControl, Params params,
                                                                        ExecuteDataQuerySettings settings) {
        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxControl(txControl.toPb())
                .setCollectStats(settings.collectStats().toPb());

        request.getQueryBuilder().setId(queryId);
        request.putAllParameters(params.toPb());

        final boolean keepInServerQueryCache = settings.isKeepInQueryCache();
        if (keepInServerQueryCache) {
            request.getQueryCachePolicyBuilder()
                    .setKeepInCache(true);
        }

        String msg = "prepared query";
        if (logger.isDebugEnabled() && keepQueryText) {
            StringBuilder sb = new StringBuilder("prepared,");
            if (queryText != null) {
                sb.append(queryText.replaceAll("\\s", " "));
            }
            if (!params.isEmpty()) {
                sb.append(" [");
                boolean one = true;
                for (Map.Entry<String, Value<?>> entry : params.values().entrySet()) {
                    if (!one) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    one = false;
                }
                sb.append("]");
            }
            msg = sb.toString();
        }

        return interceptResultWithLog(msg, rpc.executeDataQuery(request.build(), makeOptions(settings).build()))
                .thenApply(result -> result.map(DataQueryResult::new));
    }

    @Override
    public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings) {
        YdbTable.PrepareDataQueryRequest.Builder request = YdbTable.PrepareDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setYqlText(query);

        return interceptResult(rpc.prepareDataQuery(request.build(), makeOptions(settings).build()))
                .thenApply(result -> result.map((value) -> {
                    String queryId = value.getQueryId();
                    Map<String, ValueProtos.Type> types = value.getParametersTypesMap();
                    return new DataQueryImpl(this, queryId, query, keepQueryText, types);
                }));
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
        YdbTable.ExecuteSchemeQueryRequest request = YdbTable.ExecuteSchemeQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setYqlText(query)
                .build();

        return interceptStatus(rpc.executeSchemeQuery(request, makeOptions(settings).build()));
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query,
                                                                              ExplainDataQuerySettings settings) {
        YdbTable.ExplainDataQueryRequest request = YdbTable.ExplainDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setYqlText(query)
                .build();

        return interceptResult(rpc.explainDataQuery(request, makeOptions(settings).build()))
                .thenApply(result -> result.map(ExplainDataQueryResult::new));
    }

    @Override
    public CompletableFuture<Result<Transaction>> beginTransaction(Transaction.Mode transactionMode,
                                                                   BeginTxSettings settings) {
        YdbTable.BeginTransactionRequest request = YdbTable.BeginTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxSettings(txSettings(transactionMode))
                .build();

        return interceptResultWithLog("begin transaction",
                rpc.beginTransaction(request, makeOptions(settings).build()))
                .thenApply(result -> result.map(tx -> new DeprecatedTransactionImpl(tx.getTxMeta().getId())));
    }

    @Override
    public TableTransaction createNewTransaction(TxMode txMode) {
        return new TableTransactionImpl(txMode, null);
    }

    @Override
    public CompletableFuture<Result<TableTransaction>> beginTransaction(TxMode txMode, BeginTxSettings settings) {
        YdbTable.BeginTransactionRequest request = YdbTable.BeginTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxSettings(TxControlToPb.txSettings(txMode))
                .build();

        return interceptResultWithLog(
                "begin transaction", rpc.beginTransaction(request, makeOptions(settings).build())
        ).thenApply(result -> result.map(tx -> new TableTransactionImpl(txMode, tx.getTxMeta().getId())));
    }

    @Override
    public GrpcReadStream<ReadTablePart> executeReadTable(String tablePath, ReadTableSettings settings) {
        YdbTable.ReadTableRequest.Builder request = YdbTable.ReadTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(tablePath)
                .setOrdered(settings.isOrdered())
                .setRowLimit(settings.getRowLimit())
                .setBatchLimitBytes(settings.batchLimitBytes())
                .setBatchLimitRows(settings.batchLimitRows());

        Value<?> fromKey = settings.getFromKey();
        if (fromKey != null) {
            YdbTable.KeyRange.Builder range = request.getKeyRangeBuilder();
            if (settings.isFromInclusive()) {
                range.setGreaterOrEqual(ProtoValue.toTypedValue(fromKey));
            } else {
                range.setGreater(ProtoValue.toTypedValue(fromKey));
            }
        }

        Value<?> toKey = settings.getToKey();
        if (toKey != null) {
            YdbTable.KeyRange.Builder range = request.getKeyRangeBuilder();
            if (settings.isToInclusive()) {
                range.setLessOrEqual(ProtoValue.toTypedValue(toKey));
            } else {
                range.setLess(ProtoValue.toTypedValue(toKey));
            }
        }

        if (!settings.getColumns().isEmpty()) {
            request.addAllColumns(settings.getColumns());
        }

        GrpcRequestSettings.Builder options = makeOptions(settings);
        if (settings.getGrpcFlowControl() != null) {
            options = options.withFlowControl(settings.getGrpcFlowControl());
        }

        GrpcReadStream<YdbTable.ReadTableResponse> origin = rpc.streamReadTable(request.build(), options.build());
        return new ProxyStream<YdbTable.ReadTableResponse, ReadTablePart>(origin) {
            @Override
            StatusIds.StatusCode readStatusCode(YdbTable.ReadTableResponse message) {
                return message.getStatus();
            }

            @Override
            List<YdbIssueMessage.IssueMessage> readIssues(YdbTable.ReadTableResponse message) {
                return message.getIssuesList();
            }

            @Override
            ReadTablePart readValue(YdbTable.ReadTableResponse message) {
                return new ReadTablePart(message.getResult(), message.getSnapshot());
            }
        };
    }

    @Override
    public GrpcReadStream<ResultSetReader> executeScanQuery(String query, Params params,
                                                            ExecuteScanQuerySettings settings) {
        YdbTable.ExecuteScanQueryRequest req = YdbTable.ExecuteScanQueryRequest.newBuilder()
                .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
                .setMode(settings.getMode().toPb())
                .putAllParameters(params.toPb())
                .setCollectStats(settings.getCollectStats().toPb())
                .build();

        GrpcRequestSettings.Builder opts = makeOptions(settings);
        if (settings.getGrpcFlowControl() != null) {
            opts = opts.withFlowControl(settings.getGrpcFlowControl());
        }

        GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> origin = rpc.streamExecuteScanQuery(req, opts.build());
        return new ProxyStream<YdbTable.ExecuteScanQueryPartialResponse, ResultSetReader>(origin) {
            @Override
            StatusIds.StatusCode readStatusCode(YdbTable.ExecuteScanQueryPartialResponse message) {
                return message.getStatus();
            }

            @Override
            List<YdbIssueMessage.IssueMessage> readIssues(YdbTable.ExecuteScanQueryPartialResponse message) {
                return message.getIssuesList();
            }

            @Override
            ResultSetReader readValue(YdbTable.ExecuteScanQueryPartialResponse message) {
                return ProtoValueReaders.forResultSet(message.getResult().getResultSet());
            }
        };
    }

    private CompletableFuture<Status> commitTransactionInternal(String txId, CommitTxSettings settings) {
        YdbTable.CommitTransactionRequest request = YdbTable.CommitTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxId(txId)
                .build();

        CompletableFuture<Status> future = rpc.commitTransaction(request, makeOptions(settings).build());
        return interceptStatusWithLog("commit transaction", future);
    }

    @Override
    public CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings) {
        return commitTransactionInternal(txId, settings);
    }

    private CompletableFuture<Status> rollbackTransactionInternal(String txId, RollbackTxSettings settings) {
        YdbTable.RollbackTransactionRequest request = YdbTable.RollbackTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxId(txId)
                .build();

        CompletableFuture<Status> future = rpc.rollbackTransaction(request, makeOptions(settings).build());
        return interceptStatusWithLog("rollback transaction", future);
    }

    @Override
    @Deprecated
    public CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings) {
        return rollbackTransactionInternal(txId, settings);
    }

    @Override
    public CompletableFuture<Result<State>> keepAlive(KeepAliveSessionSettings settings) {
        YdbTable.KeepAliveRequest request = YdbTable.KeepAliveRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        GrpcRequestSettings config = makeOptions(settings).build();
        CompletableFuture<Result<YdbTable.KeepAliveResult>> future = rpc.keepAlive(request, config);
        return interceptResult(future).thenApply(result -> result.map(BaseSession::mapSessionStatus));
    }

    @Override
    public CompletableFuture<Status> executeBulkUpsert(String tablePath, ListValue rows, BulkUpsertSettings settings) {
        ValueProtos.TypedValue typedRows = ValueProtos.TypedValue.newBuilder()
                .setType(rows.getType().toPb())
                .setValue(rows.toPb())
                .build();

        YdbTable.BulkUpsertRequest request = YdbTable.BulkUpsertRequest.newBuilder()
                .setTable(tablePath)
                .setRows(typedRows)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        return interceptStatus(rpc.bulkUpsert(request, makeOptions(settings).build()));
    }

    private static State mapSessionStatus(YdbTable.KeepAliveResult result) {
        switch (result.getSessionStatus()) {
            case UNRECOGNIZED:
            case SESSION_STATUS_UNSPECIFIED:
                return State.UNSPECIFIED;
            case SESSION_STATUS_BUSY:
                return State.BUSY;
            case SESSION_STATUS_READY:
                return State.READY;
            default:
                throw new IllegalStateException("unknown session status: " + result.getSessionStatus());
        }
    }

    public CompletableFuture<Status> delete(DeleteSessionSettings settings) {
        YdbTable.DeleteSessionRequest request = YdbTable.DeleteSessionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        return interceptStatus(rpc.deleteSession(request, makeOptions(settings).build()));
    }

    private <T> CompletableFuture<Result<T>> interceptResultWithLog(String msg, CompletableFuture<Result<T>> future) {
        final long start = Instant.now().toEpochMilli();
        return future.whenComplete((r, t) -> {
            long ms = Instant.now().toEpochMilli() - start;
            logger.debug("Session[{}] {} => {}, took {} ms", getId(), msg, r.getStatus().getCode(), ms);
            updateSessionState(t, r.getStatus().getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    private <T> CompletableFuture<Result<T>> interceptResult(CompletableFuture<Result<T>> future) {
        return future.whenComplete((r, t) -> {
            updateSessionState(t, r.getStatus().getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    private CompletableFuture<Status> interceptStatus(CompletableFuture<Status> future) {
        return future.whenComplete((r, t) -> {
            updateSessionState(t, r.getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    private CompletableFuture<Status> interceptStatusWithLog(String msg, CompletableFuture<Status> future) {
        final long start = Instant.now().toEpochMilli();
        return future.whenComplete((r, t) -> {
            long ms = Instant.now().toEpochMilli() - start;
            logger.debug("Session[{}] {} => {}, took {} ms", getId(), msg, r.getCode(), ms);
            updateSessionState(t, r.getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    protected abstract void updateSessionState(Throwable th, StatusCode code, boolean shutdownHint);

    @Override
    public String toString() {
        return "Session{" + id + "}";
    }

    private abstract class ProxyStream<R, T> implements GrpcReadStream<T> {
        private final GrpcReadStream<R> origin;
        private final CompletableFuture<Status> result;
        private final UpdatableOptional<Status> operationStatus = new UpdatableOptional<>();
        private final UpdatableOptional<Throwable> operationError = new UpdatableOptional<>();

        ProxyStream(GrpcReadStream<R> origin) {
            this.origin = origin;
            this.result = new CompletableFuture<>();
        }

        abstract StatusIds.StatusCode readStatusCode(R message);

        abstract List<YdbIssueMessage.IssueMessage> readIssues(R message);

        abstract T readValue(R message);

        private void onClose(Status streamStatus, Throwable streamError) {
            Throwable th = operationError.orElse(streamError);
            if (th != null) {
                updateSessionState(th, null, false);
                result.completeExceptionally(th);
                return;
            }

            Status st = operationStatus.orElse(streamStatus);
            if (st != null) {
                updateSessionState(null, st.getCode(), false);
                result.complete(st);
            }
        }

        @Override
        public CompletableFuture<Status> start(Observer<T> observer) {
            origin.start(message -> {
                StatusIds.StatusCode code = readStatusCode(message);
                if (code == StatusIds.StatusCode.SUCCESS) {
                    try {
                        observer.onNext(readValue(message));
                    } catch (Throwable th) {
                        operationError.update(th);
                        origin.cancel();
                    }
                } else {
                    operationStatus.update(Status.of(StatusCode.fromProto(code), Issue.fromPb(readIssues(message))));
                    origin.cancel();
                }
            }).whenComplete(this::onClose);
            return result;
        }

        @Override
        public void cancel() {
            origin.cancel();
        }
    }

    class TableTransactionImpl extends YdbTransactionImpl implements TableTransaction {

        TableTransactionImpl(TxMode txMode, String txId) {
            super(txMode, txId);
        }

        @Override
        public String getSessionId() {
            return id;
        }

        @Override
        public Session getSession() {
            return BaseSession.this;
        }

        @Override
        public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
                String query, boolean commitAtEnd, Params params, ExecuteDataQuerySettings settings) {
            // If we intend to commit, statusFuture is reset to reflect only future actions in transaction
            CompletableFuture<Status> currentStatusFuture = commitAtEnd
                    ? statusFuture.getAndSet(new CompletableFuture<>())
                    : statusFuture.get();
            final String currentId = txId.get();
            YdbTable.TransactionControl transactionControl = currentId != null
                    ? TxControlToPb.txIdCtrl(currentId, commitAtEnd)
                    : TxControlToPb.txModeCtrl(txMode, commitAtEnd);
            return executeDataQueryInternal(query, transactionControl, params, settings)
                    .whenComplete((result, th) -> {
                        if (th != null) {
                            currentStatusFuture.completeExceptionally(
                                    new RuntimeException("ExecuteDataQuery on transaction failed with exception ", th));
                            setNewId(currentId, null);
                        } else if (result.isSuccess()) {
                            setNewId(currentId, result.getValue().getTxId());
                            if (commitAtEnd) {
                                currentStatusFuture.complete(Status.SUCCESS);
                            }
                        } else {
                            setNewId(currentId, null);
                            currentStatusFuture.complete(Status
                                    .of(StatusCode.ABORTED)
                                    .withIssues(Issue.of("ExecuteDataQuery on transaction failed with status "
                                            + result.getStatus(), Issue.Severity.ERROR)));
                        }
                    });
        }

        private void setNewId(String currentId, String newId) {
            if (!txId.compareAndSet(currentId, newId)) {
                logger.warn("{} Couldn't change transaction id from {} to {}", BaseSession.this, currentId, newId);
            }
        }

        @Override
        public CompletableFuture<Status> commit(CommitTxSettings settings) {
            CompletableFuture<Status> currentStatusFuture = statusFuture.getAndSet(new CompletableFuture<>());
            final String transactionId = txId.get();
            if (transactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                return CompletableFuture.completedFuture(Status.of(StatusCode.SUCCESS, issue));
            }
            return commitTransactionInternal(transactionId, settings).whenComplete(((status, th) -> {
                if (th != null) {
                    currentStatusFuture.completeExceptionally(th);
                } else {
                    currentStatusFuture.complete(status);
                }
            }));
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
            CompletableFuture<Status> currentStatusFuture = statusFuture.getAndSet(new CompletableFuture<>());
            final String transactionId = txId.get();
            if (transactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                return CompletableFuture.completedFuture(Status.of(StatusCode.SUCCESS, issue));
            }
            return rollbackTransactionInternal(transactionId, settings)
                    .whenComplete((status, th) -> currentStatusFuture.complete(Status
                            .of(StatusCode.ABORTED)
                            .withIssues(Issue.of("Transaction was rolled back", Issue.Severity.ERROR))));
        }
    }

    public final class DeprecatedTransactionImpl implements Transaction {
        private final String txId;

        DeprecatedTransactionImpl(String txId) {
            this.txId = txId;
        }

        @Override
        public String getId() {
            return txId;
        }

        @Override
        public CompletableFuture<Status> commit(CommitTxSettings settings) {
            return commitTransactionInternal(txId, settings);
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
            return rollbackTransactionInternal(txId, settings);
        }
    }

    private static class ShutdownHandler implements Consumer<Metadata> {
        private static final String GRACEFUL_SHUTDOWN_HINT = "session-close";
        private volatile boolean needShutdown;

        public boolean isGracefulShutdown() {
            return needShutdown;
        }

        @Override
        public void accept(Metadata metadata) {
            if (metadata == null) {
                return;
            }

            Iterable<String> serverHints = metadata.getAll(YdbHeaders.YDB_SERVER_HINTS);
            if (serverHints != null) {
                for (String value : serverHints) {
                    if (GRACEFUL_SHUTDOWN_HINT.equals(value)) {
                        needShutdown = true;
                        return;
                    }
                }
            }
        }
    }
}
