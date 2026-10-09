package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.TopicStreamBase;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReadSession extends TopicStreamBase<FromServer, FromClient> {
    private static final Logger logger = LoggerFactory.getLogger(ReadSession.class);

    public interface PartitionControl {
        boolean isActive();
        void confirmRangeProcessed(OffsetsRange range);
    }

    public interface Handler {
        void onPartitionStarted(StartPartitionSessionEvent event);
        void onPartitionStopped(StopPartitionSessionEvent event);
        void onPartitionClosed(PartitionSessionClosedEvent event);

        void onData(PartitionControl control, DataReceivedEvent event);

        void onCommitAck(CommitOffsetAcknowledgementEvent event);
    }

    private final String debugId;
    private final ReadConfig config;
    private final MessageDecoder decoder;
    private final BufferManager bufferManager;
    private final Handler handler;

    private final Map<Long, ReadPartition> partitions = new ConcurrentHashMap<>();
    private volatile boolean isClosed = false;

    public ReadSession(String id, GrpcReadWriteStream<FromServer, FromClient> stream, FromClient initReq,
            Handler handler, ReadConfig config) {
        super(logger, id, stream, initReq);
        this.debugId = id;
        this.config = config;
        this.decoder = new MessageDecoder(config);
        this.bufferManager = new BufferManager(id, config.getMaxMemoryUsageBytes(), new ReadRequest());
        this.handler = handler;
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

    BufferManager getBufferManager() {
        return bufferManager;
    }

    ReadConfig getConfig() {
        return config;
    }

    MessageDecoder getDecoder() {
        return decoder;
    }

    Handler getHandler() {
        return handler;
    }

    public void closeAll() {
        isClosed = true;
        decoder.stop();
        partitions.values().forEach(ReadPartition::close);
        partitions.clear();
        config.getMetrics().unregister();
    }

    public boolean commitOffsets(PartitionSession session, List<OffsetsRange> rangesToCommit) {
        ReadPartition partition = partitions.get(session.getId());
        if (isClosed || partition == null || !partition.isActive()) {
            logger.info("[{}] Need to send CommitRequest for {} with offset ranges {}, "
                    + "but reading partition session is already closed", debugId, session,
                    rangesToCommit.stream().map(Object::toString).collect(Collectors.joining(", ")));
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

    public void sendStartPartition(PartitionSession ps, Long readFrom, Long commitTo) {
        if (isClosed) {
            return;
        }

        YdbTopic.StreamReadMessage.StartPartitionSessionResponse.Builder resp = YdbTopic.StreamReadMessage
                .StartPartitionSessionResponse.newBuilder()
                .setPartitionSessionId(ps.getId());
        if (readFrom != null) {
            resp.setReadOffset(readFrom);
        }
        if (commitTo != null) {
            resp.setCommitOffset(commitTo);
        }

        send(YdbTopic.StreamReadMessage.FromClient.newBuilder().setStartPartitionSessionResponse(resp.build()).build());
    }

    public void sendStopPartition(PartitionSession ps) {
        if (isClosed) {
            return;
        }

        YdbTopic.StreamReadMessage.StopPartitionSessionResponse resp = YdbTopic.StreamReadMessage
                .StopPartitionSessionResponse.newBuilder()
                .setPartitionSessionId(ps.getId())
                .build();

        send(YdbTopic.StreamReadMessage.FromClient.newBuilder().setStopPartitionSessionResponse(resp).build());

        ReadPartition partition = partitions.remove(ps.getId());
        if (partition != null) {
            partition.close();
        }
    }

    public void onInit(YdbTopic.StreamReadMessage.InitResponse response) {
        bufferManager.init(response.getSessionId());
        config.getMetrics().register(partitions::size, bufferManager::getCreditBalanceBytes,
                () -> partitions.values().stream().filter(ReadPartition::isActive)
                        .mapToLong(ReadPartition::getCommitOffsetLag).max().orElse(0));
    }

    public void onStartPartition(YdbTopic.StreamReadMessage.StartPartitionSessionRequest req) {
        long psid = req.getPartitionSession().getPartitionSessionId();
        long pid = req.getPartitionSession().getPartitionId();
        long committed = req.getCommittedOffset();

        PartitionSession ps = new PartitionSession(psid, pid, req.getPartitionSession().getPath());
        OffsetsRange offsets = new OffsetsRangeImpl(
                req.getPartitionOffsets().getStart(),
                req.getPartitionOffsets().getEnd()
        );

        String tid = debugId + '/' + psid + "-p" + pid;
        ReadPartition partition = new ReadPartition(tid, this, ps, committed);
        if (partitions.putIfAbsent(psid, partition) != null) {
            logger.error("[{}] Received second StartPartitionSessionRequest for the already active {}", debugId, ps);
            Issue issue = Issue.of("Restarting read session due to receiving second StartPartitionSessionRequest with "
                    + ps, Issue.Severity.FATAL);
            fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue));
            return;
        }

        partition.start(committed, offsets);
    }

    public void onClosePartition(long partitionSessionId) {
        ReadPartition partition = partitions.remove(partitionSessionId);
        if (partition == null) {
            logger.warn("[{}] Received force StopPartitionSessionRequest for partition session {}, " +
                    "but have no such partition session running", debugId, partitionSessionId);
            return;
        }

        logger.info("[{}] Received force StopPartitionSessionRequest for {} ", debugId, partition.getPartition());
        partition.close();
    }

    public void onStopPartition(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request) {
        long committedOffset = request.getCommittedOffset();
        long psid = request.getPartitionSessionId();
        ReadPartition partition = partitions.get(psid);
        if (partition != null) {
            partition.stop(committedOffset);
            return;
        }

        logger.error("[{}] Received graceful StopPartitionSessionRequest for partition session {}, " +
                "but have no such partition session active", debugId, psid);
        Issue issue = Issue.of("Restarting read session due to receiving StopPartitionSessionRequest with "
                + "PartitionSessionId " + psid + " that SDK knows nothing about", Issue.Severity.FATAL);
        fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue));
    }

    public void onRead(YdbTopic.StreamReadMessage.ReadResponse response) {
        logger.debug("[{}] Received ReadResponse of {} bytes", debugId, response.getBytesSize());
        config.getMetrics().reportReceivedBytes(response.getBytesSize());
        bufferManager.allocate(response.getBytesSize(), response.getPartitionDataList());

        for (YdbTopic.StreamReadMessage.ReadResponse.PartitionData data: response.getPartitionDataList()) {
            long psid = data.getPartitionSessionId();
            ReadPartition partition = partitions.get(psid);
            if (partition == null || !partition.addBatches(data.getBatchesList())) {
                logger.warn("[{}] Received PartitionData for unknown(most likely already closed) PartitionSessionId={}",
                        debugId, psid);
                bufferManager.releasePartition(psid);
            }
        }

        decoder.decodeNext();
    }

    public void onCommitOffset(YdbTopic.StreamReadMessage.CommitOffsetResponse response) {
        logger.trace("[{}] Received CommitOffsetResponse", debugId);

        for (CommitOffsetResponse.PartitionCommittedOffset offset: response.getPartitionsCommittedOffsetsList()) {
            ReadPartition partition = partitions.get(offset.getPartitionSessionId());
            if (partition == null) {
                logger.info("[{}] Received CommitOffsetResponse for unknown (most likely already closed) " +
                                "partition session with id={}", debugId, offset.getPartitionSessionId());
                continue;
            }

            partition.confirmCommittedOffset(offset.getCommittedOffset());
        }
    }

    public void onPartitionSessionStatus(YdbTopic.StreamReadMessage.PartitionSessionStatusResponse resp) {
        ReadPartition partition = partitions.get(resp.getPartitionSessionId());
        logger.info("[{}] Received PartitionSessionStatusResponse: partition session {} (partition {})." +
                        " Partition offsets: [{}, {}). Committed offset: {}", debugId,
                resp.getPartitionSessionId(),
                partition == null ? "unknown" : partition.getPartition().getPartitionId(),
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
}
