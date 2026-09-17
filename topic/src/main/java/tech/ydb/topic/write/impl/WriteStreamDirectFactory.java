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
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class WriteStreamDirectFactory extends WriteStreamFactory {
    private static final Logger logger = LoggerFactory.getLogger(WriteStreamDirectFactory.class);

    public WriteStreamDirectFactory(TopicRpc rpc, WriterSettings settings) {
        super(rpc, settings);

        if (settings.getPartitionId() == null && settings.getProducerId() == null) {
            throw new IllegalArgumentException("Direct writing requires PartitionId or ProducerId in WriterSettings");
        }
    }

    @Override
    public CompletableFuture<WriteSession.Stream> createNewStream(String id) {
        CompletableFuture<Result<Long>> partitionIdLookup = partitionId == null
                ? lookupPartitionId(id)
                : CompletableFuture.completedFuture(Result.success(partitionId));

        return partitionIdLookup.thenCompose(lookupResult -> {
            if (!lookupResult.isSuccess()) {
                return CompletableFuture.completedFuture(new WriteStream.Fail(id, lookupResult.getStatus()));
            }

            long targetPartitionId = lookupResult.getValue();
            return lookupLocation(id, targetPartitionId)
                    .thenApply(location -> buildDirectStream(id, targetPartitionId, location));
        });
    }

    private WriteSession.Stream buildDirectStream(String id, long targetPartitionId,
            Result<YdbTopic.PartitionLocation> location) {
        if (!location.isSuccess()) {
            return new WriteStream.Fail(id, location.getStatus());
        }

        StreamWriteMessage.InitRequest.Builder req = StreamWriteMessage.InitRequest.newBuilder()
                .setPath(topicPath)
                .setPartitionWithGeneration(YdbTopic.PartitionWithGeneration.newBuilder()
                        .setPartitionId(targetPartitionId)
                        .setGeneration(location.getValue().getGeneration())
                        .build());

        if (producerId != null) {
            req.setProducerId(producerId);
        }

        FromClient init = FromClient.newBuilder().setInitRequest(req.build()).build();
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder()
                .withTraceId(id)
                .disableDeadline()
                .withDirectMode(true)
                .withPreferredNodeID(location.getValue().getNodeId())
                .build();

        return new WriteStream(id, rpc.writeSession(settings), init);
    }

    protected CompletableFuture<Result<YdbTopic.PartitionLocation>> lookupLocation(String id, long targetPartitionId) {
        logger.info("[{}] describe topic {} to look up node for partition {}", id, topicPath, targetPartitionId);
        return rpc.describeTopic(
                YdbTopic.DescribeTopicRequest.newBuilder().setIncludeLocation(true).setPath(topicPath).build(),
                GrpcRequestSettings.newBuilder().withDeadline(Duration.ofMinutes(1)).build()
        ).thenApply(describeTopic -> parseLocation(id, targetPartitionId, describeTopic));
    }

    private Result<YdbTopic.PartitionLocation> parseLocation(
            String id,
            long targetPartitionId,
            Result<YdbTopic.DescribeTopicResult> describeTopic
    ) {
        if (!describeTopic.isSuccess()) {
            logger.warn("[{}] describe topic {} failed with status {}", id, topicPath, describeTopic.getStatus());
            return Result.fail(describeTopic.getStatus());
        }

        // lookup for partition location
        for (YdbTopic.DescribeTopicResult.PartitionInfo partition : describeTopic.getValue().getPartitionsList()) {
            if (partition.getPartitionId() == targetPartitionId) {
                if (!partition.hasPartitionLocation()) {
                    logger.warn("[{}] partition {} has no valid location info", id, targetPartitionId);
                    Issue issue = Issue.of("Partition " + targetPartitionId + " has no location", Issue.Severity.ERROR);
                    return Result.fail(Status.of(StatusCode.BAD_REQUEST, issue));
                }

                return Result.success(partition.getPartitionLocation());
            }
        }

        logger.warn("[{}] topic {} doesn't have partition {}, direct writing failed", id, topicPath, targetPartitionId);
        Issue issue = Issue.of("Cannot find partition " + targetPartitionId, Issue.Severity.ERROR);
        return Result.fail(Status.of(StatusCode.BAD_REQUEST, issue));
    }

    private CompletableFuture<Result<Long>> lookupPartitionId(String id) {
        CompletableFuture<Result<Long>> pidFuture = new CompletableFuture<>();

        // create one-shot stream to detect partitionID for this producer
        logger.info("[{}] create probe stream for topic {} with producer {}", id, topicPath, producerId);
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder()
                .withTraceId(id + "-probe")
                .withDeadline(Duration.ofMinutes(1))
                .build();
        GrpcReadWriteStream<FromServer, FromClient> stream = rpc.writeSession(settings);

        CompletableFuture<Status> streamFuture = stream.start(resp -> {
            if (resp.getStatus() != StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                Status status = Status.of(StatusCode.fromProto(resp.getStatus()), Issue.fromPb(resp.getIssuesList()));
                logger.warn("[{}] probe stream to topic {} with producer {} got error {}", id, topicPath,
                        producerId, status);
                pidFuture.complete(Result.fail(status));
                return;
            }

            if (resp.hasInitResponse()) {
                long pid = resp.getInitResponse().getPartitionId();
                logger.info("[{}] probe stream to topic {} with producer {} has partition {}", id, topicPath,
                        producerId, pid);
                pidFuture.complete(Result.success(pid));
                return;
            }

            logger.warn("[{}] probe stream to topic {} with producer {} got unexpected message {}", id, topicPath,
                    producerId, resp.getClass().getName());

            Issue issue = Issue.of("Unexpected message from stream with producer " + producerId, Issue.Severity.ERROR);
            pidFuture.complete(Result.fail(Status.of(StatusCode.BAD_REQUEST, issue)));
        });

        if (streamFuture.isDone()) {
            logger.warn("[{}] probe stream to topic {} with producer {} failed with status {}", id, topicPath,
                    producerId, streamFuture.join());
            return CompletableFuture.completedFuture(Result.fail(streamFuture.join()));
        }

        streamFuture.whenComplete((st, th) -> {
            Status status = st != null ? st : Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
            if (pidFuture.complete(Result.fail(status))) {
                logger.warn("[{}] probe stream to topic {} with producer {} failed with status {}", id, topicPath,
                        producerId, status);
            }
        });

        // the probe stream is closed as soon as the partition is known, whichever thread completes the future
        CompletableFuture<Result<Long>> result = pidFuture.whenComplete((__, ___) -> {
            if (!streamFuture.isDone()) {
                stream.close();
            }
        });

        try {
            YdbTopic.StreamWriteMessage.FromClient init = YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                    .setInitRequest(buildInitRequest())
                    .build();
            stream.sendNext(init);
        } catch (Throwable throwable) {
            logger.warn(
                    "[{}] cannot send init request to probe stream of topic {} with producer {}",
                    id,
                    topicPath,
                    producerId,
                    throwable
            );
            pidFuture.complete(Result.fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, throwable)));
        }

        return result;
    }
}
