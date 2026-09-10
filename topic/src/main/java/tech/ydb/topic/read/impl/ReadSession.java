package tech.ydb.topic.read.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.CommitOffsetRequest;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.CommitOffsetResponse;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.StartPartitionSessionResponse;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.TopicStreamBase;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.events.StartPartitionSessionEventImpl;
import tech.ydb.topic.read.impl.events.StopPartitionSessionEventImpl;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReadSession extends TopicStreamBase<FromServer, FromClient> implements ReaderImpl.Releaser {
    private static final Logger logger = LoggerFactory.getLogger(ReadSession.class);

    private final String debugId;
    private final ReadConfig config;
    private final MessageDecoder decoder;
    private final BufferManager bufferManager;
    private final BiConsumer<ReaderImpl.Releaser, DataReceivedEvent> eventConsumer;

    private final Map<Long, PartitionSession> partitions = new ConcurrentHashMap<>();
    private final Map<Long, ReadPartitionSession> readQueues = new ConcurrentHashMap<>();
    private volatile boolean isClosed = false;

    public ReadSession(String id, GrpcReadWriteStream<FromServer, FromClient> stream, FromClient initReq,
            BiConsumer<ReaderImpl.Releaser, DataReceivedEvent> eventConsumer, ReadConfig config) {
        super(logger, id, stream, initReq);
        this.debugId = id;
        this.config = config;
        this.decoder = new MessageDecoder(config);
        this.bufferManager = new BufferManager(id, config.getMaxMemoryUsageBytes(), new ReadRequest());
        this.eventConsumer = eventConsumer;
    }

    @Override
    protected FromClient updateTokenMessage(String token) {
        YdbTopic.UpdateTokenRequest req = YdbTopic.UpdateTokenRequest.newBuilder().setToken(token).build();
        return FromClient.newBuilder().setUpdateTokenRequest(req).build();
    }

    @Override
    protected Status parseMessageStatus(FromServer message) {
        return Status.of(StatusCode.fromProto(message.getStatus()), Issue.fromPb(message.getIssuesList()));
    }

    public Set<PartitionSession> closeAll() {
        decoder.stop();

        Set<PartitionSession> closed = new HashSet<>(partitions.values());
        partitions.clear();

        readQueues.values().forEach(ReadPartitionSession::stop);
        readQueues.clear();

        return closed;
    }

    @Override
    public void releaseRange(PartitionSession partition, OffsetsRange range) {
        bufferManager.releaseRange(partition.getId(), range);
        ReadPartitionSession queue = readQueues.get(partition.getId());
        if (queue != null) {
            queue.releaseRange(range);
        }
    }

    public boolean commitOffsets(PartitionSession session, List<OffsetsRange> rangesToCommit) {
        if (isClosed) {
            logger.atInfo()
                    .setMessage("[{}] Need to send CommitRequest for {} with offset ranges {}, "
                            + "but reading session is already closed")
                    .addArgument(debugId)
                    .addArgument(session)
                    .addArgument(() -> rangesToCommit.stream().map(Object::toString).collect(Collectors.joining(", ")))
                    .log();
            return false;
        }

        CommitOffsetRequest req = CommitOffsetRequest.newBuilder()
                .addCommitOffsets(CommitOffsetRequest.PartitionCommitOffset.newBuilder()
                        .setPartitionSessionId(session.getId())
                        .addAllOffsets(rangesToCommit.stream()
                                .map(ReaderImpl::buildOffsetRange)
                                .collect(Collectors.toList()))
                        .build())
                .build();

        send(FromClient.newBuilder().setCommitOffsetRequest(req).build());
        return true;
    }

    public void onInit(YdbTopic.StreamReadMessage.InitResponse response) {
        bufferManager.init(response.getSessionId());
    }

    public StartPartitionSessionEvent onStartPartition(YdbTopic.StreamReadMessage.StartPartitionSessionRequest req) {
        long psid = req.getPartitionSession().getPartitionSessionId();
        long pid = req.getPartitionSession().getPartitionId();
        long committed = req.getCommittedOffset();

        PartitionSession partition = new PartitionSession(psid, pid, req.getPartitionSession().getPath());
        OffsetsRange offsets = new OffsetsRangeImpl(
                req.getPartitionOffsets().getStart(),
                req.getPartitionOffsets().getEnd()
        );

        String traceID = debugId + '/' + psid + "-p" + pid;
        logger.info("[{}] Received StartPartitionSessionRequest for {} and consumer \"{}\" with committedOffset {}"
                + " and partitionOffsets {}", traceID, partition, config.getConsumerName(), committed, offsets);

        partitions.put(psid, partition);
        return new StartPartitionRequest(traceID, partition, committed, offsets);
    }

    public PartitionSession onClosePartition(long partitionSessionId) {
        PartitionSession partition = partitions.remove(partitionSessionId);
        if (partition == null) {
            logger.warn("[{}] Received force StopPartitionSessionRequest for partition session {}, " +
                    "but have no such partition session running", debugId, partitionSessionId);
            return null;
        }

        ReadPartitionSession queue = readQueues.remove(partitionSessionId);
        if (queue != null) {
            logger.info("[{}] Received force StopPartitionSessionRequest for {} ", debugId, queue.getPartition());
            queue.stop();
            bufferManager.releasePartition(partitionSessionId);
        }

        return partition;
    }

    public StopPartitionSessionEvent onStopPartition(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        long committedOffset = request.getCommittedOffset();
        long psid = request.getPartitionSessionId();
        PartitionSession partition = partitions.get(psid);
        if (partition == null) {
            logger.error("[{}] Received graceful StopPartitionSessionRequest for partition session {}, " +
                    "but have no such partition session active", debugId, psid);
            return null;
        }

        logger.info("[{}] Received graceful StopPartitionSessionRequest for {}", debugId, partition);
        return new StopPartitionRequest(partition, committedOffset);
    }

    public void onRead(YdbTopic.StreamReadMessage.ReadResponse response) {
        logger.debug("[{}] Received ReadResponse of {} bytes", debugId, response.getBytesSize());
        bufferManager.allocate(response.getBytesSize(), response.getPartitionDataList());

        for (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data: response.getPartitionDataList()) {
            long psid = data.getPartitionSessionId();
            ReadPartitionSession queue = readQueues.get(psid);
            if (queue == null || !queue.addBatches(data.getBatchesList())) {
                logger.warn("[{}] Received PartitionData for unknown(most likely already closed) PartitionSessionId={}",
                        debugId, psid);
                bufferManager.releasePartition(psid);
            }
        }

        decoder.decodeNext();
    }

    public void onCommitOffset(YdbTopic.StreamReadMessage.CommitOffsetResponse response,
            BiConsumer<Long, PartitionSession> callback) {
        logger.trace("[{}] Received CommitOffsetResponse", debugId);

        for (CommitOffsetResponse.PartitionCommittedOffset offset: response.getPartitionsCommittedOffsetsList()) {
            ReadPartitionSession queue = readQueues.get(offset.getPartitionSessionId());
            if (queue == null) {
                logger.info("[{}] Received CommitOffsetResponse for unknown (most likely already closed) " +
                                "partition session with id={}", debugId, offset.getPartitionSessionId());
                return;
            }

            // Handling CompletableFuture completions for single commits
            queue.confirmCommittedOffset(offset.getCommittedOffset());
            // Handling onCommitResponse callback
            callback.accept(offset.getCommittedOffset(), queue.getPartition());
        }
    }

    public void onPartitionSessionStatus(YdbTopic.StreamReadMessage.PartitionSessionStatusResponse resp) {
        PartitionSession partition = partitions.get(resp.getPartitionSessionId());
        logger.info("[{}] Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                        " Partition offsets: [{}, {}). Committed offset: {}", debugId,
                resp.getPartitionSessionId(),
                partition == null ? "unknown" : partition.getPartitionId(),
                resp.getPartitionOffsets().getStart(),
                resp.getPartitionOffsets().getEnd(),
                resp.getCommittedOffset());
    }

    private class ReadRequest implements Consumer<Long> {
        @Override
        public void accept(Long sizeToRequest) {
            logger.debug("[{}] Sending DataRequest with {} bytes", debugId, sizeToRequest);
            send(YdbTopic.StreamReadMessage.FromClient.newBuilder()
                    .setReadRequest(YdbTopic.StreamReadMessage.ReadRequest.newBuilder()
                            .setBytesSize(sizeToRequest)
                            .build())
                    .build());
        }
    }

    private class StartPartitionRequest extends StartPartitionSessionEventImpl {
        private final String traceID;

        StartPartitionRequest(String traceID, PartitionSession ps, long committed, OffsetsRange offsets) {
            super(ps, committed, offsets);
            this.traceID = traceID;
        }

        @Override
        public void confirm(StartPartitionSessionSettings options) {
            if (isClosed) {
                logger.info("[{}] Need to send StartPartitionSessionResponse, but reading session is "
                        + "already closed", traceID);
                return;
            }

            long psid = getPartitionSession().getId();
            long readFrom = getCommittedOffset();
            long commitTo = getCommittedOffset();

            PartitionSession partition = partitions.get(psid);
            if (partition == null) {
                logger.info("[{}] Need to send StartPartitionSessionResponse, but have no such active partition "
                        + "session anymore", traceID);
                return;
            }

            StartPartitionSessionResponse.Builder resp = StartPartitionSessionResponse.newBuilder()
                    .setPartitionSessionId(psid);

            if (options != null) {
                if (options.getReadOffset() != null) {
                    readFrom = options.getReadOffset();
                    resp.setReadOffset(readFrom);
                }
                if (options.getCommitOffset() != null) {
                    commitTo = options.getCommitOffset();
                    resp.setCommitOffset(commitTo);
                }
            }

            MessageCommitterImpl committer = new MessageCommitterImpl(traceID, ReadSession.this, partition, commitTo);
            ReadPartitionSession queue = new ReadPartitionSession(traceID, config, partition, committer, decoder,
                    event -> eventConsumer.accept(ReadSession.this, event), commitTo);
            if (readQueues.putIfAbsent(psid, queue) != null) {
                logger.warn("[{}] partition {} is already started", traceID, partition);
                return;
            }

            logger.info("[{}] Sending StartPartitionSessionResponse for {} and consumer \"{}\" with readOffset "
                    + "{} and commitOffset {}", traceID, partition, config.getConsumerName(), readFrom, commitTo);
            send(FromClient.newBuilder().setStartPartitionSessionResponse(resp.build()).build());
        }
    };

    private class StopPartitionRequest extends StopPartitionSessionEventImpl {
        StopPartitionRequest(PartitionSession partition, long committedOffset) {
            super(partition, committedOffset);
        }

        @Override
        public void confirm() {
            PartitionSession partition = getPartitionSession();
            long psid = getPartitionSessionId();
            if (isClosed) {
                logger.info("[{}] Need to send StopPartitionSessionResponse for {}, " +
                        "but reading session is already closed", debugId, partition);
                return;
            }

            if (partitions.remove(psid, partition)) {
                logger.info("[{}] Sending StopPartitionSessionResponse for {}", debugId, partition);
                send(YdbTopic.StreamReadMessage.FromClient.newBuilder().setStopPartitionSessionResponse(
                                YdbTopic.StreamReadMessage.StopPartitionSessionResponse.newBuilder()
                                        .setPartitionSessionId(psid)
                                        .build())
                        .build());

                ReadPartitionSession session = readQueues.remove(psid);
                if (session != null) {
                    session.stop();
                }
            }

            bufferManager.releasePartition(psid);
        }
    }
}
