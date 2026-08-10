package tech.ydb.topic.read.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.ReadResponse;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;

/**
 * Unit tests for {@link MessageDecoder} flow control and scheduling.
 */
public class MessageDecoderTest {
    private static final CodecRegistry REGISTRY = new CodecRegistry();
    private static final PartitionSession PS1 = new PartitionSession(1, 1, "/topic");
    private static final PartitionSession PS2 = new PartitionSession(2, 2, "/topic");

    private static void assertDecompressionException(String msg, ThrowingRunnable runnable) {
        DecompressionException ex = Assert.assertThrows(DecompressionException.class, runnable);
        Assert.assertEquals(msg, ex.getCause().getMessage());
    }

    private static byte[] gzip(byte[] data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(baos)) {
                gz.write(data);
            }
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static ReadResponse.MessageData rawMsg(long offset, long uncompressedSize, byte[] data) {
        return ReadResponse.MessageData.newBuilder()
                .setOffset(offset)
                .setUncompressedSize(uncompressedSize)
                .setData(ByteString.copyFrom(data))
                .build();
    }

    private static ReadResponse.MessageData gzipMsg(int offset, int uncompressedSize) {
        byte[] data = new byte[uncompressedSize];
        Arrays.fill(data, (byte) offset);
        return ReadResponse.MessageData.newBuilder()
                .setOffset(offset)
                .setUncompressedSize(uncompressedSize)
                .setData(ByteString.copyFrom(gzip(data)))
                .build();
    }

    private static BatchMeta meta(int codec) {
        return new BatchMeta(ReadResponse.Batch.newBuilder().setCodec(codec).build());
    }

    @Rule
    public final HideLoggersRule hideLoggerRule = new HideLoggersRule();

    @Test
    public void rawDecodeTest() {
        MessageDecoder decoder = new MessageDecoder(10000, Runnable::run, REGISTRY);
        AtomicInteger ready = new AtomicInteger(0);
        ReadPartitionDecoder partition = new ReadPartitionDecoder("t1", decoder, PS1, null, ready::incrementAndGet);

        BatchMeta meta = meta(Codec.RAW);
        MessageImpl m1 = partition.decode(meta, OffsetsRange.of(1), rawMsg(1, 100, new byte[] {1, 2}));
        MessageImpl m2 = partition.decode(meta, OffsetsRange.of(2), rawMsg(2, 100, new byte[] {3}));

        Assert.assertFalse(m1.isReady());
        Assert.assertFalse(m2.isReady());

        decoder.decodeNext();

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertArrayEquals(new byte[] {1, 2}, m1.getData());
        Assert.assertArrayEquals(new byte[] {3}, m2.getData());
        Assert.assertEquals(2, ready.get());
    }

    @Test
    public void flowControlByBudgetTest() {
        MessageDecoder decoder = new MessageDecoder(100, Runnable::run, REGISTRY);

        AtomicInteger ready = new AtomicInteger(0);
        ReadPartitionDecoder p1 = new ReadPartitionDecoder("p1", decoder, PS1, null, ready::incrementAndGet);

        BatchMeta meta = meta(Codec.GZIP);
        MessageImpl m1 = p1.decode(meta, OffsetsRange.of(1), gzipMsg(1, 40));
        MessageImpl m2 = p1.decode(meta, OffsetsRange.of(2), gzipMsg(2, 50));
        MessageImpl m3 = p1.decode(meta, OffsetsRange.of(3), gzipMsg(3, 60));
        MessageImpl m4 = p1.decode(meta, OffsetsRange.of(4), gzipMsg(4, 70));
        MessageImpl m5 = p1.decode(meta, OffsetsRange.of(5), gzipMsg(5, 120));

        Assert.assertFalse(m1.isReady());
        Assert.assertFalse(m2.isReady());
        Assert.assertFalse(m3.isReady());
        Assert.assertFalse(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertEquals(0, ready.get());

        decoder.decodeNext();

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertTrue(m3.isReady());
        Assert.assertFalse(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertEquals(3, ready.get());

        Assert.assertEquals(40, m1.getData().length);
        Assert.assertEquals(50, m2.getData().length);
        Assert.assertEquals(60, m3.getData().length);
        Assert.assertEquals(1, m1.getData()[0]);
        Assert.assertEquals(2, m2.getData()[0]);
        Assert.assertEquals(3, m3.getData()[0]);

        p1.releaseRange(OffsetsRange.of(1)); // 40 is not enough to resume decoding
        p1.releaseRange(OffsetsRange.of(4)); // that offset is not decodec yet
        Assert.assertFalse(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertEquals(3, ready.get());

        p1.releaseRange(OffsetsRange.of(2));
        Assert.assertTrue(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertEquals(4, ready.get());

        Assert.assertEquals(70, m4.getData().length);
        Assert.assertEquals(4, m4.getData()[0]);

        p1.releaseRange(OffsetsRange.of(0, 3)); // double release
        Assert.assertFalse(m5.isReady());
        Assert.assertEquals(4, ready.get());

        p1.releaseRange(OffsetsRange.of(0, 5));
        Assert.assertTrue(m5.isReady());
        Assert.assertEquals(5, ready.get());
    }

    @Test
    public void partitionFlowTest() {
        MessageDecoder decoder = new MessageDecoder(100, Runnable::run, REGISTRY);

        AtomicInteger r1 = new AtomicInteger(0);
        ReadPartitionDecoder p1 = new ReadPartitionDecoder("p1", decoder, PS1, null, r1::incrementAndGet);
        AtomicInteger r2 = new AtomicInteger(0);
        ReadPartitionDecoder p2 = new ReadPartitionDecoder("p2", decoder, PS2, null, r2::incrementAndGet);

        BatchMeta meta = meta(Codec.GZIP);
        MessageImpl m1 = p1.decode(meta, OffsetsRange.of(1), gzipMsg(1, 40));
        MessageImpl m2 = p1.decode(meta, OffsetsRange.of(2), gzipMsg(2, 50));
        MessageImpl m3 = p2.decode(meta, OffsetsRange.of(10), gzipMsg(1, 60));
        MessageImpl m4 = p2.decode(meta, OffsetsRange.of(11), gzipMsg(2, 40));
        MessageImpl m5 = p2.decode(meta, OffsetsRange.of(12), gzipMsg(3, 30));

        Assert.assertEquals(0, r1.get());
        Assert.assertEquals(0, r2.get());

        decoder.decodeNext();

        MessageImpl m6 = p1.decode(meta, OffsetsRange.of(4), gzipMsg(4, 10));
        MessageImpl m7 = p1.decode(meta, OffsetsRange.of(5), gzipMsg(5, 20));
        MessageImpl m8 = p2.decode(meta, OffsetsRange.of(14), gzipMsg(14, 10));
        MessageImpl m9 = p2.decode(meta, OffsetsRange.of(15), gzipMsg(15, 20));

        decoder.decodeNext();

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertTrue(m3.isReady());
        Assert.assertFalse(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertFalse(m6.isReady());
        Assert.assertFalse(m7.isReady());
        Assert.assertFalse(m8.isReady());
        Assert.assertFalse(m9.isReady());

        Assert.assertEquals(2, r1.get());
        Assert.assertEquals(1, r2.get());

        p1.close();

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertTrue(m3.isReady());
        Assert.assertTrue(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertFalse(m6.isReady());
        Assert.assertFalse(m7.isReady());
        Assert.assertFalse(m8.isReady());
        Assert.assertFalse(m9.isReady());

        Assert.assertEquals(2, r1.get());
        Assert.assertEquals(2, r2.get());

        p1.releaseRange(OffsetsRange.of(0, 100));
        p2.releaseRange(OffsetsRange.of(0, 100));

        Assert.assertTrue(m5.isReady());
        Assert.assertTrue(m6.isReady());
        Assert.assertTrue(m7.isReady());
        Assert.assertTrue(m8.isReady());
        Assert.assertTrue(m9.isReady());

        Assert.assertEquals(2, r1.get()); // p1 is already stopped
        Assert.assertEquals(5, r2.get());
}

    @Test
    public void decodeStopTest() {
        MessageDecoder decoder = new MessageDecoder(70, Runnable::run, REGISTRY);

        AtomicInteger ready = new AtomicInteger(0);
        ReadPartitionDecoder partition = new ReadPartitionDecoder("t3", decoder, PS1, null, ready::incrementAndGet);

        BatchMeta meta = meta(Codec.GZIP);
        MessageImpl m1 = partition.decode(meta, OffsetsRange.of(1), gzipMsg(1, 40));
        MessageImpl m2 = partition.decode(meta, OffsetsRange.of(2), gzipMsg(2, 50));
        MessageImpl m3 = partition.decode(meta, OffsetsRange.of(3), gzipMsg(3, 60));

        Assert.assertEquals(0, ready.get());

        decoder.decodeNext();
        Assert.assertEquals(2, ready.get());
        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertFalse(m3.isReady());

        decoder.stop();
        Assert.assertEquals(2, ready.get());
        Assert.assertFalse(m3.isReady());

        partition.releaseRange(OffsetsRange.of(0, 10));
        Assert.assertEquals(2, ready.get());
        Assert.assertFalse(m3.isReady());
    }

    @Test
    public void decodesOnProvidedExecutorTest() {
        Queue<Runnable> decodeTasks = new ConcurrentLinkedQueue<>();
        MessageDecoder decoder = new MessageDecoder(1000, decodeTasks::add, REGISTRY);

        AtomicInteger p1ready = new AtomicInteger();
        AtomicInteger p2ready = new AtomicInteger();
        ReadPartitionDecoder p1 = new ReadPartitionDecoder("p1", decoder, PS1, null, p1ready::incrementAndGet);
        ReadPartitionDecoder p2 = new ReadPartitionDecoder("p2", decoder, PS2, null, p2ready::incrementAndGet);

        BatchMeta meta = meta(Codec.GZIP);
        MessageImpl m1 = p1.decode(meta, OffsetsRange.of(1), gzipMsg(1, 400));
        MessageImpl m2 = p2.decode(meta, OffsetsRange.of(1), gzipMsg(1, 500));
        MessageImpl m3 = p2.decode(meta, OffsetsRange.of(2), gzipMsg(2, 600));
        MessageImpl m4 = p1.decode(meta, OffsetsRange.of(2), gzipMsg(2, 400));
        MessageImpl m5 = p1.decode(meta, OffsetsRange.of(3), gzipMsg(3, 400));

        Assert.assertFalse(m1.isReady());
        Assert.assertFalse(m2.isReady());
        Assert.assertFalse(m3.isReady());
        Assert.assertFalse(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertEquals(0, decodeTasks.size());
        Assert.assertEquals(0, p1ready.get());
        Assert.assertEquals(0, p2ready.get());

        decoder.decodeNext();

        Assert.assertFalse(m1.isReady());
        Assert.assertFalse(m2.isReady());
        Assert.assertEquals(3, decodeTasks.size());
        Assert.assertEquals(0, p1ready.get());
        Assert.assertEquals(0, p2ready.get());

        decodeTasks.poll().run();

        Assert.assertTrue(m1.isReady());
        Assert.assertFalse(m2.isReady());
        Assert.assertEquals(2, decodeTasks.size());
        Assert.assertEquals(1, p1ready.get());
        Assert.assertEquals(0, p2ready.get());

        decodeTasks.poll().run();

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertEquals(1, decodeTasks.size());
        Assert.assertEquals(1, p1ready.get());
        Assert.assertEquals(1, p2ready.get());

        decoder.stop();
        p1.close();
        p2.close();

        Assert.assertEquals(1, decodeTasks.size());
        decodeTasks.poll().run();
        Assert.assertEquals(0, decodeTasks.size());

        Assert.assertEquals(1, p1ready.get());
        Assert.assertEquals(1, p2ready.get());
    }

    @Test
    @HideLoggers(MessageDecoder.class)
    public void decodeProblemsTest() {
        MessageDecoder decoder = new MessageDecoder(200, Runnable::run, REGISTRY);

        AtomicInteger ready = new AtomicInteger();
        ReadPartitionDecoder p1 = new ReadPartitionDecoder("p1", decoder, PS1, null, ready::incrementAndGet);

        BatchMeta meta1 = meta(1244); // Uknown codec
        MessageImpl m1 = p1.decode(meta1, OffsetsRange.of(1), rawMsg(1, 50, new byte[0]));
        MessageImpl m2 = p1.decode(meta1, OffsetsRange.of(2), rawMsg(2, 50, new byte[0]));

        BatchMeta meta2 = meta(Codec.GZIP);
        MessageImpl m3 = p1.decode(meta2, OffsetsRange.of(3), rawMsg(3, 50, new byte[] { 0x1, 0x2 }));
        MessageImpl m4 = p1.decode(meta2, OffsetsRange.of(4), rawMsg(4, 50, new byte[] { 0x1, 0x2 }));

        MessageImpl m5 = p1.decode(meta2, OffsetsRange.of(5), gzipMsg(5, 50));
        MessageImpl m6 = p1.decode(meta2, OffsetsRange.of(6), gzipMsg(6, 50));

        decoder.decodeNext();

        Assert.assertEquals(4, ready.get());

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());
        Assert.assertTrue(m3.isReady());
        Assert.assertTrue(m4.isReady());
        Assert.assertFalse(m5.isReady());
        Assert.assertFalse(m6.isReady());

        assertDecompressionException("Codec 1244 is not registered", m1::getData);
        assertDecompressionException("Codec 1244 is not registered", m2::getData);
        assertDecompressionException("Not in GZIP format", m3::getData);
        assertDecompressionException("Not in GZIP format", m4::getData);

        p1.close();

        Assert.assertEquals(4, ready.get());
        Assert.assertTrue(m5.isReady());
        Assert.assertTrue(m6.isReady());
        assertDecompressionException("Partition session 1 (partition 1) for topic \"/topic\" is already closed",
                m5::getData);
        assertDecompressionException("Partition session 1 (partition 1) for topic \"/topic\" is already closed",
                m6::getData);
    }

    @Test
    @HideLoggers(MessageDecoder.class)
    public void wrongDecoderTest() {
        Executor decompressor = (Runnable command) -> {
            throw new RejectedExecutionException("rejected");
        };

        MessageDecoder decoder = new MessageDecoder(200, decompressor, REGISTRY);

        AtomicInteger ready = new AtomicInteger();
        ReadPartitionDecoder p1 = new ReadPartitionDecoder("p1", decoder, PS1, null, ready::incrementAndGet);

        BatchMeta meta = meta(Codec.GZIP);
        MessageImpl m1 = p1.decode(meta, OffsetsRange.of(5), gzipMsg(5, 50));
        MessageImpl m2 = p1.decode(meta, OffsetsRange.of(6), gzipMsg(6, 50));

        decoder.decodeNext();

        Assert.assertEquals(2, ready.get());

        Assert.assertTrue(m1.isReady());
        Assert.assertTrue(m2.isReady());

        assertDecompressionException("Decompression for Partition session 1 (partition 1) for topic \"/topic\" error",
                m1::getData);
        assertDecompressionException("Decompression for Partition session 1 (partition 1) for topic \"/topic\" error",
                m2::getData);
    }
}
