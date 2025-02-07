package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicRetriesTest extends BaseMockedTest {

    @Test
    public void writerDefaultRetryTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(defaultStreamMockAnswer())
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(defaultStreamMockAnswer()); // and repeat

        SyncWriter writer = client.createSyncWriter(WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .build());
        writer.init();

        // Retry #1 - TRANSPORT_UNAVAILABLE
        Assert.assertNull(currentStream());
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(0);

        writer.send(Message.of("test-message".getBytes()));
        stream1.nextMsg().isWrite().hasWrite(2, 1);
        stream1.responseWriteWritten(1, 1);

        stream1.complete(Status.of(StatusCode.SUCCESS));

        // Retry #2 - Stream is closed by server
        getScheduler().hasTasks(1).executeNextTasks(1);

        // Retry #3 - OVERLOADED
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream2 = currentStream();
        Assert.assertNotEquals(stream1, stream2);

        stream2.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream2.hasNoNewMessages();
        stream2.responseErrorBadRequest();

        // Retry #4 - Stream send bad request
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream3 = currentStream();
        Assert.assertNotEquals(stream2, stream3);

        stream3.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream3.hasNoNewMessages();
        stream3.responseInit(1);

        writer.send(Message.of("other-message".getBytes()));
        stream3.nextMsg().isWrite().hasWrite(2, 2);
        stream3.responseWriteWritten(2, 1);

        writer.flush();
        writer.shutdown(1, TimeUnit.SECONDS);
        stream3.complete(Status.SUCCESS);
    }

    @Test
    public void writerNoRetryNetworkErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE));

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.noRetries())
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        // No stream and no retries in scheduler
        Assert.assertNull(currentStream());
        getScheduler().hasNoTasks();

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> writer.send(Message.of("test-message".getBytes())));
        Assert.assertEquals("Writer is already stopped", ex.getMessage());

        writer.shutdown(1, TimeUnit.SECONDS);
    }

    @Test
    public void writerNoRetryStreamCloseTest() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.noRetries())
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(0);

        // Even successful completing closes writer
        stream1.complete(Status.SUCCESS);

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> writer.send(Message.of("test-message".getBytes())));
        Assert.assertEquals("Writer is already stopped", ex.getMessage());

        writer.shutdown(1, TimeUnit.SECONDS);
    }

    @Test
    public void writerNoRetryStreamErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.noRetries())
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(0);

        stream1.responseErrorBadRequest();
        stream1.complete(Status.SUCCESS);

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> writer.send(Message.of("test-message".getBytes())));
        Assert.assertEquals("Writer is already stopped", ex.getMessage());

        writer.shutdown(1, TimeUnit.SECONDS);
    }

    @Test
    public void writerIdempotentRetryTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(defaultStreamMockAnswer())
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(defaultStreamMockAnswer()); // and repeat

        SyncWriter writer = client.createSyncWriter(WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setProducerId("test-id")
                .setRetryConfig(RetryConfig.idempotentRetryForever())
                .build());
        writer.init();

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(10);

        writer.send(Message.of("test-message".getBytes()));
        stream1.nextMsg().isWrite().hasWrite(2, 11);
        stream1.responseWriteWritten(11, 1);

        stream1.complete(new RuntimeException("io exception",
                new UnexpectedResultException("inner", Status.of(StatusCode.CLIENT_INTERNAL_ERROR)))
        );

        // Retry #1 - Stream is by runtime exception
        getScheduler().hasTasks(1).executeNextTasks(1);

        // Retry #2 - OVERLOADED
        getScheduler().hasTasks(1).executeNextTasks(1);
        // Retry #3 - TRANSPORT_UNAVAILABLE
        getScheduler().hasTasks(1).executeNextTasks(1);
        // Retry #4 - OVERLOADED
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream2 = currentStream();
        Assert.assertNotEquals(stream1, stream2);

        stream2.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream2.hasNoNewMessages();
        stream2.responseInit(12);

        writer.send(Message.of("other-message".getBytes()));
        stream2.nextMsg().isWrite().hasWrite(2, 13);
        stream2.responseWriteWritten(13, 1);

        writer.flush();
        writer.shutdown(1, TimeUnit.SECONDS);
        stream2.complete(Status.SUCCESS);
    }

    @Test
    public void asyncWriterDefaultRetryTest() throws QueueOverflowException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(defaultStreamMockAnswer())
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(defaultStreamMockAnswer()); // and repeat

        AsyncWriter writer = client.createAsyncWriter(WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setProducerId("producer-id")
                .build());
        CompletableFuture<InitResult> initFuture = writer.init();

        // Retry #1 - TRANSPORT_UNAVAILABLE
        Assert.assertNull(currentStream());
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(3);

        Assert.assertTrue(initFuture.isDone());
        Assert.assertEquals(3, initFuture.join().getSeqNo());

        CompletableFuture<WriteAck> m1Future = writer.send(Message.of("test-message".getBytes()));
        CompletableFuture<WriteAck> m2Future = writer.send(Message.of("test-message2".getBytes()));

        stream1.nextMsg().isWrite().hasWrite(2, 4); // m1
        stream1.nextMsg().isWrite().hasWrite(2, 5); // m2

        Assert.assertFalse(m1Future.isDone());
        Assert.assertFalse(m2Future.isDone());

        stream1.responseWriteWritten(4, 1); // ack for m1

        Assert.assertTrue(m1Future.isDone());
        Assert.assertEquals(4, m1Future.join().getSeqNo());
        Assert.assertFalse(m2Future.isDone());

        stream1.complete(Status.of(StatusCode.BAD_SESSION));

        // Retry #2 - Stream is closed by server
        getScheduler().hasTasks(1).executeNextTasks(1);

        // Retry #3 - OVERLOADED
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream2 = currentStream();
        Assert.assertNotEquals(stream1, stream2);

        stream2.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream2.hasNoNewMessages();
        stream2.responseInit(4);

        stream2.nextMsg().isWrite().hasWrite(2, 5); // m2

        CompletableFuture<WriteAck> m3Future = writer.send(Message.of("other-message3".getBytes()));

        stream2.responseErrorSchemeError();

        // Retry #4 - Stream send bad request
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream3 = currentStream();
        Assert.assertNotEquals(stream2, stream3);

        stream3.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream3.hasNoNewMessages();
        stream3.responseInit(4);

        stream3.nextMsg().isWrite().hasWrite(2, 5, 6); // m2 & m3

        Assert.assertFalse(m2Future.isDone());
        Assert.assertFalse(m3Future.isDone());

        stream3.responseWriteWritten(5, 2);

        Assert.assertTrue(m2Future.isDone());
        Assert.assertEquals(5, m2Future.join().getSeqNo());
        Assert.assertTrue(m3Future.isDone());
        Assert.assertEquals(6, m3Future.join().getSeqNo());

        writer.shutdown();
        stream3.complete(Status.SUCCESS);
    }

    @Test
    public void asyncWriterIdempotentRetryTest() throws QueueOverflowException {
        mockStreams()
                .then(defaultStreamMockAnswer())
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(defaultStreamMockAnswer()); // and repeat

        AsyncWriter writer = client.createAsyncWriter(WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.idempotentRetryForever())
                .build());

        CompletableFuture<InitResult> initFuture = writer.init();
        CompletableFuture<WriteAck> m1Future = writer.send(Message.of("test-message".getBytes()));
        CompletableFuture<WriteAck> m2Future = writer.send(Message.of("test-message2".getBytes()));

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(30);

        Assert.assertTrue(initFuture.isDone());
        Assert.assertEquals(30, initFuture.join().getSeqNo());

        stream1.nextMsg().isWrite().hasWrite(2, 31, 32); // m1 & m2

        Assert.assertFalse(m1Future.isDone());
        Assert.assertFalse(m2Future.isDone());

        stream1.responseWriteWritten(31, 1);

        Assert.assertTrue(m1Future.isDone());
        Assert.assertEquals(31, m1Future.join().getSeqNo());
        Assert.assertFalse(m2Future.isDone());

        stream1.complete(Status.of(StatusCode.ABORTED));

        // Retry #1 - ABORTED
        getScheduler().hasTasks(1).executeNextTasks(1);
        // Retry #2 - OVERLOADED
        getScheduler().hasTasks(1).executeNextTasks(1);
        // Retry #3 - TRANSPORT_UNAVAILABLE
        getScheduler().hasTasks(1).executeNextTasks(1);
        // Retry #4 - OVERLOADED
        getScheduler().hasTasks(1).executeNextTasks(1);

        MockedWriteStream stream2 = currentStream();
        stream2.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream2.hasNoNewMessages();
        stream2.responseInit(31);

        stream2.nextMsg().isWrite().hasWrite(2, 32); // m2

        CompletableFuture<WriteAck> m3Future = writer.send(Message.of("other-message".getBytes()));

        stream2.nextMsg().isWrite().hasWrite(2, 33);
        stream2.responseWriteWritten(32, 2);

        Assert.assertTrue(m2Future.isDone());
        Assert.assertTrue(m3Future.isDone());
        Assert.assertEquals(32, m2Future.join().getSeqNo());
        Assert.assertEquals(33, m3Future.join().getSeqNo());

        writer.shutdown().join();
        stream2.complete(Status.SUCCESS);
    }

    @Test
    public void asyncDisabledRetryStreamCloseTest() throws QueueOverflowException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.noRetries())
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);
        CompletableFuture<InitResult> initFuture = writer.init();
        CompletableFuture<WriteAck> messageFuture = writer.send(Message.of("test".getBytes()));

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();

        Assert.assertFalse(initFuture.isDone());
        Assert.assertFalse(messageFuture.isDone());

        // Even successful completing closes writer
        stream1.complete(Status.SUCCESS);

        Assert.assertTrue(initFuture.isCompletedExceptionally());
        Assert.assertTrue(messageFuture.isCompletedExceptionally());

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> writer.send(Message.of("test-message".getBytes())));
        Assert.assertEquals("Writer is already stopped", ex.getMessage());

        writer.shutdown().join();
    }

    @Test
    public void asyncDisabledRetryStreamErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.noRetries())
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(0);

        stream1.responseErrorBadRequest();
        stream1.complete(Status.SUCCESS);

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> writer.send(Message.of("test-message".getBytes())));
        Assert.assertEquals("Writer is already stopped", ex.getMessage());

        writer.shutdown(1, TimeUnit.SECONDS);
    }

    @Test
    public void asyncDisabledRetryNetworkErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE));

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryConfig(RetryConfig.noRetries())
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();
//        writer.

        // No stream and no retries in scheduler
        Assert.assertNull(currentStream());
        getScheduler().hasNoTasks();

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> writer.send(Message.of("test-message".getBytes())));
        Assert.assertEquals("Writer is already stopped", ex.getMessage());

        writer.shutdown(1, TimeUnit.SECONDS);
    }
}
