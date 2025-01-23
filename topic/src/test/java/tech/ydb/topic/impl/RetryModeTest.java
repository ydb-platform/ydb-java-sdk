package tech.ydb.topic.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.settings.RetryMode;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryModeTest extends BaseMockedTest {

    @Test
    public void alwaysRetryWriterTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(defaultStreamMockAnswer())
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(defaultStreamMockAnswer()); // and repeat

        SyncWriter writer = client.createSyncWriter(WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryMode(RetryMode.ALWAYS)
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

        stream1.complete(Status.SUCCESS);

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
        stream3.nextMsg().isWrite().hasWrite(2, 1);
        stream3.responseWriteWritten(2, 1);

        writer.flush();
        writer.shutdown(1, TimeUnit.SECONDS);
        stream3.complete(Status.SUCCESS);
    }

    @Test
    public void disabledRetryNetworkErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE));

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryMode(RetryMode.NONE)
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
    public void disabledRetryStreamCloseTest() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryMode(RetryMode.NONE)
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
    public void disabledRetryStreamErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryMode(RetryMode.NONE)
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
    public void recoverRetryNetworkErrorTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE));

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryMode(RetryMode.RECOVER)
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
    public void recoverRetryWriterTest() throws InterruptedException, ExecutionException, TimeoutException {
        mockStreams()
                .then(defaultStreamMockAnswer())
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(errorStreamMockAnswer(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(errorStreamMockAnswer(StatusCode.OVERLOADED))
                .then(defaultStreamMockAnswer()); // and repeat

        SyncWriter writer = client.createSyncWriter(WriterSettings.newBuilder()
                .setTopicPath("/mocked_topic")
                .setRetryMode(RetryMode.RECOVER)
                .build());
        writer.init();

        MockedWriteStream stream1 = currentStream();
        stream1.nextMsg().isInit().hasInitPath("/mocked_topic");
        stream1.hasNoNewMessages();
        stream1.responseInit(0);

        writer.send(Message.of("test-message".getBytes()));
        stream1.nextMsg().isWrite().hasWrite(2, 1);
        stream1.responseWriteWritten(1, 1);

        stream1.complete(new RuntimeException("io exception"));

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
        stream2.responseInit(1);

        writer.send(Message.of("other-message".getBytes()));
        stream2.nextMsg().isWrite().hasWrite(2, 1);
        stream2.responseWriteWritten(2, 1);

        writer.flush();
        writer.shutdown(1, TimeUnit.SECONDS);
        stream2.complete(Status.SUCCESS);
    }
}
