package tech.ydb.topic.write.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;

/**
 * Utility class that splits messages into several requests so that every request would be less than grpc size limit
 * @author Nikolay Perfilov
 */
public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    private static final int MAX_GRPC_MESSAGE_SIZE = 64_000_000;

    private static final int REQUEST_OVERHEAD;
    private static final int MESSAGE_OVERHEAD;

    static {
        YdbTopic.StreamWriteMessage.FromClient emptyReq =
                YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                    .setWriteRequest(YdbTopic.StreamWriteMessage.WriteRequest.newBuilder().setCodec(1))
                    .build();
        REQUEST_OVERHEAD = emptyReq.getSerializedSize();

        YdbTopic.StreamWriteMessage.WriteRequest.MessageData message =
                YdbTopic.StreamWriteMessage.WriteRequest.MessageData.newBuilder()
                        .setSeqNo(Long.MAX_VALUE)
                        .setData(ByteString.EMPTY)
                        .setCreatedAt(ProtobufUtils.instantToProto(Instant.now()))
                        .setUncompressedSize(1_000_000)
                        .build();
        YdbTopic.StreamWriteMessage.FromClient oneMessageReq = YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setWriteRequest(YdbTopic.StreamWriteMessage.WriteRequest.newBuilder().setCodec(2).addMessages(message))
                .build();
        int messageDataSize = message.getSerializedSize();
        MESSAGE_OVERHEAD = oneMessageReq.getSerializedSize() - REQUEST_OVERHEAD - messageDataSize;
        logger.debug("Calculated per-message bytes overhead: {}, request overhead: {}", MESSAGE_OVERHEAD,
                REQUEST_OVERHEAD);
    }

    private final int codecCode;
    private final List<YdbTopic.StreamWriteMessage.WriteRequest.MessageData> messages = new ArrayList<>();
    private final AtomicInteger messagesPbSize = new AtomicInteger(0);

    private volatile WriteSession session;
    private volatile YdbTransaction currentTransaction = null;

    public MessageSender(int codecCode) {
        this.codecCode = codecCode;
    }

    public void setSession(WriteSession session) {
        this.session = session;
    }

    public int getCurrentRequestSize() {
        return REQUEST_OVERHEAD + messagesPbSize.get() + MESSAGE_OVERHEAD * messages.size();
    }

    public void sendWriteRequest() {
        YdbTopic.StreamWriteMessage.WriteRequest.Builder req = YdbTopic.StreamWriteMessage.WriteRequest.newBuilder();
        if (currentTransaction != null) {
            req.setTx(YdbTopic.TransactionIdentity.newBuilder()
                    .setId(currentTransaction.getId())
                    .setSession(currentTransaction.getSessionId()));
        }

        req.setCodec(codecCode);
        req.addAllMessages(messages);

        YdbTopic.StreamWriteMessage.FromClient fromClient = YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setWriteRequest(req.build())
                .build();

        if (logger.isDebugEnabled()) {
            logger.debug("Predicted request size: {} = {}(request overhead) + {}(all MessageData protos) " +
                            "+ {}(message overheads) Actual request size: {} bytes", getCurrentRequestSize(),
                    REQUEST_OVERHEAD, messagesPbSize, MESSAGE_OVERHEAD * messages.size(),
                    fromClient.getSerializedSize());
        }

        session.send(fromClient);
        messages.clear();
        messagesPbSize.set(0);
    }

    public void sendMessage(SentMessage message) {
        YdbTransaction messageTx = message.getTx();
        if (messageTx != currentTransaction) {
            flush();
            currentTransaction = messageTx;
        }


        YdbTopic.StreamWriteMessage.WriteRequest.MessageData pb = message.getPb();
        long sizeWithCurrentMessage = getCurrentRequestSize() + pb.getSerializedSize() + MESSAGE_OVERHEAD;
        if (sizeWithCurrentMessage > MAX_GRPC_MESSAGE_SIZE) {
            flush();
        }

        messagesPbSize.addAndGet(pb.getSerializedSize());
        messages.add(pb);
    }

    public void flush() {
        if (!messages.isEmpty()) {
            sendWriteRequest();
        }
    }
}
