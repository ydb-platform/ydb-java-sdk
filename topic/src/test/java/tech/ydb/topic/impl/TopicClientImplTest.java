package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.CommitOffsetSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeConsumerSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TopicClientImplTest {

    @Test
    public void testEnablePreferReadyChannelSetting() {
        TopicRpc mock = mock(TopicRpc.class);
        when(mock.createTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.dropTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.alterTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.commitOffset(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.describeTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(
                Result.success(mock(YdbTopic.DescribeTopicResult.class))));
        when(mock.describeConsumer(any(), any())).thenReturn(CompletableFuture.completedFuture(
                Result.success(mock(YdbTopic.DescribeConsumerResult.class))));

        final String topic = "topic";
        final String consumer = "consumer";
        final String sessionId = "sessionId";

        ArgumentCaptor<GrpcRequestSettings> requestCaptor = ArgumentCaptor.forClass(GrpcRequestSettings.class);
        TopicClient client = TopicClientImpl.newClient(mock).build();

        // createTopic
        client.createTopic(topic, CreateTopicSettings.newBuilder()
                .withPreferReadyChannel(true)
                .build());

        verify(mock).createTopic(any(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isPreferReadyChannel());

        // dropTopic
        client.dropTopic(topic, DropTopicSettings.newBuilder()
                .withPreferReadyChannel(true)
                .build());

        verify(mock).dropTopic(any(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isPreferReadyChannel());

        // alterTopic
        client.alterTopic(topic, AlterTopicSettings.newBuilder()
                .withPreferReadyChannel(true)
                .build());

        verify(mock).alterTopic(any(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isPreferReadyChannel());

        // commitOffset
        client.commitOffset(topic, CommitOffsetSettings.newBuilder()
                .setReadSessionId(sessionId)
                .withPreferReadyChannel(true)
                .setConsumer(consumer)
                .setPartitionId(0)
                .build());

        verify(mock).commitOffset(any(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isPreferReadyChannel());

        // describeTopic
        client.describeTopic(topic, DescribeTopicSettings.newBuilder()
                .withPreferReadyChannel(true)
                .build());

        verify(mock).describeTopic(any(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isPreferReadyChannel());

        // describeConsumer
        client.describeConsumer(topic, consumer, DescribeConsumerSettings.newBuilder()
                .withPreferReadyChannel(true)
                .build());

        verify(mock).describeConsumer(any(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isPreferReadyChannel());
    }

    @Test
    public void testDefaultPreferReadyChannelSetting() {
        TopicRpc mock = mock(TopicRpc.class);
        when(mock.createTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.dropTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.alterTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.commitOffset(any(), any())).thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        when(mock.describeTopic(any(), any())).thenReturn(CompletableFuture.completedFuture(
                Result.success(mock(YdbTopic.DescribeTopicResult.class))));
        when(mock.describeConsumer(any(), any())).thenReturn(CompletableFuture.completedFuture(
                Result.success(mock(YdbTopic.DescribeConsumerResult.class))));

        final String topic = "topic";
        final String consumer = "consumer";
        final String sessionId = "sessionId";

        ArgumentCaptor<GrpcRequestSettings> requestCaptor = ArgumentCaptor.forClass(GrpcRequestSettings.class);
        TopicClient client = TopicClientImpl.newClient(mock).build();

        // createTopic
        client.createTopic(topic, CreateTopicSettings.newBuilder()
                .build());

        verify(mock).createTopic(any(), requestCaptor.capture());
        assertFalse(requestCaptor.getValue().isPreferReadyChannel());

        // dropTopic
        client.dropTopic(topic, DropTopicSettings.newBuilder()
                .build());

        verify(mock).dropTopic(any(), requestCaptor.capture());
        assertFalse(requestCaptor.getValue().isPreferReadyChannel());

        // alterTopic
        client.alterTopic(topic, AlterTopicSettings.newBuilder()
                .build());

        verify(mock).alterTopic(any(), requestCaptor.capture());
        assertFalse(requestCaptor.getValue().isPreferReadyChannel());

        // commitOffset
        client.commitOffset(topic, CommitOffsetSettings.newBuilder()
                .setReadSessionId(sessionId)
                .setConsumer(consumer)
                .setPartitionId(0)
                .build());

        verify(mock).commitOffset(any(), requestCaptor.capture());
        assertFalse(requestCaptor.getValue().isPreferReadyChannel());

        // describeTopic
        client.describeTopic(topic, DescribeTopicSettings.newBuilder()
                .build());

        verify(mock).describeTopic(any(), requestCaptor.capture());
        assertFalse(requestCaptor.getValue().isPreferReadyChannel());

        // describeConsumer
        client.describeConsumer(topic, consumer, DescribeConsumerSettings.newBuilder()
                .build());

        verify(mock).describeConsumer(any(), requestCaptor.capture());
        assertFalse(requestCaptor.getValue().isPreferReadyChannel());
    }
}
