package tech.ydb.topic.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import tech.ydb.topic.settings.CreateTopicSettings;
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

        java.time.Duration retentionPeriod = settings.getRetentionPeriod();
        if (!retentionPeriod.equals(java.time.Duration.ZERO)) {
            requestBuilder.setRetentionPeriod(ProtoUtils.toProto(retentionPeriod));
        }

        List<Codec> supportedCodecs = settings.getSupportedCodecs();
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

            List<Codec> consumerCodecs = settings.getSupportedCodecs();
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

    @Override
    public void close() {
        topicRpc.close();
    }
}
