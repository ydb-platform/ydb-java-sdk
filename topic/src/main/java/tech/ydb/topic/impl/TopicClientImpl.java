package tech.ydb.topic.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.ConsumerDescription;
import tech.ydb.topic.description.MeteringMode;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.description.PartitionStats;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.impl.AsyncReaderImpl;
import tech.ydb.topic.read.impl.SyncReaderImpl;
import tech.ydb.topic.settings.AlterAutoPartitioningWriteStrategySettings;
import tech.ydb.topic.settings.AlterConsumerSettings;
import tech.ydb.topic.settings.AlterPartitioningSettings;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.AutoPartitioningWriteStrategySettings;
import tech.ydb.topic.settings.CommitOffsetSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeConsumerSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.ProtoUtils;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.impl.AsyncWriterImpl;
import tech.ydb.topic.write.impl.SyncWriterImpl;

/**
 * @author Nikolay Perfilov
 */
public class TopicClientImpl implements TopicClient {
    private static final Logger logger = LoggerFactory.getLogger(TopicClientImpl.class);
    private static final int DEFAULT_COMPRESSION_THREAD_COUNT = 5;

    private final TopicRpc topicRpc;
    private final Executor compressionExecutor;
    private final ExecutorService defaultCompressionExecutorService;

    TopicClientImpl(TopicClientBuilderImpl builder) {
        this.topicRpc = builder.topicRpc;
        if (builder.compressionExecutor != null) {
            this.defaultCompressionExecutorService = null;
            this.compressionExecutor = builder.compressionExecutor;
        } else {
            this.defaultCompressionExecutorService = Executors.newFixedThreadPool(
                    builder.compressionExecutorThreadCount == null
                            ? DEFAULT_COMPRESSION_THREAD_COUNT
                            : builder.compressionExecutorThreadCount);
            this.compressionExecutor = defaultCompressionExecutorService;
        }
    }

    public static Builder newClient(TopicRpc rpc) {
        return new TopicClientBuilderImpl(rpc);
    }

    private GrpcRequestSettings makeGrpcRequestSettings(BaseRequestSettings settings) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .build();
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<Status> createTopic(String path, CreateTopicSettings settings) {
        YdbTopic.CreateTopicRequest.Builder requestBuilder = YdbTopic.CreateTopicRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings))
                .setPath(path)
                .setRetentionStorageMb(settings.getRetentionStorageMb())
                .setPartitionWriteSpeedBytesPerSecond(settings.getPartitionWriteSpeedBytesPerSecond())
                .setPartitionWriteBurstBytes(settings.getPartitionWriteBurstBytes())
                .putAllAttributes(settings.getAttributes())
                .setMeteringMode(toProto(settings.getMeteringMode()));

        PartitioningSettings partitioningSettings = settings.getPartitioningSettings();
        if (partitioningSettings != null) {
            requestBuilder.setPartitioningSettings(YdbTopic.PartitioningSettings.newBuilder()
                    .setMinActivePartitions(partitioningSettings.getMinActivePartitions())
                    .setPartitionCountLimit(partitioningSettings.getPartitionCountLimit())
                    .setAutoPartitioningSettings(YdbTopic.AutoPartitioningSettings.newBuilder()
                            .setStrategy(toProto(partitioningSettings.getAutoPartitioningStrategy()))));

            AutoPartitioningWriteStrategySettings writeStrategySettings = partitioningSettings
                    .getWriteStrategySettings();

            if (writeStrategySettings != null) {
                requestBuilder.getPartitioningSettingsBuilder().getAutoPartitioningSettingsBuilder()
                        .setPartitionWriteSpeed(YdbTopic.AutoPartitioningWriteSpeedStrategy.newBuilder()
                                .setStabilizationWindow(ProtobufUtils.durationToProto(
                                        writeStrategySettings.getStabilizationWindow()
                                ))
                                .setDownUtilizationPercent(writeStrategySettings.getDownUtilizationPercent())
                                .setUpUtilizationPercent(writeStrategySettings.getUpUtilizationPercent())
                        );
            }
        }

        Duration retentionPeriod = settings.getRetentionPeriod();
        if (retentionPeriod != null) {
            requestBuilder.setRetentionPeriod(ProtobufUtils.durationToProto(retentionPeriod));
        }

        SupportedCodecs supportedCodecs = settings.getSupportedCodecs();
        if (supportedCodecs != null) {
            requestBuilder.setSupportedCodecs(toProto(supportedCodecs));
        }

        for (Consumer consumer : settings.getConsumers()) {
            requestBuilder.addConsumers(toProto(consumer));
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.createTopic(requestBuilder.build(), grpcRequestSettings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<Status> alterTopic(String path, AlterTopicSettings settings) {
        YdbTopic.AlterTopicRequest.Builder requestBuilder = YdbTopic.AlterTopicRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings))
                .setPath(path)/*
                .putAllAttributes(settings.getAttributes())
                .setMeteringMode(toProto(settings.getMeteringMode()))*/;

        AlterPartitioningSettings partitioningSettings = settings.getAlterPartitioningSettings();
        if (partitioningSettings != null) {
            YdbTopic.AlterPartitioningSettings.Builder builder = YdbTopic.AlterPartitioningSettings.newBuilder();
            Long minActivePartitions = partitioningSettings.getMinActivePartitions();
            if (minActivePartitions != null) {
                builder.setSetMinActivePartitions(minActivePartitions);
            }
            Long partitionCountLimit = partitioningSettings.getPartitionCountLimit();
            if (partitionCountLimit != null) {
                builder.setSetPartitionCountLimit(partitionCountLimit);
            }
            AutoPartitioningStrategy autoPartitioningStrategy = partitioningSettings.getAutoPartitioningStrategy();
            if (autoPartitioningStrategy != null) {
                YdbTopic.AutoPartitioningStrategy protoReference = toProto(autoPartitioningStrategy);
                builder.getAlterAutoPartitioningSettingsBuilder().setSetStrategy(protoReference);
            }
            AlterAutoPartitioningWriteStrategySettings writeStrategySettings = partitioningSettings
                    .getWriteStrategySettings();
            if (writeStrategySettings != null) {
                Duration stabilizationWindow = writeStrategySettings.getStabilizationWindow();
                if (stabilizationWindow != null) {
                    builder.getAlterAutoPartitioningSettingsBuilder().getSetPartitionWriteSpeedBuilder()
                            .setSetStabilizationWindow(ProtobufUtils.durationToProto(stabilizationWindow));
                }
                Integer upUtilizationPercent = writeStrategySettings.getUpUtilizationPercent();
                if (upUtilizationPercent != null) {
                    builder.getAlterAutoPartitioningSettingsBuilder().getSetPartitionWriteSpeedBuilder()
                            .setSetUpUtilizationPercent(upUtilizationPercent);
                }
                Integer downUtilizationPercent = writeStrategySettings.getDownUtilizationPercent();
                if (downUtilizationPercent != null) {
                    builder.getAlterAutoPartitioningSettingsBuilder().getSetPartitionWriteSpeedBuilder()
                            .setSetDownUtilizationPercent(downUtilizationPercent);
                }
            }
            requestBuilder.setAlterPartitioningSettings(builder);
        }

        Duration retentionPeriod = settings.getRetentionPeriod();
        if (retentionPeriod != null) {
            requestBuilder.setSetRetentionPeriod(ProtobufUtils.durationToProto(retentionPeriod));
        }

        Long retentionStorageMb = settings.getRetentionStorageMb();
        if (retentionStorageMb != null) {
            requestBuilder.setSetRetentionStorageMb(retentionStorageMb);
        }

        SupportedCodecs supportedCodecs = settings.getSupportedCodecs();
        if (supportedCodecs != null) {
            requestBuilder.setSetSupportedCodecs(toProto(supportedCodecs));
        }

        Long partitionWriteSpeedBytesPerSecond = settings.getPartitionWriteSpeedBytesPerSecond();
        if (partitionWriteSpeedBytesPerSecond != null) {
            requestBuilder.setSetPartitionWriteSpeedBytesPerSecond(partitionWriteSpeedBytesPerSecond);
        }

        Long partitionWriteBurstBytes = settings.getPartitionWriteBurstBytes();
        if (partitionWriteBurstBytes != null) {
            requestBuilder.setSetPartitionWriteBurstBytes(partitionWriteBurstBytes);
        }

        for (Consumer consumer : settings.getAddConsumers()) {
            requestBuilder.addAddConsumers(toProto(consumer));
        }

        for (String dropConsumer : settings.getDropConsumers()) {
            requestBuilder.addDropConsumers(dropConsumer);
        }

        List<AlterConsumerSettings> alterConsumers = settings.getAlterConsumers();
        if (!alterConsumers.isEmpty()) {
            for (AlterConsumerSettings alterConsumer : alterConsumers) {
                YdbTopic.AlterConsumer.Builder alterConsumerBuilder = YdbTopic.AlterConsumer.newBuilder()
                        .setName(alterConsumer.getName());
                Boolean important = alterConsumer.getImportant();
                if (important != null) {
                    alterConsumerBuilder.setSetImportant(important);
                }
                Instant readFrom = alterConsumer.getReadFrom();
                if (readFrom != null) {
                    alterConsumerBuilder.setSetReadFrom(ProtobufUtils.instantToProto(readFrom));
                }

                SupportedCodecs consumerSupportedCodecs = alterConsumer.getSupportedCodecs();
                if (consumerSupportedCodecs != null) {
                    alterConsumerBuilder.setSetSupportedCodecs(toProto(consumerSupportedCodecs));
                }

                Map<String, String> consumerAttributes = alterConsumer.getAlterAttributes();
                if (!consumerAttributes.isEmpty()) {
                    alterConsumerBuilder.putAllAlterAttributes(consumerAttributes);
                }

                for (String attributeToDrop : alterConsumer.getDropAttributes()) {
                    alterConsumerBuilder.putAlterAttributes(attributeToDrop, "");
                }

                requestBuilder.addAlterConsumers(alterConsumerBuilder);
            }
        }

        MeteringMode meteringMode = settings.getMeteringMode();
        if (meteringMode != null) {
            requestBuilder.setSetMeteringMode(toProto(meteringMode));
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.alterTopic(requestBuilder.build(), grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Status> dropTopic(String path, DropTopicSettings settings) {
        YdbTopic.DropTopicRequest request = YdbTopic.DropTopicRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings))
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.dropTopic(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Result<TopicDescription>> describeTopic(String path, DescribeTopicSettings settings) {
        YdbTopic.DescribeTopicRequest request = YdbTopic.DescribeTopicRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings))
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.describeTopic(request, grpcRequestSettings)
                .thenApply(result -> result.map(this::mapDescribeTopic));
    }

    @Override
    public CompletableFuture<Result<ConsumerDescription>> describeConsumer(
            String topicPath, String consumerName, DescribeConsumerSettings settings
    ) {
        YdbTopic.DescribeConsumerRequest request = YdbTopic.DescribeConsumerRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings))
                .setPath(topicPath)
                .setConsumer(consumerName)
                .setIncludeStats(settings.isIncludeStats())
                .setIncludeLocation(settings.isIncludeLocation())
                .build();
        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.describeConsumer(request, grpcRequestSettings)
                .thenApply(result -> result.map(ConsumerDescription::new));
    }

    @SuppressWarnings("deprecation")
    private TopicDescription mapDescribeTopic(YdbTopic.DescribeTopicResult result) {
        if (logger.isTraceEnabled()) {
            logger.trace("Received topic describe response:\n{}", result);
        }
        TopicDescription.Builder description = TopicDescription.newBuilder()
                .setRetentionPeriod(ProtobufUtils.protoToDuration(result.getRetentionPeriod()))
                .setRetentionStorageMb(result.getRetentionStorageMb())
                .setPartitionWriteSpeedBytesPerSecond(result.getPartitionWriteSpeedBytesPerSecond())
                .setPartitionWriteBurstBytes(result.getPartitionWriteBurstBytes())
                .setAttributes(result.getAttributesMap())
                .setMeteringMode(fromProto(result.getMeteringMode()));

        YdbTopic.PartitioningSettings partitioningSettings = result.getPartitioningSettings();
        YdbTopic.AutoPartitioningSettings autoPartitioningSettings = partitioningSettings.getAutoPartitioningSettings();
        YdbTopic.AutoPartitioningStrategy autoPartitioningStrategy = autoPartitioningSettings.getStrategy();

        PartitioningSettings.Builder partitioningDescription = PartitioningSettings.newBuilder()
                .setMinActivePartitions(partitioningSettings.getMinActivePartitions())
                .setPartitionCountLimit(partitioningSettings.getPartitionCountLimit())
                .setAutoPartitioningStrategy(fromProto(autoPartitioningStrategy));

        YdbTopic.AutoPartitioningWriteSpeedStrategy partitionWriteSpeed = autoPartitioningSettings
                .getPartitionWriteSpeed();
        partitioningDescription.setWriteStrategySettings(AutoPartitioningWriteStrategySettings.newBuilder()
                .setStabilizationWindow(ProtobufUtils.protoToDuration(
                        partitionWriteSpeed.getStabilizationWindow()
                ))
                .setUpUtilizationPercent(partitionWriteSpeed.getUpUtilizationPercent())
                .setDownUtilizationPercent(partitionWriteSpeed.getDownUtilizationPercent())
                .build());

        description.setPartitioningSettings(partitioningDescription.build());

        List<PartitionInfo> partitions = new ArrayList<>();
        for (YdbTopic.DescribeTopicResult.PartitionInfo partition : result.getPartitionsList()) {
            PartitionInfo.Builder partitionBuilder = PartitionInfo.newBuilder()
                    .setPartitionId(partition.getPartitionId())
                    .setActive(partition.getActive())
                    .setChildPartitionIds(partition.getChildPartitionIdsList())
                    .setParentPartitionIds(partition.getParentPartitionIdsList())
                    .setPartitionStats(new PartitionStats(partition.getPartitionStats()));

            partitions.add(partitionBuilder.build());
        }
        description.setPartitions(partitions);

        SupportedCodecs.Builder supportedCodecsBuilder = SupportedCodecs.newBuilder();
        for (int codec : result.getSupportedCodecs().getCodecsList()) {
            supportedCodecsBuilder.addCodec(ProtoUtils.codecFromProto(codec));
        }
        description.setSupportedCodecs(supportedCodecsBuilder.build());

        description.setConsumers(result.getConsumersList().stream()
                .map(Consumer::new).collect(Collectors.toList()));

        return description.build();
    }

    @Override
    public SyncReader createSyncReader(ReaderSettings settings) {
        return new SyncReaderImpl(topicRpc, settings);
    }

    @Override
    public AsyncReader createAsyncReader(ReaderSettings settings, ReadEventHandlersSettings handlersSettings) {
        return new AsyncReaderImpl(topicRpc, settings, handlersSettings);
    }

    @Override
    public CompletableFuture<Status> commitOffset(String path, CommitOffsetSettings settings) {
        YdbTopic.CommitOffsetRequest.Builder request = YdbTopic.CommitOffsetRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings))
                .setOffset(settings.getOffset())
                .setConsumer(settings.getConsumer())
                .setPartitionId(settings.getPartitionId())
                .setPath(path);

        if (settings.getReadSessionId() != null) {
            request.setReadSessionId(settings.getReadSessionId());
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.commitOffset(request.build(), grpcRequestSettings);
    }

    @Override
    public SyncWriter createSyncWriter(WriterSettings settings) {
        return new SyncWriterImpl(topicRpc, settings, compressionExecutor);
    }

    @Override
    public AsyncWriter createAsyncWriter(WriterSettings settings) {
        return new AsyncWriterImpl(topicRpc, settings, compressionExecutor);
    }

    private static YdbTopic.MeteringMode toProto(MeteringMode meteringMode) {
        switch (meteringMode) {
            case UNSPECIFIED:
                return YdbTopic.MeteringMode.METERING_MODE_UNSPECIFIED;
            case REQUEST_UNITS:
                return YdbTopic.MeteringMode.METERING_MODE_REQUEST_UNITS;
            case RESERVED_CAPACITY:
                return YdbTopic.MeteringMode.METERING_MODE_RESERVED_CAPACITY;
            default:
                throw new IllegalArgumentException("Unknown metering mode: " + meteringMode);
        }
    }

    private static MeteringMode fromProto(YdbTopic.MeteringMode meteringMode) {
        switch (meteringMode) {
            case METERING_MODE_UNSPECIFIED:
                return MeteringMode.UNSPECIFIED;
            case METERING_MODE_REQUEST_UNITS:
                return MeteringMode.REQUEST_UNITS;
            case METERING_MODE_RESERVED_CAPACITY:
                return MeteringMode.RESERVED_CAPACITY;
            default:
                throw new RuntimeException("Unknown metering mode from proto: " + meteringMode);
        }
    }

    private static YdbTopic.Consumer toProto(Consumer consumer) {
        YdbTopic.Consumer.Builder consumerBuilder = YdbTopic.Consumer.newBuilder()
                .setName(consumer.getName())
                .setImportant(consumer.isImportant())
                .putAllAttributes(consumer.getAttributes());

        if (consumer.getReadFrom() != null) {
            consumerBuilder.setReadFrom(ProtobufUtils.instantToProto(consumer.getReadFrom()));
        }

        List<Codec> supportedCodecs = consumer.getSupportedCodecsList();
        if (!supportedCodecs.isEmpty()) {
            YdbTopic.SupportedCodecs.Builder codecBuilder = YdbTopic.SupportedCodecs.newBuilder();
            supportedCodecs.forEach(codec -> codecBuilder.addCodecs(ProtoUtils.toProto(codec)));
            consumerBuilder.setSupportedCodecs(codecBuilder.build());
        }

        return consumerBuilder.build();
    }

    private static YdbTopic.SupportedCodecs toProto(SupportedCodecs supportedCodecs) {
        List<Codec> supportedCodecsList = supportedCodecs.getCodecs();
        YdbTopic.SupportedCodecs.Builder codecsBuilder = YdbTopic.SupportedCodecs.newBuilder();
        for (Codec codec : supportedCodecsList) {
            codecsBuilder.addCodecs(tech.ydb.topic.utils.ProtoUtils.toProto(codec));
        }
        return codecsBuilder.build();
    }

    private static AutoPartitioningStrategy fromProto(YdbTopic.AutoPartitioningStrategy autoPartitioningStrategy) {
        switch (autoPartitioningStrategy) {
            case AUTO_PARTITIONING_STRATEGY_PAUSED:
                return AutoPartitioningStrategy.PAUSED;
            case AUTO_PARTITIONING_STRATEGY_SCALE_UP:
                return AutoPartitioningStrategy.SCALE_UP;
            case AUTO_PARTITIONING_STRATEGY_SCALE_UP_AND_DOWN:
                return AutoPartitioningStrategy.SCALE_UP_AND_DOWN;
            case AUTO_PARTITIONING_STRATEGY_DISABLED:
                return AutoPartitioningStrategy.DISABLED;
            default:
                return null;
        }
    }

    private static YdbTopic.AutoPartitioningStrategy toProto(AutoPartitioningStrategy autoPartitioningStrategy) {
        switch (autoPartitioningStrategy) {
            case PAUSED:
                return YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_PAUSED;
            case SCALE_UP:
                return YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_SCALE_UP;
            case SCALE_UP_AND_DOWN:
                return YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_SCALE_UP_AND_DOWN;
            case DISABLED:
                return YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_DISABLED;
            default:
                throw new IllegalArgumentException("Unknown auto partitioning strategy: " + autoPartitioningStrategy);
        }
    }

    @Override
    public void close() {
        logger.debug("TopicClientImpl.close() is called");
        if (defaultCompressionExecutorService != null) {
            defaultCompressionExecutorService.shutdown();
        }
    }
}
