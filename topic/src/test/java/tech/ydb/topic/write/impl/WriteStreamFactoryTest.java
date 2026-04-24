package tech.ydb.topic.write.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicResult;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;

/**
 * @author Aleksandr Gorshenin
 */
public class WriteStreamFactoryTest {

    @SuppressWarnings("unchecked")
    private static GrpcReadWriteStream<FromServer, FromClient> mockGrpcStream() {
        GrpcReadWriteStream<FromServer, FromClient> grpc = Mockito.mock(GrpcReadWriteStream.class);
        Mockito.when(grpc.authToken()).thenReturn("");
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

    private static DescribeTopicResult.PartitionInfo partition(long partitionId, int nodeId) {
        return DescribeTopicResult.PartitionInfo.newBuilder()
                .setPartitionId(partitionId)
                .setPartitionLocation(YdbTopic.PartitionLocation.newBuilder()
                        .setNodeId(nodeId)
                        .build())
                .build();
    }

    private static void mockDescribeResult(TopicRpc rpc, DescribeTopicResult.PartitionInfo... partitions) {
        Mockito.when(rpc.describeTopic(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.success(
                        DescribeTopicResult.newBuilder().addAllPartitions(Arrays.asList(partitions)).build())
                ));
    }

    private static void mockDescribeResult(TopicRpc rpc, Status status) {
        Mockito.when(rpc.describeTopic(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(status)));
    }

    @Test
    public void regularWriteTest() {
        GrpcReadWriteStream<FromServer, FromClient> grpc = mockGrpcStream();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.writeSession(Mockito.eq("s1"))).thenReturn(grpc);

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/local/topic")
                .build();

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, settings);
        Assert.assertEquals("/local/topic", factory.getTopicPath());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);
        Mockito.verify(rpc).writeSession("s1");
    }

    @Test
    public void writeWithoutDeduplicationTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.initRequest()
                .getInitRequest();
        Assert.assertEquals("/test/topic", req.getPath());
        Assert.assertEquals("", req.getProducerId());
        Assert.assertFalse(req.hasMessageGroupId());
        Assert.assertFalse(req.hasPartitionId());
    }

    @Test
    public void writeWithProducerIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer")
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.initRequest()
                .getInitRequest();
        Assert.assertEquals("/test/topic", req.getPath());
        Assert.assertEquals("producer", req.getProducerId());
        Assert.assertFalse(req.hasMessageGroupId());
        Assert.assertFalse(req.hasPartitionId());
    }

    @Test
    public void writeWithProducerIdAndMessageGroupIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer")
                .setMessageGroupId("producer")
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.initRequest()
                .getInitRequest();
        Assert.assertEquals("/test/topic", req.getPath());
        Assert.assertEquals("producer", req.getProducerId());
        Assert.assertEquals("producer", req.getMessageGroupId());
        Assert.assertFalse(req.hasPartitionId());
    }

    @Test
    public void writeWithPartitionIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setPartitionId(5L)
                .build());

        YdbTopic.StreamWriteMessage.InitRequest req = factory.initRequest().getInitRequest();
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
        Exception ex = Assert.assertThrows(IllegalArgumentException.class, () -> WriteStreamFactory.of(rpc, settings));
        Assert.assertEquals("Both MessageGroupId and PartitionId are set in WriterSettings", ex.getMessage());
    }

    @Test
    public void invalidDirectWriteTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/local/topic")
                .setDirectWrite(true) // requires producerId or partitionId
                .build();

        Exception ex = Assert.assertThrows(IllegalArgumentException.class, () -> WriteStreamFactory.of(rpc, settings));
        Assert.assertEquals("Direct writing requires PartitionId or ProducerId in WriterSettings", ex.getMessage());
    }

    @Test
    public void directWriteByPartitionIdTest() {
        GrpcReadWriteStream<FromServer, FromClient> grpc = mockGrpcStream();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        mockDescribeResult(rpc, partition(1L, 10), partition(2L, 42), partition(3L, 23));
        Mockito.when(rpc.writeSession(Mockito.eq("s1"), Mockito.eq(42))).thenReturn(grpc);

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/local/topic")
                .setPartitionId(2L)
                .setDirectWrite(true)
                .build();

        // just verify it doesn't throw and returns a factory for the correct topic
        WriteStreamFactory factory = WriteStreamFactory.of(rpc, settings);
        Assert.assertEquals("/local/topic", factory.getTopicPath());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);
        Mockito.verify(rpc).writeSession("s1", 42);
    }

    @Test
    public void directWriteByPartitionIdTestDescribeFailTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        mockDescribeResult(rpc, Status.of(StatusCode.UNAVAILABLE));

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setPartitionId(3L)
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");

        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(), Mockito.any(Integer.class));

        Assert.assertTrue(stream instanceof WriteStream.Fail);
        CompletableFuture<Status> res = stream.start(null, null);
        Assert.assertTrue(res.isDone());
        Assert.assertEquals(Status.of(StatusCode.UNAVAILABLE), res.join());

        stream.close(); // no effect
    }

    @Test
    public void directWriteByPartitionIdTestPartitionNotFoundTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        // result has partition 5, but we're looking for partition 3
        mockDescribeResult(rpc, partition(4L, 99), partition(5L, 100));

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setPartitionId(3L)
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");

        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(), Mockito.any(Integer.class));

        Assert.assertTrue(stream instanceof WriteStream.Fail);
        CompletableFuture<Status> res = stream.start(null, null);
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
                .setInitResponse(YdbTopic.StreamWriteMessage.InitResponse.newBuilder()
                        .setLastSeqNo(0)
                        .setPartitionId(7L)
                        .setSessionId("session")
                        .build())
                .build();

        mockStreamResponse(probeGrpc, initResponse);
        mockDescribeResult(rpc, partition(7L, 55));

        Mockito.when(rpc.writeSession(Mockito.any())).thenReturn(probeGrpc);
        Mockito.when(rpc.writeSession(Mockito.any(), Mockito.eq(55))).thenReturn(actualGrpc);

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);
        Mockito.verify(rpc).writeSession("s1");
        Mockito.verify(rpc).writeSession("s1", 55);
    }

    @Test
    public void directWriteByProducerIdProbeFailTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();

        mockStreamError(probeGrpc, Status.of(StatusCode.UNAUTHORIZED));

        Mockito.when(rpc.writeSession(Mockito.any())).thenReturn(probeGrpc);

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        Mockito.verify(rpc).writeSession("s1");
        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(), Mockito.any(Integer.class));

        CompletableFuture<Status> res = stream.start(null, null);
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

        Mockito.when(rpc.writeSession(Mockito.any())).thenReturn(probeGrpc);

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        Mockito.verify(rpc).writeSession("s1");
        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(), Mockito.any(Integer.class));

        CompletableFuture<Status> res = stream.start(null, null);
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

        Mockito.when(rpc.writeSession(Mockito.any())).thenReturn(probeGrpc);

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        Mockito.verify(rpc).writeSession("s1");
        Mockito.verify(rpc, Mockito.never()).writeSession(Mockito.any(), Mockito.any(Integer.class));

        CompletableFuture<Status> res = stream.start(null, null);
        Assert.assertTrue(res.isDone());
        Issue issue = Issue.of("Unexpected message from stream with producer producer-1", Issue.Severity.ERROR);
        Assert.assertEquals(Status.of(StatusCode.BAD_REQUEST, issue), res.join());
        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdPartitionNotFoundTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        GrpcReadWriteStream<FromServer, FromClient> probeGrpc = mockGrpcStream();
        GrpcReadWriteStream<FromServer, FromClient> actualGrpc = mockGrpcStream();

        FromServer initResponse = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(YdbTopic.StreamWriteMessage.InitResponse.newBuilder()
                        .setLastSeqNo(0)
                        .setPartitionId(5L)
                        .setSessionId("session")
                        .build())
                .build();

        mockStreamResponse(probeGrpc, initResponse);
        mockDescribeResult(rpc, partition(1L, 55), partition(2L, 55));

        Mockito.when(rpc.writeSession(Mockito.any())).thenReturn(probeGrpc);
        Mockito.when(rpc.writeSession(Mockito.any(), Mockito.eq(55))).thenReturn(actualGrpc);

        WriteStreamFactory factory = WriteStreamFactory.of(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream.Fail);
        CompletableFuture<Status> res = stream.start(null, null);
        Assert.assertTrue(res.isDone());
        Status expected = Status.of(StatusCode.BAD_REQUEST, Issue.of("Cannot find partition 5", Issue.Severity.ERROR));
        Assert.assertEquals(expected, res.join());
    }
}
