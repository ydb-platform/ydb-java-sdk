package tech.ydb.topic.write.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.ProtoUtils;

/**
 * Utility class that splits messages into several requests so that every request would be less than grpc size limit
 * @author Nikolay Perfilov
 */
public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    private static final int MAX_GRPC_MESSAGE_SIZE = 64_000_000;

    private final WriterSettings settings;
    private final int requestOverheadBytes;
    private final int messageOverheadBytes;

    private WriteSession session;
    private long seqNo = 0;
    private long totalMessageDataProtoSize;
    private YdbTopic.StreamWriteMessage.WriteRequest.Builder writeRequestBuilder;
    private int messageCount;

    public MessageSender(WriteSession session, WriterSettings settings) {
        this.session = session;
        this.settings = settings;
        List<Integer> overheads = calculateOverheads();
        this.messageOverheadBytes = overheads.get(0);
        this.requestOverheadBytes = overheads.get(1);
    }

    private List<Integer> calculateOverheads() {
        reset();
        YdbTopic.StreamWriteMessage.FromClient requestWithoutMessage =
                YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                        .setWriteRequest(writeRequestBuilder.build())
                        .build();
        int requestOverhead = requestWithoutMessage.getSerializedSize();

        YdbTopic.StreamWriteMessage.WriteRequest.MessageData messageData =
                YdbTopic.StreamWriteMessage.WriteRequest.MessageData.newBuilder()
                        .setSeqNo(Long.MAX_VALUE)
                        .setData(ByteString.EMPTY)
                        .build();
        YdbTopic.StreamWriteMessage.FromClient requestWithMessage = YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setWriteRequest(writeRequestBuilder.addMessages(messageData))
                .build();
        int messageDataSize = messageData.getSerializedSize();
        int sizeWithMessage = requestWithMessage.getSerializedSize();
        int messageOverhead = sizeWithMessage - requestOverhead - messageDataSize;
        logger.debug("Calculated per-message bytes overhead: {}, request overhead: {}", messageOverhead,
                requestOverhead);
        return Arrays.asList(messageOverhead, requestOverhead);
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public void setSession(WriteSession session) {
        this.session = session;
    }

    private void reset() {
        writeRequestBuilder = YdbTopic.StreamWriteMessage.WriteRequest.newBuilder()
                .setCodec(ProtoUtils.toProto(settings.getCodec()));
        messageCount = 0;
        totalMessageDataProtoSize = 0;
    }

    public long getCurrentRequestSize() {
        return requestOverheadBytes + totalMessageDataProtoSize + (long) messageCount * messageOverheadBytes;
    }

    public void addMessage(YdbTopic.StreamWriteMessage.WriteRequest.MessageData message) {
        messageCount++;
        totalMessageDataProtoSize += message.getSerializedSize();
        writeRequestBuilder.addMessages(message);
    }

    public void sendWriteRequest() {
        YdbTopic.StreamWriteMessage.FromClient fromClient = YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setWriteRequest(writeRequestBuilder)
                .build();
        if (logger.isDebugEnabled()) {
            logger.debug("Predicted request size: {} = {}(request overhead) + {}(all MessageData protos) " +
                            "+ {}(message overheads)\nActual request size: {} bytes", getCurrentRequestSize(),
                    requestOverheadBytes, totalMessageDataProtoSize, messageOverheadBytes * messageCount,
                    fromClient.getSerializedSize());
        }
        if (fromClient.getSerializedSize() > MAX_GRPC_MESSAGE_SIZE) {
            List<YdbTopic.StreamWriteMessage.WriteRequest.MessageData> messages = writeRequestBuilder.getMessagesList();
            if (messages.size() > 1) {
                int firstHalfMessagesCount = messages.size() / 2;
                logger.debug("Failed to predict request total size. Total size is {} which exceeds the limit of {}. " +
                                "Splitting {} messages into two requests of {} and {} messages",
                        fromClient.getSerializedSize(), MAX_GRPC_MESSAGE_SIZE, messages.size(), firstHalfMessagesCount,
                        messages.size() - firstHalfMessagesCount);

                for (List<YdbTopic.StreamWriteMessage.WriteRequest.MessageData> sublist : Arrays.asList(
                        messages.subList(0, firstHalfMessagesCount),
                        messages.subList(firstHalfMessagesCount, messages.size())
                )) {
                    writeRequestBuilder = YdbTopic.StreamWriteMessage.WriteRequest.newBuilder()
                                .setCodec(ProtoUtils.toProto(settings.getCodec()));
                    writeRequestBuilder.addAllMessages(sublist);
                    YdbTopic.StreamWriteMessage.FromClient subRequest = YdbTopic.StreamWriteMessage.FromClient
                            .newBuilder()
                            .setWriteRequest(writeRequestBuilder)
                            .build();
                    logger.debug("Total sub-request size: {} bytes", subRequest.getSerializedSize());
                    session.send(subRequest);
                }
                return;
            }
        }
        session.send(fromClient);
    }

    public void tryAddMessageToRequest(EnqueuedMessage message) {
        long messageSeqNo = message.getSeqNo() == null
                ? (message.getMessage().getSeqNo() == null ? ++seqNo : message.getMessage().getSeqNo())
                : message.getSeqNo();
        if (message.getSeqNo() == null) {
            message.setSeqNo(messageSeqNo);
        }

        YdbTopic.StreamWriteMessage.WriteRequest.MessageData messageData =
                YdbTopic.StreamWriteMessage.WriteRequest.MessageData.newBuilder()
                        .setSeqNo(messageSeqNo)
                        .setData(ByteString.copyFrom(message.getMessage().getData()))
                        .build();
        long sizeWithCurrentMessage = getCurrentRequestSize() + messageData.getSerializedSize() + messageOverheadBytes;
        if (sizeWithCurrentMessage <= MAX_GRPC_MESSAGE_SIZE) {
            addMessage(messageData);
        } else {
            if (messageCount > 0) {
                logger.debug("Adding next message to the same request would lead to grpc request size overflow. " +
                        "Sending previous {} messages...", messageCount);
                sendWriteRequest();
                reset();
                addMessage(messageData);
            } else {
                logger.error("A single message is larger than grpc size limit. Sending it anyway...");
                addMessage(messageData);
                sendWriteRequest();
                reset();
            }
        }
    }

    public void sendMessages(Queue<EnqueuedMessage> messages) {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to send {} message(s)...", messages.size());
        }
        reset();
        messages.forEach(this::tryAddMessageToRequest);
        if (messageCount > 0) {
            sendWriteRequest();
        }
    }
}
