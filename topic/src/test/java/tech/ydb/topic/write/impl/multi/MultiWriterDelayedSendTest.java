package tech.ydb.topic.write.impl.multi;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.WriteAck;

/** A public send call may return only after the coordinator has exhausted its close deadline. */
public class MultiWriterDelayedSendTest {
    @Test
    public void delayedSendStillClosesLowerWriterAfterCoordinatorDisposal() throws Exception {
        MultiWriterSettings settings = MultiWriterTestSupport.settings()
                .setCloseTimeout(Duration.ofMillis(100)).setIoThreads(1).build();
        MultiWriterRuntime runtime = new MultiWriterRuntime(settings);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<DelayedWriter> current = new AtomicReference<>();
        MultiWriterImpl writer = new MultiWriterImpl(settings, new CodecRegistry().getCodec(settings.getCodec()),
                config -> {
                    DelayedWriter lower = new DelayedWriter(entered, release);
                    current.set(lower);
                    return lower;
                }, () -> CompletableFuture.completedFuture(Collections.singletonList(
                        MultiWriterTestSupport.partition(0))), runtime);
        try {
            writer.init().get(5, TimeUnit.SECONDS);
            CompletableFuture<MultiWriteResult> message = writer.send(MultiMessage.of(new byte[] {1}, "key"));
            Assert.assertTrue("Lower send did not start", entered.await(5, TimeUnit.SECONDS));
            DelayedWriter lower = current.get();
            CompletableFuture<Void> shutdown = writer.shutdown();
            ExecutionException timeout = Assert.assertThrows(ExecutionException.class,
                    () -> shutdown.get(5, TimeUnit.SECONDS));
            Assert.assertTrue("Close must report its deadline", timeout.getCause() instanceof TimeoutException);
            Assert.assertThrows(ExecutionException.class, () -> message.get(5, TimeUnit.SECONDS));
            Assert.assertTrue("Coordinator must be disposed before the held send returns",
                    ((ExecutorService) runtime.coordinator).awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertEquals("Lower shutdown cannot run while send is held", 1, lower.closed.getCount());

            // The failed close promise is observed before releasing send. Its finally event now has no coordinator.
            release.countDown();
            Assert.assertTrue("Late send return must still call lower shutdown",
                    lower.closed.await(5, TimeUnit.SECONDS));
            assertTerminated(runtime);
        } finally {
            release.countDown();
            runtime.shutdown();
            ((ExecutorService) runtime.topicCallExecutor).shutdownNow();
            ((ExecutorService) runtime.coordinator).shutdownNow();
            ((ExecutorService) runtime.completion).shutdownNow();
            ((ExecutorService) runtime.controlCompletion).shutdownNow();
            runtime.scheduler.shutdownNow();
            assertTerminated(runtime);
        }
    }

    @Test
    public void acceptedSendCleanupStillClosesAfterQueuedTimeoutDisposesRuntime() throws Exception {
        MultiWriterSettings settings = MultiWriterTestSupport.settings()
                .setCloseTimeout(Duration.ofMillis(100)).setIoThreads(1).build();
        MultiWriterRuntime runtime = new MultiWriterRuntime(settings);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        CountDownLatch coordinatorHeld = new CountDownLatch(1);
        CountDownLatch releaseCoordinator = new CountDownLatch(1);
        AtomicReference<DelayedWriter> current = new AtomicReference<>();
        MultiWriterImpl writer = new MultiWriterImpl(settings, new CodecRegistry().getCodec(settings.getCodec()),
                config -> {
                    DelayedWriter lower = new DelayedWriter(entered, releaseSend);
                    current.set(lower);
                    return lower;
                }, () -> CompletableFuture.completedFuture(Collections.singletonList(
                        MultiWriterTestSupport.partition(0))), runtime);
        try {
            writer.init().get(5, TimeUnit.SECONDS);
            writer.send(MultiMessage.of(new byte[] {1}, "key"));
            Assert.assertTrue("Lower send did not start", entered.await(5, TimeUnit.SECONDS));
            DelayedWriter lower = current.get();
            CompletableFuture<Void> shutdown = writer.shutdown();
            // This task runs after the shutdown command, fixing closeStarted before the deadline is tested.
            runtime.coordinator.execute(() -> {
                coordinatorHeld.countDown();
                try {
                    Assert.assertTrue("Test did not release the coordinator",
                            releaseCoordinator.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Coordinator hold was interrupted", error);
                }
            });
            Assert.assertTrue("Coordinator was not held", coordinatorHeld.await(5, TimeUnit.SECONDS));
            // The single scheduler enqueues its earlier periodic tick before this marker completes.
            runtime.scheduler.schedule(() -> { }, 200, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS);
            Assert.assertFalse("Held coordinator must not process the timeout yet", shutdown.isDone());
            CompletableFuture<Void> sendReturned = CompletableFuture.runAsync(() -> { }, runtime.topicCallExecutor);
            releaseSend.countDown();
            // The sole topic call worker executes this sentinel after send's finally has posted its cleanup event.
            sendReturned.get(5, TimeUnit.SECONDS);
            Assert.assertFalse("Cleanup was accepted while the coordinator was alive", shutdown.isDone());
            Assert.assertEquals(1, lower.closed.getCount());
            releaseCoordinator.countDown();

            ExecutionException timeout = Assert.assertThrows(ExecutionException.class,
                    () -> shutdown.get(5, TimeUnit.SECONDS));
            Assert.assertTrue(timeout.getCause() instanceof TimeoutException);
            Assert.assertTrue("Coordinator must process its already accepted cleanup before terminating",
                    ((ExecutorService) runtime.coordinator).awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertTrue("Accepted late cleanup must close lower writer after topic call executor shutdown",
                    lower.closed.await(5, TimeUnit.SECONDS));
            assertTerminated(runtime);
        } finally {
            releaseSend.countDown();
            releaseCoordinator.countDown();
            runtime.shutdown();
            ((ExecutorService) runtime.topicCallExecutor).shutdownNow();
            ((ExecutorService) runtime.coordinator).shutdownNow();
            ((ExecutorService) runtime.completion).shutdownNow();
            ((ExecutorService) runtime.controlCompletion).shutdownNow();
            runtime.scheduler.shutdownNow();
            assertTerminated(runtime);
        }
    }

    @Test
    public void queuedFlushMustResolveWhileLateLowerShutdownIsBlocked() throws Exception {
        MultiWriterSettings settings = MultiWriterTestSupport.settings()
                .setCloseTimeout(Duration.ofMillis(100)).setIoThreads(1).build();
        MultiWriterRuntime runtime = new MultiWriterRuntime(settings);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        CountDownLatch coordinatorHeld = new CountDownLatch(1);
        CountDownLatch releaseCoordinator = new CountDownLatch(1);
        CountDownLatch shutdownEntered = new CountDownLatch(1);
        CountDownLatch releaseShutdown = new CountDownLatch(1);
        MultiWriterImpl writer = new MultiWriterImpl(settings, new CodecRegistry().getCodec(settings.getCodec()),
                config -> new DelayedWriter(entered, releaseSend, shutdownEntered, releaseShutdown),
                () -> CompletableFuture.completedFuture(Collections.singletonList(
                        MultiWriterTestSupport.partition(0))), runtime);
        try {
            writer.init().get(5, TimeUnit.SECONDS);
            writer.send(MultiMessage.of(new byte[] {1}, "key"));
            Assert.assertTrue("Lower send did not start", entered.await(5, TimeUnit.SECONDS));
            CompletableFuture<Void> shutdown = writer.shutdown();
            runtime.coordinator.execute(() -> {
                coordinatorHeld.countDown();
                try {
                    Assert.assertTrue("Test did not release the coordinator",
                            releaseCoordinator.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Coordinator hold was interrupted", error);
                }
            });
            Assert.assertTrue("Coordinator was not held", coordinatorHeld.await(5, TimeUnit.SECONDS));
            runtime.scheduler.schedule(() -> { }, 200, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS);
            CompletableFuture<Void> sendReturned = CompletableFuture.runAsync(() -> { }, runtime.topicCallExecutor);
            releaseSend.countDown();
            sendReturned.get(5, TimeUnit.SECONDS);
            // Queue order is now timeout tick, accepted send cleanup, then this accepted flush command.
            CompletableFuture<Void> flush = writer.flush();
            Assert.assertFalse("Flush was accepted before disposal", flush.isDone());
            releaseCoordinator.countDown();
            ExecutionException timeout = Assert.assertThrows(ExecutionException.class,
                    () -> shutdown.get(5, TimeUnit.SECONDS));
            Assert.assertTrue(timeout.getCause() instanceof TimeoutException);
            Assert.assertTrue("Late lower shutdown was not entered", shutdownEntered.await(5, TimeUnit.SECONDS));
            Assert.assertEquals("Lower shutdown is still held", 1, releaseShutdown.getCount());
            // A physical cleanup lock must not keep an already accepted logical barrier unresolved.
            ExecutionException flushFailure = Assert.assertThrows(ExecutionException.class,
                    () -> flush.get(1, TimeUnit.SECONDS));
            Assert.assertTrue(flushFailure.getCause() instanceof TimeoutException);
        } finally {
            releaseSend.countDown();
            releaseCoordinator.countDown();
            releaseShutdown.countDown();
            runtime.shutdown();
            ((ExecutorService) runtime.topicCallExecutor).shutdownNow();
            ((ExecutorService) runtime.coordinator).shutdownNow();
            ((ExecutorService) runtime.completion).shutdownNow();
            ((ExecutorService) runtime.controlCompletion).shutdownNow();
            runtime.scheduler.shutdownNow();
            assertTerminated(runtime);
        }
    }

    private static void assertTerminated(MultiWriterRuntime runtime) throws InterruptedException {
        Assert.assertTrue("Topic call executor retained a task", ((ExecutorService) runtime.topicCallExecutor)
                .awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertTrue("Coordinator did not terminate", ((ExecutorService) runtime.coordinator)
                .awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertTrue("Message completion pool did not terminate", ((ExecutorService) runtime.completion)
                .awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertTrue("Control completion pool did not terminate", ((ExecutorService) runtime.controlCompletion)
                .awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertTrue("Scheduler did not terminate", runtime.scheduler.awaitTermination(5, TimeUnit.SECONDS));
    }

    private static final class DelayedWriter implements AsyncWriter {
        private final CountDownLatch entered;
        private final CountDownLatch release;
        private final CountDownLatch closed = new CountDownLatch(1);
        private final CountDownLatch shutdownEntered;
        private final CountDownLatch releaseShutdown;
        private volatile boolean sent;
        private final CompletableFuture<WriteAck> acknowledgement = new CompletableFuture<>();

        private DelayedWriter(CountDownLatch entered, CountDownLatch release) {
            this(entered, release, null, null);
        }

        private DelayedWriter(CountDownLatch entered, CountDownLatch release,
                CountDownLatch shutdownEntered, CountDownLatch releaseShutdown) {
            this.entered = entered;
            this.release = release;
            this.shutdownEntered = shutdownEntered;
            this.releaseShutdown = releaseShutdown;
        }

        @Override
        public CompletableFuture<InitResult> init() {
            return CompletableFuture.completedFuture(new InitResult(0));
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message) {
            return send(message, null);
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message, SendSettings settings) {
            sent = true;
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Test did not release the delayed send");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Delayed send was interrupted", error);
            }
            return acknowledgement;
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            if (sent && shutdownEntered != null) {
                shutdownEntered.countDown();
                try {
                    if (!releaseShutdown.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Test did not release lower shutdown");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Lower shutdown was interrupted", error);
                }
            }
            acknowledgement.completeExceptionally(new IllegalStateException("Lower writer stopped"));
            closed.countDown();
            return CompletableFuture.completedFuture(null);
        }
    }
}
