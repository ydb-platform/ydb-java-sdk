package tech.ydb.topic.write.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.InitResponse;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class WriteStreamDirectFactoryTest {
    private static GrpcReadWriteStream<FromServer, FromClient> mockGrpcStream() {
        @SuppressWarnings("unchecked")
        GrpcReadWriteStream<FromServer, FromClient> grpc = Mockito.mock(GrpcReadWriteStream.class);
        Mockito.when(grpc.authToken()).thenReturn("");
        Mockito.when(grpc.start(Mockito.any())).thenReturn(new CompletableFuture<>());
        return grpc;
    }

    private static void mockStreamError(GrpcReadWriteStream<FromServer, FromClient> mock, Status error) {
        Mockito.when(mock.start(Mockito.any())).thenReturn(CompletableFuture.completedFuture(error));
    }

    private static void mockStreamResponse(GrpcReadWriteStream<FromServer, FromClient> mock, FromServer response) {
        CompletableFuture<Status> result = new CompletableFuture<>();

        Mockito.when(mock.start(Mockito.any())).thenAnswer(iom -> {
            GrpcReadStream.Observer<FromServer> obs = iom.getArgument(0);
            obs.onNext(response);
            return result;
        }).thenReturn(result);

        Mockito.doAnswer((iom) -> {
            result.complete(Status.SUCCESS);
            return null;
        }).when(mock).close();
    }

    private static YdbTopic.DescribeTopicResult.PartitionInfo partition(long partitionId, int nodeId, long generation) {
        return YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder()
                .setPartitionId(partitionId)
                .setPartitionLocation(YdbTopic.PartitionLocation.newBuilder()
                        .setNodeId(nodeId)
                        .setGeneration(generation)
                        .build())
                .build();
    }

    private static void mockDescribeResult(TopicRpc rpc, YdbTopic.DescribeTopicResult.PartitionInfo... partitions) {
        Mockito.when(rpc.describeTopic(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.success(
                        YdbTopic.DescribeTopicResult.newBuilder().addAllPartitions(Arrays.asList(partitions)).build())
                ));
    }

    private static void mockDescribeResult(TopicRpc rpc, Status status) {
        Mockito.when(rpc.describeTopic(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(status)));
    }

    @Test
    public void invalidDirectWriteTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/local/topic")
                .setDirectWrite(true) // requires producerId or partitionId
                .build();

        Exception ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> new WriteStreamDirectFactory(rpc, settings)
        );
        Assert.assertEquals("Direct writing requires PartitionId or ProducerId in WriterSettings", ex.getMessage());
    }

    @Test
    public void directWriteByPartitionIdTest() {
        GrpcReadWriteStream<FromServer, FromClient> grpc = mockGrpcStream();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        mockDescribeResult(rpc, partition(1L, 10, 3L), partition(2L, 42, 1L), partition(3L, 23, 2L));
        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(grpc);

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/local/topic")
                .setPartitionId(2L)
                .setDirectWrite(true)
                .build();

        // just verify it doesn't throw and returns a factory for the correct topic
        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, settings);
        Assert.assertEquals("/local/topic", factory.getTopicPath());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);

        ArgumentCaptor<GrpcRequestSettings> options = ArgumentCaptor.forClass(GrpcRequestSettings.class);
        Mockito.verify(rpc).writeSession(options.capture());
        Assert.assertTrue(options.getValue().isDirectMode());
        Assert.assertEquals(42, options.getValue().getPreferredNodeID().intValue());

        stream.start(null);

        ArgumentCaptor<FromClient> msg = ArgumentCaptor.forClass(FromClient.class);
        Mockito.verify(grpc).sendNext(msg.capture());
        Assert.assertTrue(msg.getValue().hasInitRequest());
        Assert.assertEquals("/local/topic", msg.getValue().getInitRequest().getPath());
        Assert.assertFalse(msg.getValue().getInitRequest().hasPartitionId());
        Assert.assertEquals("", msg.getValue().getInitRequest().getProducerId());
        Assert.assertEquals(2L, msg.getValue().getInitRequest().getPartitionWithGeneration().getPartitionId());
        Assert.assertEquals(1L, msg.getValue().getInitRequest().getPartitionWithGeneration().getGeneration());
    }

    @Test
    public void directWriteByPartitionIdTestDescribeFailTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        mockDescribeResult(rpc, Status.of(StatusCode.UNAVAILABLE));

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setPartitionId(3L)
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");

        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(GrpcRequestSettings.class));

        Assert.assertTrue(stream instanceof WriteStream.Fail);
        CompletableFuture<Status> res = stream.start(null);
        Assert.assertTrue(res.isDone());
        Assert.assertEquals(Status.of(StatusCode.UNAVAILABLE), res.join());

        stream.close(); // no effect
    }

    @Test
    public void directWriteByPartitionIdTestPartitionNotFoundTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        // result has partition 5, but we're looking for partition 3
        mockDescribeResult(rpc, partition(4L, 99, 1L), partition(5L, 100, 2L));

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setPartitionId(3L)
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");

        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(GrpcRequestSettings.class));

        Assert.assertTrue(stream instanceof WriteStream.Fail);
        CompletableFuture<Status> res = stream.start(null);
        Assert.assertTrue(res.isDone());
        Status expected = Status.of(StatusCode.BAD_REQUEST, Issue.of("Cannot find partition 3", Issue.Severity.ERROR));
        Assert.assertEquals(expected, res.join());

        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();
        GrpcReadWriteStream<FromServer, FromClient> actualGrpc = mockGrpcStream();

        FromServer initResponse = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(InitResponse.newBuilder()
                        .setLastSeqNo(0)
                        .setPartitionId(7L)
                        .setSessionId("session")
                        .build())
                .build();

        mockStreamResponse(probeGrpc, initResponse);
        mockDescribeResult(rpc, partition(7L, 55, 3L));

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class)))
                .thenReturn(probeGrpc).thenReturn(actualGrpc);

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);

        ArgumentCaptor<GrpcRequestSettings> options = ArgumentCaptor.forClass(GrpcRequestSettings.class);
        Mockito.verify(rpc, Mockito.times(2)).writeSession(options.capture());
        Assert.assertTrue(options.getValue().isDirectMode());
        Assert.assertEquals(55, options.getValue().getPreferredNodeID().intValue());

        stream.start(null);

        ArgumentCaptor<FromClient> msg = ArgumentCaptor.forClass(FromClient.class);
        Mockito.verify(actualGrpc).sendNext(msg.capture());
        Assert.assertTrue(msg.getValue().hasInitRequest());
        Assert.assertEquals("/test/topic", msg.getValue().getInitRequest().getPath());
        Assert.assertFalse(msg.getValue().getInitRequest().hasPartitionId());
        Assert.assertEquals("producer-1", msg.getValue().getInitRequest().getProducerId());
        Assert.assertEquals(7L, msg.getValue().getInitRequest().getPartitionWithGeneration().getPartitionId());
        Assert.assertEquals(3L, msg.getValue().getInitRequest().getPartitionWithGeneration().getGeneration());
    }

    @Test
    public void directWriteByProducerIdProbeFailTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();

        mockStreamError(probeGrpc, Status.of(StatusCode.UNAUTHORIZED));

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probeGrpc);

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        Mockito.verify(rpc).writeSession(Mockito.any(GrpcRequestSettings.class));

        CompletableFuture<Status> res = stream.start(null);
        Assert.assertTrue(res.isDone());
        Assert.assertEquals(Status.of(StatusCode.UNAUTHORIZED), res.join());
        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdProbeWrongResponseTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();

        FromServer initResponse = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.INTERNAL_ERROR)
                .build();
        mockStreamResponse(probeGrpc, initResponse);

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probeGrpc);

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        Mockito.verify(rpc).writeSession(Mockito.any(GrpcRequestSettings.class));

        CompletableFuture<Status> res = stream.start(null);
        Assert.assertTrue(res.isDone());
        Assert.assertEquals(Status.of(StatusCode.INTERNAL_ERROR), res.join());
        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdProbeUnexpectedResponseTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();

        FromServer initResponse = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setUpdateTokenResponse(YdbTopic.UpdateTokenResponse.newBuilder().build())
                .build();
        mockStreamResponse(probeGrpc, initResponse);

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probeGrpc);

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        Mockito.verify(rpc).writeSession(Mockito.any(GrpcRequestSettings.class));

        CompletableFuture<Status> res = stream.start(null);
        Assert.assertTrue(res.isDone());
        Issue issue = Issue.of("Unexpected message from stream with producer producer-1", Issue.Severity.ERROR);
        Assert.assertEquals(Status.of(StatusCode.BAD_REQUEST, issue), res.join());
        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdPartitionNotFoundTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();

        FromServer initResponse = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(InitResponse.newBuilder()
                        .setLastSeqNo(0)
                        .setPartitionId(5L)
                        .setSessionId("session")
                        .build())
                .build();

        mockStreamResponse(probeGrpc, initResponse);
        mockDescribeResult(rpc, partition(1L, 55, 8L), partition(2L, 55, 7L));

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probeGrpc);

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        CompletableFuture<Status> res = stream.start(null);
        Assert.assertTrue(res.isDone());
        Status expected = Status.of(StatusCode.BAD_REQUEST, Issue.of("Cannot find partition 5", Issue.Severity.ERROR));
        Assert.assertEquals(expected, res.join());
    }
}
