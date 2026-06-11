package tech.ydb.topic.write.impl;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 * @author Aleksandr Gorshenin
 */
public class WriteStreamFactoryTest {
    @Test
    public void regularWriteTest() {
        @SuppressWarnings("unchecked")
        GrpcReadWriteStream<FromServer, FromClient> grpc = Mockito.mock(GrpcReadWriteStream.class);

        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.writeSession(Mockito.eq("s1"))).thenReturn(grpc);

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/local/topic")
                .build();

        WriteStreamFactory factory = new WriteStreamFactory(rpc, settings);
        Assert.assertEquals("/local/topic", factory.getTopicPath());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);
        Mockito.verify(rpc).writeSession("s1");
    }

    @Test
    public void writeWithoutDeduplicationTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = new WriteStreamFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.buildInitRequest();
        Assert.assertEquals("/test/topic", req.getPath());
        Assert.assertEquals("", req.getProducerId());
        Assert.assertFalse(req.hasMessageGroupId());
        Assert.assertFalse(req.hasPartitionId());
    }

    @Test
    public void writeWithProducerIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = new WriteStreamFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer")
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.buildInitRequest();
        Assert.assertEquals("/test/topic", req.getPath());
        Assert.assertEquals("producer", req.getProducerId());
        Assert.assertFalse(req.hasMessageGroupId());
        Assert.assertFalse(req.hasPartitionId());
    }

    @Test
    public void writeWithProducerIdAndMessageGroupIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = new WriteStreamFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer")
                .setMessageGroupId("producer")
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.buildInitRequest();
        Assert.assertEquals("/test/topic", req.getPath());
        Assert.assertEquals("producer", req.getProducerId());
        Assert.assertEquals("producer", req.getMessageGroupId());
        Assert.assertFalse(req.hasPartitionId());
    }

    @Test
    public void writeWithPartitionIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = new WriteStreamFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setPartitionId(5L)
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.buildInitRequest();
        Assert.assertEquals(5L, req.getPartitionId());
        Assert.assertFalse(req.hasMessageGroupId());
    }

    @Test
    public void messageGroupAndPartitionErrorTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setMessageGroupId("group-1")
                .setPartitionId(5L)
                .build();
        Exception ex = Assert.assertThrows(IllegalArgumentException.class, () -> new WriteStreamFactory(rpc, settings));
        Assert.assertEquals("Both MessageGroupId and PartitionId are set in WriterSettings", ex.getMessage());
    }
}
