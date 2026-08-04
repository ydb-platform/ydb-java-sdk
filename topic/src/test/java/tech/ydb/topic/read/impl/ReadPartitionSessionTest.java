package tech.ydb.topic.read.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ReadPartitionSessionTest {
    /**
     * A batch queued while {@code sendDataToReadersIfNeeded} holds the reading flag must still be delivered. If the
     * method releases the flag without re-checking the queue, the notification of the queueing thread is lost, the
     * batch stays unread forever and its read future never completes - which stops the read session from ever
     * requesting more data.
     */
    @Test(timeout = 30_000)
    public void batchQueuedWhileReadingFlagIsHeldIsNotLost() throws Exception {
        ReadSession session = mock(ReadSession.class);
        Mockito.when(session.getMaxBatchSize()).thenReturn(0);
        Mockito.when(session.getMessageDecoder())
                .thenReturn(new MessageDecoder(1024, Runnable::run, new CodecRegistry()));

        PartitionSession partition = new PartitionSession(1, 1, "/topic");
        List<Long> delivered = Collections.synchronizedList(new ArrayList<>());

        ReadPartitionSession reader = new ReadPartitionSession("test", session, partition, 0) {
            @Override
            CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
                event.getMessages().forEach(message -> delivered.add(message.getOffset()));
                return completedFuture(null);
            }
        };

        injectQueue(reader, new EmptyOnceQueue());

        CompletableFuture<Void> batchRead = reader.addBatches(singletonList(rawProtoBatch(1)));

        batchRead.get(10, TimeUnit.SECONDS);
        assertEquals(singletonList(1L), delivered);
    }

    private static YdbTopic.StreamReadMessage.ReadResponse.Batch rawProtoBatch(long offset) {
        return YdbTopic.StreamReadMessage.ReadResponse.Batch.newBuilder()
                // RAW batches need no decoding, so they are ready as soon as they are queued
                .setCodec(Codec.RAW)
                .setProducerId("producer")
                .addMessageData(YdbTopic.StreamReadMessage.ReadResponse.MessageData.newBuilder()
                        .setOffset(offset)
                        .setSeqNo(offset)
                        .setData(ByteString.copyFromUtf8("data"))
                        .build()
                )
                .build();
    }

    private static void injectQueue(ReadPartitionSession session, Queue<Batch> queue) throws Exception {
        Field field = ReadPartitionSession.class.getDeclaredField("readingQueue");
        field.setAccessible(true);
        field.set(session, queue);
    }

    /**
     * Reading queue that reports itself as empty exactly once.
     *
     * <p>It reproduces the interleaving where {@code sendDataToReadersIfNeeded} looks at an empty queue while the gRPC
     * thread is adding an already decoded batch: the batch is offered and the notification of the gRPC thread is
     * swallowed by the losing {@code compareAndSet}, all before the reading flag is released.
     */
    private static class EmptyOnceQueue extends ConcurrentLinkedQueue<Batch> {
        private static final long serialVersionUID = 1L;

        private final AtomicBoolean reportEmpty = new AtomicBoolean(true);

        @Override
        public Batch peek() {
            if (reportEmpty.getAndSet(false)) {
                return null;
            }

            return super.peek();
        }
    }
}
