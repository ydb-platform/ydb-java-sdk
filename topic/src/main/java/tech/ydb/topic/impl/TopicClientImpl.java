package tech.ydb.topic.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.utils.GrpcUtils;
import tech.ydb.core.utils.ProtoUtils;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.MeteringMode;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;

/**
 * @author Nikolay Perfilov
 */
public class TopicClientImpl implements TopicClient {

    private final TopicRpc topicRpc;

    TopicClientImpl(TopicClientBuilderImpl builder) {
        this.topicRpc = builder.topicRpc;
    }

    public static Builder newClient(TopicRpc rpc) {
        return new TopicClientBuilderImpl(rpc);
    }

    @Override
    public CompletableFuture<Status> createTopic(String path, CreateTopicSettings settings) {
        YdbTopic.CreateTopicRequest.Builder requestBuilder = YdbTopic.CreateTopicRequest.newBuilder()
                .setOperationParams(ProtoUtils.fromRequestSettings(settings))
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
            requestBuilder.setRetentionPeriod(ProtoUtils.toProto(retentionPeriod));
        }

        List<Codec> supportedCodecs = settings.getSupportedCodecs().getCodecs();
        if (!supportedCodecs.isEmpty()) {
            YdbTopic.SupportedCodecs.Builder codecsBuilder = YdbTopic.SupportedCodecs.newBuilder();
            for (Codec codec : supportedCodecs) {
                codecsBuilder.addCodecs(toProto(codec));
            }
            requestBuilder.setSupportedCodecs(codecsBuilder);
        }

        for (Consumer consumer : settings.getConsumers()) {
            YdbTopic.Consumer.Builder consumerBuilder =  YdbTopic.Consumer.newBuilder()
                    .setName(consumer.getName())
                    .setImportant(consumer.isImportant())
                    .setReadFrom(ProtoUtils.toProto(consumer.getReadFrom()))
                    .putAllAttributes(consumer.getAttributes());

            List<Codec> consumerCodecs = settings.getSupportedCodecs().getCodecs();
            if (!consumerCodecs.isEmpty()) {
                YdbTopic.SupportedCodecs.Builder builder = YdbTopic.SupportedCodecs.newBuilder();
                for (Codec codec : consumerCodecs) {
                    builder.addCodecs(toProto(codec));
                }
                requestBuilder.setSupportedCodecs(builder);
            }
            requestBuilder.addConsumers(consumerBuilder);
        }

        final GrpcRequestSettings grpcRequestSettings = GrpcUtils.makeGrpcRequestSettingsBuilder(settings).build();
        return topicRpc.createTopic(requestBuilder.build(), grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Status> dropTopic(String path, DropTopicSettings settings) {
        YdbTopic.DropTopicRequest request = YdbTopic.DropTopicRequest.newBuilder()
                .setOperationParams(ProtoUtils.fromRequestSettings(settings))
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = GrpcUtils.makeGrpcRequestSettingsBuilder(settings).build();
        return topicRpc.dropTopic(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Result<TopicDescription>> describeTopic(String path, DescribeTopicSettings settings) {
        YdbTopic.DescribeTopicRequest request = YdbTopic.DescribeTopicRequest.newBuilder()
                .setOperationParams(ProtoUtils.fromRequestSettings(settings))
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = GrpcUtils.makeGrpcRequestSettingsBuilder(settings).build();
        return topicRpc.describeTopic(request, grpcRequestSettings)
                .thenApply(result -> result.map(desc -> mapDescribeTopic(desc)));
    }

    private TopicDescription mapDescribeTopic(YdbTopic.DescribeTopicResult result) {
        TopicDescription.Builder description = TopicDescription.newBuilder()
                .setRetentionPeriod(ProtoUtils.fromProto(result.getRetentionPeriod()))
                .setRetentionStorageMb(result.getRetentionStorageMb())
                .setPartitionWriteSpeedBytesPerSecond(result.getPartitionWriteSpeedBytesPerSecond())
                .setPartitionWriteBurstBytes(result.getPartitionWriteBurstBytes())
                .setAttributes(result.getAttributesMap())
                .setMeteringMode(fromProto(result.getMeteringMode()));

        YdbTopic.PartitioningSettings partitioningSettings = result.getPartitioningSettings();
        description.setPartitioningSettings(new PartitioningSettings(partitioningSettings.getMinActivePartitions(),
                partitioningSettings.getPartitionCountLimit()));

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
                    .setReadFrom(ProtoUtils.fromProto(consumer.getReadFrom()))
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

    private static int toProto(Codec codec) {
        switch (codec) {
            case RAW:
                return YdbTopic.Codec.CODEC_RAW_VALUE;
            case GZIP:
                return  YdbTopic.Codec.CODEC_GZIP_VALUE;
            case LZOP:
                return  YdbTopic.Codec.CODEC_LZOP_VALUE;
            case ZSTD:
                return  YdbTopic.Codec.CODEC_ZSTD_VALUE;
            case CUSTOM:
                return  YdbTopic.Codec.CODEC_CUSTOM_VALUE;
            default:
                throw new IllegalArgumentException("Unknown codec value: " + codec);
        }
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

    @Override
    public void close() {
        topicRpc.close();
    }
}
