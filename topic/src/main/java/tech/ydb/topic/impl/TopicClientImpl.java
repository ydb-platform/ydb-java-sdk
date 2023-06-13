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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.MeteringMode;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.impl.AsyncReaderImpl;
import tech.ydb.topic.read.impl.SyncReaderImpl;
import tech.ydb.topic.settings.AlterConsumerSettings;
import tech.ydb.topic.settings.AlterPartitioningSettings;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.WriterSettings;
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
    public CompletableFuture<Status> createTopic(String path, CreateTopicSettings settings) {
        YdbTopic.CreateTopicRequest.Builder requestBuilder = YdbTopic.CreateTopicRequest.newBuilder()
                .setOperationParams(Operations.createParams(settings))
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
                    .setPartitionCountLimit(partitioningSettings.getPartitionCountLimit()));
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
    public CompletableFuture<Status> alterTopic(String path, AlterTopicSettings settings) {
        YdbTopic.AlterTopicRequest.Builder requestBuilder = YdbTopic.AlterTopicRequest.newBuilder()
                .setOperationParams(Operations.createParams(settings))
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
                .setOperationParams(Operations.createParams(settings))
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.dropTopic(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Result<TopicDescription>> describeTopic(String path, DescribeTopicSettings settings) {
        YdbTopic.DescribeTopicRequest request = YdbTopic.DescribeTopicRequest.newBuilder()
                .setOperationParams(Operations.createParams(settings))
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return topicRpc.describeTopic(request, grpcRequestSettings)
                .thenApply(result -> result.map(this::mapDescribeTopic));
    }

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
        description.setPartitioningSettings(PartitioningSettings.newBuilder()
                .setMinActivePartitions(partitioningSettings.getMinActivePartitions())
                .setPartitionCountLimit(partitioningSettings.getPartitionCountLimit())
                .build());

        List<PartitionInfo> partitions = new ArrayList<>();
        for (YdbTopic.DescribeTopicResult.PartitionInfo partition : result.getPartitionsList()) {
            PartitionInfo.Builder partitionBuilder = PartitionInfo.newBuilder()
                    .setPartitionId(partition.getPartitionId())
                    .setActive(partition.getActive())
                    .setChildPartitionIds(partition.getChildPartitionIdsList())
                    .setParentPartitionIds(partition.getParentPartitionIdsList());
            // TODO: read partition stats
            partitions.add(partitionBuilder.build());
        }
        description.setPartitions(partitions);

        SupportedCodecs.Builder supportedCodecsBuilder = SupportedCodecs.newBuilder();
        for (int codec : result.getSupportedCodecs().getCodecsList()) {
            supportedCodecsBuilder.addCodec(codecFromProto(codec));
        }
        description.setSupportedCodecs(supportedCodecsBuilder.build());

        List<Consumer> consumers = new ArrayList<>();
        for (YdbTopic.Consumer consumer : result.getConsumersList()) {
            Consumer.Builder consumerBuilder = Consumer.newBuilder()
                    .setName(consumer.getName())
                    .setImportant(consumer.getImportant())
                    .setReadFrom(ProtobufUtils.protoToInstant(consumer.getReadFrom()))
                    .setAttributes(consumer.getAttributesMap());

            SupportedCodecs.Builder consumerSupportedCodecsBuilder = SupportedCodecs.newBuilder();
            for (int codec : consumer.getSupportedCodecs().getCodecsList()) {
                consumerSupportedCodecsBuilder.addCodec(codecFromProto(codec));
            }
            consumerBuilder.setSupportedCodecs(consumerSupportedCodecsBuilder.build());

            // TODO: set consumer stats

            consumers.add(consumerBuilder.build());
        }
        description.setConsumers(consumers);

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
    public SyncWriter createSyncWriter(WriterSettings settings) {
        return new SyncWriterImpl(topicRpc, settings, compressionExecutor);
    }

    @Override
    public AsyncWriter createAsyncWriter(WriterSettings settings) {
        return new AsyncWriterImpl(topicRpc, settings, compressionExecutor);
    }

    private static Codec codecFromProto(int codec) {
        switch (codec) {
            case YdbTopic.Codec.CODEC_RAW_VALUE:
                return Codec.RAW;
            case YdbTopic.Codec.CODEC_GZIP_VALUE:
                return Codec.GZIP;
            case YdbTopic.Codec.CODEC_LZOP_VALUE:
                return Codec.LZOP;
            case YdbTopic.Codec.CODEC_ZSTD_VALUE:
                return Codec.ZSTD;
            case YdbTopic.Codec.CODEC_CUSTOM_VALUE:
                return Codec.CUSTOM;
            default:
                throw new RuntimeException("Unknown codec value from proto: " + codec);
        }
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
                .putAllAttributes(consumer.getAttributes());
        Boolean important = consumer.isImportant();
        if (important != null) {
            consumerBuilder.setImportant(important);
        }
        SupportedCodecs consumerCodecs = consumer.getSupportedCodecs();
        if (consumerCodecs != null) {
            consumerBuilder.setSupportedCodecs(toProto(consumerCodecs));
        }
        Instant readFrom = consumer.getReadFrom();
        if (readFrom != null) {
            consumerBuilder.setReadFrom(ProtobufUtils.instantToProto(readFrom));
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

    @Override
    public void close() {
        logger.debug("TopicClientImpl.close() is called");
        if (defaultCompressionExecutorService != null) {
            defaultCompressionExecutorService.shutdown();
        }
    }
}
