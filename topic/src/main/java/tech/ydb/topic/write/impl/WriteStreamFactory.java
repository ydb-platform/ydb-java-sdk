package tech.ydb.topic.write.impl;



import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStreamFactory {
    protected final String topicPath;
    protected final TopicRpc rpc;
    protected final String producerId;
    protected final String messageGroupId;
    protected final Long partitionId;

    public WriteStreamFactory(TopicRpc rpc, WriterSettings settings) {
        this.rpc = rpc;
        this.topicPath = settings.getTopicPath();

        this.producerId = settings.getProducerId();
        this.messageGroupId = settings.getMessageGroupId();
        this.partitionId = settings.getPartitionId();

        if (messageGroupId != null && partitionId != null) {
            throw new IllegalArgumentException("Both MessageGroupId and PartitionId are set in WriterSettings");
        }
    }

    public String getTopicPath() {
        return topicPath;
    }

    public StreamWriteMessage.InitRequest buildInitRequest() {
        StreamWriteMessage.InitRequest.Builder req = StreamWriteMessage.InitRequest.newBuilder()
                .setPath(topicPath);

        if (producerId != null) {
            req.setProducerId(producerId);
        }
        if (messageGroupId != null) {
            req.setMessageGroupId(messageGroupId);
        }
        if (partitionId != null) {
            req.setPartitionId(partitionId);
        }

        return req.build();
    }

    public WriteSession.Stream createNewStream(String id) {
        FromClient init = FromClient.newBuilder().setInitRequest(buildInitRequest()).build();
        return new WriteStream(id, rpc.writeSession(id), init);
    }
}
