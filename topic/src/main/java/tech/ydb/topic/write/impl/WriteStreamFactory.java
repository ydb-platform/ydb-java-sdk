package tech.ydb.topic.write.impl;


import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStreamFactory {
    private final String topicPath;
    private final TopicRpc rpc;
    private final YdbTopic.StreamWriteMessage.InitRequest initRequest;

    public WriteStreamFactory(TopicRpc rpc, WriterSettings settings) {
        this.rpc = rpc;
        this.topicPath = settings.getTopicPath();

        YdbTopic.StreamWriteMessage.InitRequest.Builder req = YdbTopic.StreamWriteMessage.InitRequest
                .newBuilder()
                .setPath(topicPath);
        String producerId = settings.getProducerId();
        if (producerId != null) {
            req.setProducerId(producerId);
        }
        String messageGroupId = settings.getMessageGroupId();
        Long partitionId = settings.getPartitionId();
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
        return new WriteStream(id, rpc);
    }

    public YdbTopic.StreamWriteMessage.FromClient initRequest() {
        return YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setInitRequest(initRequest)
                .build();
    }
}
