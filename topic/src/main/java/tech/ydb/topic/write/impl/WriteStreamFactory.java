package tech.ydb.topic.write.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicResult;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStreamFactory {
    private static final Logger logger = LoggerFactory.getLogger(WriteStreamFactory.class);

    private final String topicPath;
    private final StreamWriteMessage.InitRequest initRequest;
    protected final TopicRpc rpc;

    private WriteStreamFactory(TopicRpc rpc, WriterSettings settings) {
        this.rpc = rpc;
        this.topicPath = settings.getTopicPath();

        String producerId = settings.getProducerId();
        String messageGroupId = settings.getMessageGroupId();
        Long partitionId = settings.getPartitionId();

        StreamWriteMessage.InitRequest.Builder req = StreamWriteMessage.InitRequest.newBuilder()
                .setPath(topicPath);

        if (producerId != null) {
            req.setProducerId(producerId);
        }
        if (messageGroupId != null) {
            if (partitionId != null) {
                throw new IllegalArgumentException("Both MessageGroupId and PartitionId are set in WriterSettings");
            }
            req.setMessageGroupId(messageGroupId);
        } else if (partitionId != null) {
            req.setPartitionId(partitionId);
        }

        this.initRequest = req.build();
    }

    public String getTopicPath() {
        return topicPath;
    }

    public WriteSession.Stream createNewStream(String id) {
        return new WriteStream(id, rpc.writeSession(id));
    }

    public YdbTopic.StreamWriteMessage.FromClient initRequest() {
        return YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setInitRequest(initRequest)
                .build();
    }

    protected Result<Integer> lookupNodeId(String id, long partitionId) {
        logger.info("[{}] describe topic {} to look up node for partition {}", id, topicPath, partitionId);
        Result<DescribeTopicResult> describeTopic = rpc.describeTopic(
                DescribeTopicRequest.newBuilder().setIncludeLocation(true).setPath(topicPath).build(),
                GrpcRequestSettings.newBuilder().withDeadline(Duration.ofMinutes(1)).build()
        ).join();

        if (!describeTopic.isSuccess()) {
            logger.warn("[{}] describe topic {} failed with status {}", id, topicPath, describeTopic.getStatus());
            return Result.fail(describeTopic.getStatus());
        }

        // lookup for nodeID
        for (DescribeTopicResult.PartitionInfo partition : describeTopic.getValue().getPartitionsList()) {
            if (partition.getPartitionId() == partitionId) {
                return Result.success(partition.getPartitionLocation().getNodeId());
            }
        }

        logger.warn("[{}] topic {} doesn't have partition {}, direct writing failed", id, topicPath, partitionId);
        Issue issue = Issue.of("Cannot find partition " + partitionId, Issue.Severity.ERROR);
        return Result.fail(Status.of(StatusCode.BAD_REQUEST, issue));
    }

    protected Result<Long> lookupPartitionId(String id, String producerId) {
        CompletableFuture<Result<Long>> partitionId = new CompletableFuture<>();

        // create one-shot stream to detect partitionID for this producer
        logger.info("[{}] create probe stream for topic {} with producer {}", id, topicPath, producerId);
        GrpcReadWriteStream<FromServer, FromClient> stream = rpc.writeSession(id);

        CompletableFuture<Status> streamFuture = stream.start(resp -> {
            if (resp.getStatus() != StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                Status status = Status.of(StatusCode.fromProto(resp.getStatus()), Issue.fromPb(resp.getIssuesList()));
                logger.warn("[{}] probe stream to topic {} with producer {} got error {}", id, topicPath,
                        producerId, status);
                partitionId.complete(Result.fail(status));
                return;
            }

            if (resp.hasInitResponse()) {
                long pid = resp.getInitResponse().getPartitionId();
                logger.warn("[{}] probe stream to topic {} with producer {} has partition {}", id, topicPath,
                        producerId, pid);
                partitionId.complete(Result.success(pid));
                return;
            }

            logger.warn("[{}] probe stream to topic {} with producer {} got unexpexted message {}", id, topicPath,
                    producerId, resp.getClass().getName());

            Issue issue = Issue.of("Unexpected message from stream with producer " + producerId, Issue.Severity.ERROR);
            partitionId.complete(Result.fail(Status.of(StatusCode.BAD_REQUEST, issue)));
        });

        if (streamFuture.isDone()) {
            logger.warn("[{}] probe stream to topic {} with producer {} failed with status {}", id, topicPath,
                    producerId, streamFuture.join());
            return Result.fail(streamFuture.join());
        }

        try {
            streamFuture.whenComplete((st, th) -> {
                Status status = st != null ? st : Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                if (!partitionId.isDone()) {
                    logger.warn("[{}] probe stream to topic {} with producer {} failed with status {}", id, topicPath,
                        producerId, streamFuture.join());
                    partitionId.complete(Result.fail(status));
                }
            });
            stream.sendNext(initRequest());
            return partitionId.join();
        } finally {
            if (!streamFuture.isDone()) {
                stream.close();
            }
        }
    }

    public static WriteStreamFactory of(TopicRpc rpc, WriterSettings settings) {
        if (!settings.isDirectWrite()) {
            return new WriteStreamFactory(rpc, settings);
        }

        if (settings.getPartitionId() != null) {
            return new DirectWriteByPartitionId(rpc, settings, settings.getPartitionId());
        }

        if (settings.getProducerId() != null) {
            return new DirectWriteByProducerId(rpc, settings, settings.getProducerId());
        }

        throw new IllegalArgumentException("Direct writing requires PartitionId or ProducerId in WriterSettings");
    }

    private static class DirectWriteByPartitionId extends WriteStreamFactory {
        private final long partitionId;

        private DirectWriteByPartitionId(TopicRpc rpc, WriterSettings settings, long partitionId) {
            super(rpc, settings);
            this.partitionId = partitionId;
        }

        @Override
        public WriteSession.Stream createNewStream(String id) {
            Result<Integer> nodeId = lookupNodeId(id, partitionId);
            if (!nodeId.isSuccess()) {
                return new WriteStream.Fail(id, nodeId.getStatus());
            }

            return new WriteStream(id, rpc.writeSession(id, nodeId.getValue()));
        }
    }

    private static class DirectWriteByProducerId extends WriteStreamFactory {
        private final String producerId;

        private DirectWriteByProducerId(TopicRpc rpc, WriterSettings settings, String producerId) {
            super(rpc, settings);
            this.producerId = producerId;
        }

        @Override
        public WriteSession.Stream createNewStream(String id) {
            Result<Long> partitionId = lookupPartitionId(id, producerId);
            if (!partitionId.isSuccess()) {
                return new WriteStream.Fail(id, partitionId.getStatus());
            }

            Result<Integer> nodeId = lookupNodeId(id, partitionId.getValue());
            if (!nodeId.isSuccess()) {
                return new WriteStream.Fail(id, nodeId.getStatus());
            }

            return new WriteStream(id, rpc.writeSession(id, nodeId.getValue()));
        }
    }
}
