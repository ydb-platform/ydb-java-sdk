package tech.ydb.topic.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.topic.description.Codec;

public class ZstdCodecTest {
    private static final Codec CODEC = ZstdCodec.getInstance();

    @Test
    public void roundTripAndCompatibility() throws IOException {
        for (int size : new int[] {0, 1, 1024, 2 * 1024 * 1024}) {
            byte[] data = data(size, size);
            byte[] encoded = encode(data);
            ByteArrayOutputStream original = new ByteArrayOutputStream();
            try (OutputStream stream = new ZstdOutputStreamNoFinalizer(original)) {
                stream.write(data);
            }
            Assert.assertArrayEquals(original.toByteArray(), encoded);
            Assert.assertArrayEquals(data, decode(encoded));
            Assert.assertArrayEquals(data, read(new ZstdInputStreamNoFinalizer(new ByteArrayInputStream(encoded))));
            Assert.assertArrayEquals(data, read(ZstdBackwardCodec.getInstance()
                    .decode(new ByteArrayInputStream(encoded))));
        }
        Assert.assertArrayEquals(new byte[0], decode(new byte[0]));
    }

    @Test
    public void invalidAndTruncatedInput() throws IOException {
        byte[] encoded = encode(data(1024 * 1024, 1));
        for (byte[] invalid : new byte[][] {new byte[] {1, 2, 3, 4}, Arrays.copyOf(encoded, encoded.length - 1)}) {
            Assert.assertThrows(IOException.class, () -> decode(invalid));
            Assert.assertArrayEquals(new byte[] {42}, decode(encode(new byte[] {42})));
        }
    }

    @Test
    public void closesParentStreamsOnce() throws IOException {
        TrackingOutput output = new TrackingOutput(false, false);
        OutputStream encoder = CODEC.encode(output);
        encoder.write(new byte[] {42});
        encoder.close();
        encoder.close();
        Assert.assertEquals(1, output.closes);

        TrackingInput input = new TrackingInput(output.bytes.toByteArray(), false, false);
        InputStream decoder = CODEC.decode(input);
        Assert.assertEquals(42, decoder.read());
        Assert.assertEquals(-1, decoder.read());
        decoder.close();
        decoder.close();
        Assert.assertEquals(1, input.closes);
    }

    @Test
    public void closesAfterInputFailures() throws IOException {
        for (boolean failRead : new boolean[] {false, true}) {
            TrackingInput input = new TrackingInput(encode(new byte[] {42}), failRead, true);
            Assert.assertThrows(IOException.class, () -> read(CODEC.decode(input)));
            Assert.assertEquals(1, input.closes);
        }
    }

    @Test
    public void outputFailuresRemainVisible() throws IOException {
        for (boolean failWrite : new boolean[] {false, true}) {
            TrackingOutput output = new TrackingOutput(failWrite, !failWrite);
            OutputStream encoder = CODEC.encode(output);
            Assert.assertThrows(IOException.class, () -> {
                try (OutputStream stream = encoder) {
                    stream.write(data(1024 * 1024, 2));
                }
            });
            encoder.close();
            Assert.assertEquals(failWrite ? 0 : 1, output.closes);
            Assert.assertArrayEquals(new byte[] {42}, decode(encode(new byte[] {42})));
        }
    }

    @Test(timeout = 30000)
    public void concurrentStreams() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < 4; thread++) {
                final int seed = thread;
                futures.add(executor.submit(() -> {
                    start.await();
                    byte[] data = data(512 * 1024, seed);
                    for (int iteration = 0; iteration < 20; iteration++) {
                        Assert.assertArrayEquals(data, decode(encode(data)));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static byte[] data(int size, int seed) {
        byte[] data = new byte[size];
        new Random(seed).nextBytes(data);
        return data;
    }

    private static byte[] encode(byte[] data) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (OutputStream stream = CODEC.encode(output)) {
            stream.write(data);
        }
        return output.toByteArray();
    }

    private static byte[] decode(byte[] data) throws IOException {
        return read(CODEC.decode(new ByteArrayInputStream(data)));
    }

    private static byte[] read(InputStream input) throws IOException {
        try (InputStream stream = input) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static class TrackingInput extends FilterInputStream {
        private final boolean failRead;
        private final boolean failClose;
        private int closes;

        TrackingInput(byte[] data, boolean failRead, boolean failClose) {
            super(new ByteArrayInputStream(data));
            this.failRead = failRead;
            this.failClose = failClose;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (failRead) {
                throw new IOException("read failed");
            }
            return super.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            closes++;
            super.close();
            if (failClose) {
                throw new IOException("close failed");
            }
        }
    }

    private static class TrackingOutput extends OutputStream {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final boolean failWrite;
        private final boolean failClose;
        private int closes;

        TrackingOutput(boolean failWrite, boolean failClose) {
            this.failWrite = failWrite;
            this.failClose = failClose;
        }

        @Override
        public void write(int value) throws IOException {
            if (failWrite) {
                throw new IOException("write failed");
            }
            bytes.write(value);
        }

        @Override
        public void close() throws IOException {
            closes++;
            if (failClose) {
                throw new IOException("close failed");
            }
        }
    }
}
