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
    private static class MockedStream {
        @SuppressWarnings("unchecked")
        private final GrpcReadWriteStream<FromServer, FromClient> grpc = Mockito.mock(GrpcReadWriteStream.class);
        @SuppressWarnings("unchecked")
        private final ArgumentCaptor<GrpcReadStream.Observer<FromServer>> observer = ArgumentCaptor
                .forClass(GrpcReadStream.Observer.class);

        private final CompletableFuture<Status> result = new CompletableFuture<>();
        private final ArgumentCaptor<FromClient> msg = ArgumentCaptor.forClass(FromClient.class);

        public MockedStream() {
            Mockito.when(grpc.authToken()).thenReturn("");
            Mockito.when(grpc.start(observer.capture())).thenReturn(result);
        }

        public FromClient verifyNextMsg() {
            Mockito.verify(grpc).sendNext(msg.capture());
            return msg.getValue();
        }

        public void responseWith(FromServer response) {
            Mockito.doAnswer((iom) -> {
                observer.getValue().onNext(response);
                return null;
            }).when(grpc).sendNext(Mockito.any());
        }

        public void responseWith(Status status) {
            Mockito.doAnswer((iom) -> {
                result.complete(status);
                return null;
            }).when(grpc).sendNext(Mockito.any());
        }

        public void responseWith(Exception ex) {
            Mockito.doAnswer((iom) -> {
                result.completeExceptionally(ex);
                return null;
            }).when(grpc).sendNext(Mockito.any());
        }

        public void closeImmediatelly(Status status) {
            result.complete(status);
        }

        public void fail(FromServer response) {
            Mockito.doAnswer((iom) -> {
                observer.getValue().onNext(response);
                return null;
            }).when(grpc).sendNext(Mockito.any());
        }
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
        MockedStream mocked = new MockedStream();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        mockDescribeResult(rpc, partition(1L, 10, 3L), partition(2L, 42, 1L), partition(3L, 23, 2L));
        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(mocked.grpc);

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

        FromClient msg = mocked.verifyNextMsg();
        Assert.assertTrue(msg.hasInitRequest());
        Assert.assertEquals("/local/topic", msg.getInitRequest().getPath());
        Assert.assertFalse(msg.getInitRequest().hasPartitionId());
        Assert.assertEquals("", msg.getInitRequest().getProducerId());
        Assert.assertEquals(2L, msg.getInitRequest().getPartitionWithGeneration().getPartitionId());
        Assert.assertEquals(1L, msg.getInitRequest().getPartitionWithGeneration().getGeneration());
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
    public void directWriteByPartitionIdTestPartitionHasNoLocationTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        mockDescribeResult(rpc, YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder()
                .setPartitionId(3L)
                .build());

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
        Status expected = Status.of(StatusCode.BAD_REQUEST, Issue.of("Partition 3 has no location", Issue.Severity.ERROR));
        Assert.assertEquals(expected, res.join());

        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        MockedStream probe = new MockedStream();
        MockedStream actual = new MockedStream();

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class)))
                .thenReturn(probe.grpc).thenReturn(actual.grpc);

        mockDescribeResult(rpc, partition(7L, 55, 3L));

        probe.responseWith(FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(InitResponse.newBuilder()
                        .setLastSeqNo(0)
                        .setPartitionId(7L)
                        .setSessionId("session")
                        .build())
                .build());

        WriteStreamFactory factory = new WriteStreamDirectFactory(rpc, WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("producer-1")
                .setMessageGroupId("producer-1")
                .setDirectWrite(true)
                .build());

        WriteSession.Stream stream = factory.createNewStream("s1");
        Assert.assertTrue(stream instanceof WriteStream);

        ArgumentCaptor<GrpcRequestSettings> options = ArgumentCaptor.forClass(GrpcRequestSettings.class);
        Mockito.verify(rpc, Mockito.times(2)).writeSession(options.capture());
        Assert.assertTrue(options.getValue().isDirectMode());
        Assert.assertEquals(55, options.getValue().getPreferredNodeID().intValue());

        stream.start(null);

        FromClient msg = actual.verifyNextMsg();
        Assert.assertTrue(msg.hasInitRequest());
        Assert.assertEquals("/test/topic", msg.getInitRequest().getPath());
        Assert.assertFalse(msg.getInitRequest().hasPartitionId());
        Assert.assertEquals("producer-1", msg.getInitRequest().getProducerId());
        Assert.assertEquals("", msg.getInitRequest().getMessageGroupId()); // never used for direct-write
        Assert.assertEquals(7L, msg.getInitRequest().getPartitionWithGeneration().getPartitionId());
        Assert.assertEquals(3L, msg.getInitRequest().getPartitionWithGeneration().getGeneration());
    }

    @Test
    public void directWriteByProducerIdProbeFailTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        MockedStream probe = new MockedStream();
        probe.closeImmediatelly(Status.of(StatusCode.UNAUTHORIZED));
        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probe.grpc);

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
    public void directWriteByProducerIdProbeFailOnSendTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        MockedStream probe = new MockedStream();
        probe.responseWith(Status.of(StatusCode.PRECONDITION_FAILED));
        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probe.grpc);

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
        Assert.assertEquals(Status.of(StatusCode.PRECONDITION_FAILED), res.join());
        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdProbeExceptionOnSendTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        MockedStream probe = new MockedStream();
        probe.responseWith(new RuntimeException("something went wrong"));
        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probe.grpc);

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
        Status status = res.join();
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, status.getCode());
        Assert.assertNotNull(status.getCause());
        Assert.assertEquals("something went wrong", status.getCause().getMessage());
        stream.close(); // no effect
    }

    @Test
    public void directWriteByProducerIdProbeWrongResponseTest() {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);

        MockedStream probe = new MockedStream();
        probe.responseWith(FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.INTERNAL_ERROR)
                .build());

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probe.grpc);

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

        MockedStream probe = new MockedStream();
        probe.responseWith(FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setUpdateTokenResponse(YdbTopic.UpdateTokenResponse.newBuilder().build())
                .build());

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probe.grpc);

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

        MockedStream probe = new MockedStream();
        probe.responseWith(FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(InitResponse.newBuilder()
                        .setLastSeqNo(0)
                        .setPartitionId(5L)
                        .setSessionId("session")
                        .build())
                .build());

        mockDescribeResult(rpc, partition(1L, 55, 8L), partition(2L, 55, 7L));

        Mockito.when(rpc.writeSession(Mockito.any(GrpcRequestSettings.class))).thenReturn(probe.grpc);

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
