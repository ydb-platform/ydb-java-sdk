package tech.ydb.topic.read.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import org.junit.Test;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.DecompressionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MessageDecoderTest {
    @Test
    public void submitsAllTasksAllowedByBuffer() {
        Queue<Runnable> executorQueue = new ConcurrentLinkedQueue<>();
        MessageDecoder decoder = new MessageDecoder(100, executorQueue::add, new CodecRegistry());
        TestBatch[] batches = new TestBatch[10];

        for (int i = 0; i < batches.length; i++) {
            batches[i] = new TestBatch(1);
            decoder.decode("test", batches[i].batch, batches[i].readyCalls::incrementAndGet);
        }

        assertEquals(10, executorQueue.size());
    }

    @Test
    public void retainsBudgetUntilBatchIsConsumed() {
        Queue<Runnable> executorQueue = new ConcurrentLinkedQueue<>();
        MessageDecoder decoder = new MessageDecoder(1, executorQueue::add, new CodecRegistry());
        TestBatch first = new TestBatch(1);
        TestBatch second = new TestBatch(1);

        decoder.decode("test", first.batch, first.readyCalls::incrementAndGet);
        decoder.decode("test", second.batch, second.readyCalls::incrementAndGet);

        assertEquals(1, executorQueue.size());
        executorQueue.poll().run();
        assertTrue(executorQueue.isEmpty());

        first.readFuture.complete(null);
        assertEquals(1, executorQueue.size());
    }

    @Test
    public void rejectionDoesNotWedgeBatch() {
        MessageDecoder decoder = new MessageDecoder(10, command -> {
            throw new RejectedExecutionException("closed");
        }, new CodecRegistry());
        TestBatch batch = new TestBatch(1);

        decoder.decode("test", batch.batch, batch.readyCalls::incrementAndGet);

        assertEquals(1, batch.readyCalls.get());
        assertTrue(batch.batch.isReady());
        assertThrows(DecompressionException.class, batch.message::getData);
    }

    @Test
    public void unexpectedExecutorFailureDoesNotWedgeBatch() {
        MessageDecoder decoder = new MessageDecoder(10, command -> {
            throw new IllegalStateException("broken executor");
        }, new CodecRegistry());
        TestBatch batch = new TestBatch(1);

        decoder.decode("test", batch.batch, batch.readyCalls::incrementAndGet);

        assertEquals(1, batch.readyCalls.get());
        assertTrue(batch.batch.isReady());
        DecompressionException exception = assertThrows(DecompressionException.class, batch.message::getData);
        assertTrue(exception.getCause().getCause() instanceof IllegalStateException);
    }

    @Test
    public void fatalCodecErrorIsNotSwallowed() {
        Codec fatalCodec = new Codec() {
            @Override
            public int getId() {
                return 10_001;
            }

            @Override
            public InputStream decode(InputStream input) {
                throw new AssertionError("fatal codec failure");
            }

            @Override
            public OutputStream encode(OutputStream output) {
                return output;
            }
        };
        MessageDecoder decoder = new MessageDecoder(
                10,
                Runnable::run,
                new CodecRegistry(Collections.singletonList(fatalCodec))
        );
        TestBatch batch = new TestBatch(1, fatalCodec.getId());

        assertThrows(AssertionError.class,
                () -> decoder.decode("test", batch.batch, batch.readyCalls::incrementAndGet));
        assertEquals(1, batch.readyCalls.get());
        assertTrue(batch.batch.isReady());
    }

    private static class TestBatch {
        private final Batch batch;
        private final MessageImpl message;
        private final CompletableFuture<Void> readFuture = new CompletableFuture<>();
        private final AtomicInteger readyCalls = new AtomicInteger();

        TestBatch(long size) {
            this(size, Codec.RAW);
        }

        TestBatch(long size, int codec) {
            YdbTopic.StreamReadMessage.ReadResponse.Batch protoBatch = YdbTopic.StreamReadMessage.ReadResponse.Batch
                    .newBuilder()
                    .setCodec(codec)
                    .build();
            BatchMeta meta = new BatchMeta(protoBatch);
            YdbTopic.StreamReadMessage.ReadResponse.MessageData protoMessage = YdbTopic.StreamReadMessage.ReadResponse
                    .MessageData.newBuilder()
                    .setData(ByteString.copyFrom(new byte[]{1}))
                    .setUncompressedSize(size)
                    .build();
            message = new MessageImpl(null, null, meta, new OffsetsRangeImpl(0, 1), protoMessage);
            batch = new Batch(meta, Collections.singletonList(message)) {
                @Override
                public CompletableFuture<Void> getReadFuture() {
                    return readFuture;
                }
            };
        }
    }
}
