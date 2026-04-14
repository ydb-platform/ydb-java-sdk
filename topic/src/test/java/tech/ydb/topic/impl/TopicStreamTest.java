package tech.ydb.topic.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.StringValue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;


public class TopicStreamTest {
    private static final Logger logger = LoggerFactory.getLogger(TopicStreamTest.class);

    private interface MockedStream extends GrpcReadWriteStream<StringValue, StringValue> { }

    private static class TestStream extends TopicStream<StringValue, StringValue> {
        TestStream(MockedStream mock) {
            super(logger, "test", mock);
        }

        @Override
        protected StringValue updateTokenMessage(String token) {
            return msg("new-token:" + token);
        }

        @Override
        protected Status parseMessageStatus(StringValue message) {
            return "fail".equals(message.getValue()) ? Status.of(StatusCode.ABORTED) : Status.SUCCESS;
        }
    }

    private static StringValue msg(String value) {
        return StringValue.newBuilder().setValue(value).build();
    }

    private MockedStream buildMockedStream(String authToken, CompletableFuture<Status> result) {
        MockedStream grpc = Mockito.mock(MockedStream.class);
        Mockito.when(grpc.authToken()).thenReturn(authToken);
        Mockito.when(grpc.start(Mockito.any())).thenReturn(result);
        return grpc;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<GrpcReadStream.Observer<StringValue>> buildObserver() {
        return ArgumentCaptor.forClass(GrpcReadStream.Observer.class);
    }

    @Test
    public void baseTest() {
        CompletableFuture<Status> streamFuture = new CompletableFuture<>();
        MockedStream mock = buildMockedStream("token", streamFuture);
        ArgumentCaptor<GrpcReadStream.Observer<StringValue>> observer = buildObserver();

        List<StringValue> received = new ArrayList<>();

        TestStream stream = new TestStream(mock);
        CompletableFuture<Status> result = stream.start(msg("init"), received::add);
        Mockito.verify(mock).start(observer.capture());

        stream.send(msg("s1"));
        observer.getValue().onNext(msg("r1"));
        stream.send(msg("s2"));
        stream.send(msg("s3"));
        observer.getValue().onNext(msg("r2"));
        observer.getValue().onNext(msg("r3"));

        Mockito.verify(mock, Mockito.never()).close();
        Mockito.verify(mock, Mockito.never()).cancel();

        Mockito.verify(mock).sendNext(msg("init"));
        Mockito.verify(mock).sendNext(msg("s1"));
        Mockito.verify(mock).sendNext(msg("s2"));
        Mockito.verify(mock).sendNext(msg("s3"));
        Assert.assertEquals(Arrays.asList(msg("r1"), msg("r2"), msg("r3")), received);

        Assert.assertFalse(result.isDone());

        stream.close();

        Mockito.verify(mock).close();
        Mockito.verify(mock, Mockito.never()).cancel();

        streamFuture.complete(Status.SUCCESS);

        stream.close(); // no effect
    }

    @Test
    public void startStreamAndImmediatelyFinishTest() {
        // Stream may be closed immediately after start, for example if token is invalid
        CompletableFuture<Status> status = new CompletableFuture<>();
        status.completeExceptionally(new RuntimeException("error"));
        MockedStream mock = buildMockedStream("token", status);

        TestStream stream = new TestStream(mock);
        stream.start(msg("init-req"), msg -> {});

        Mockito.verify(mock).start(Mockito.any());
        stream.send(msg("s1"));

        Mockito.verify(mock, Mockito.never()).sendNext(Mockito.any());
        Mockito.verify(mock, Mockito.never()).close();
        Mockito.verify(mock, Mockito.never()).cancel();
    }

    @Test
    public void nonSuccessMessageStopsStreamTest() {
        CompletableFuture<Status> streamFuture = new CompletableFuture<>();
        MockedStream mock = buildMockedStream("token", streamFuture);
        ArgumentCaptor<GrpcReadStream.Observer<StringValue>> observer = buildObserver();

        List<StringValue> received = new ArrayList<>();

        TestStream stream = new TestStream(mock);
        CompletableFuture<Status> result = stream.start(msg("init"), received::add);

        Mockito.verify(mock).start(observer.capture());

        stream.send(msg("s1"));
        observer.getValue().onNext(msg("r1"));
        stream.send(msg("s2"));
        observer.getValue().onNext(msg("r2"));
        stream.send(msg("s3"));
        observer.getValue().onNext(msg("fail"));
        observer.getValue().onNext(msg("fail")); // no effect

        Assert.assertTrue(result.isDone());
        Mockito.verify(mock).close();
        Mockito.verify(mock, Mockito.never()).cancel();

        Mockito.verify(mock).sendNext(msg("init"));
        Mockito.verify(mock).sendNext(msg("s1"));
        Mockito.verify(mock).sendNext(msg("s2"));
        Assert.assertEquals(Arrays.asList(msg("r1"), msg("r2")), received);
    }

    @Test
    public void tokenUpdatesTest() {
        CompletableFuture<Status> streamFuture = new CompletableFuture<>();
        MockedStream mock = Mockito.mock(MockedStream.class);
        Mockito.when(mock.authToken()).thenReturn("t", "t", "t", "t2", "t2", "t3");
        Mockito.when(mock.start(Mockito.any())).thenReturn(streamFuture);

        TestStream stream = new TestStream(mock);
        stream.start(msg("init"), msg -> {});

        Mockito.verify(mock).start(Mockito.any());

        stream.send(msg("s1"));
        stream.send(msg("s2"));
        stream.send(msg("s3"));
        stream.send(msg("s4"));
        stream.send(msg("s5"));
        stream.send(msg("s6"));

        stream.close();

        Mockito.verify(mock).close();
        Mockito.verify(mock, Mockito.never()).cancel();
        Mockito.verify(mock).sendNext(msg("init"));
        Mockito.verify(mock).sendNext(msg("s1"));
        Mockito.verify(mock).sendNext(msg("s2"));
        Mockito.verify(mock).sendNext(msg("s3"));
        Mockito.verify(mock).sendNext(msg("new-token:t2"));
        Mockito.verify(mock).sendNext(msg("s4"));
        Mockito.verify(mock).sendNext(msg("s5"));
        Mockito.verify(mock).sendNext(msg("new-token:t3"));
        Mockito.verify(mock).sendNext(msg("s6"));
    }
}
