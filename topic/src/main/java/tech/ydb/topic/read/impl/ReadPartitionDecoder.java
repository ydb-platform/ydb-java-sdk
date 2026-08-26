package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.MessageCommitter;
import tech.ydb.topic.read.PartitionSession;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReadPartitionDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    private final String traceID;
    private final PartitionSession partition;
    private final MessageCommitter committer;
    private final MessageDecoder decoder;
    private final Runnable readyHandler;

    private final AtomicLong allocatedTotal = new AtomicLong(0);
    private final Queue<EncodedMessage> allocatedMessages = new ConcurrentLinkedQueue<>();
    private volatile boolean isStopped = false;

    public ReadPartitionDecoder(String traceId, MessageDecoder decoder, PartitionSession partition,
            MessageCommitter committer, Runnable readyHandler) {
        this.traceID = traceId;
        this.decoder = decoder;
        this.partition = partition;
        this.committer = committer;
        this.readyHandler = readyHandler;
    }

    public MessageImpl decode(BatchMeta m, OffsetsRange r, YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
        EncodedMessage encoded = new EncodedMessage(m, r, msg);
        decoder.add(encoded);
        return encoded;
    }

    public void releaseRange(OffsetsRange range) {
        long released = 0;
        Iterator<EncodedMessage> it = allocatedMessages.iterator();
        while (it.hasNext()) {
            EncodedMessage msg = it.next();
            if (msg.getOffset() < range.getStart()) {
                continue;
            }

            if (msg.getOffset() < range.getEnd()) {
                released += msg.uncompressedSize;
                it.remove();
            } else {
                break;
            }
        }

        while (true) {
            long curr = allocatedTotal.get();
            long free = Math.min(released, curr);
            if (free <= 0) {
                return;
            }
            if (allocatedTotal.compareAndSet(curr, curr - free)) {
                decoder.free(free);
                return;
            }
        }
    }

    public void close() {
        isStopped = true;
        release();
    }

    private void release() {
        decoder.free(allocatedTotal.getAndSet(0));
    }

    public class EncodedMessage extends MessageImpl  {
        private final int codecCode;
        private final long uncompressedSize;
        private ByteString origin;
        private byte[] data = null;
        private IOException problem = null;

        private volatile boolean isReady = false;

        EncodedMessage(BatchMeta meta, OffsetsRange range, YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
            super(partition, committer, meta, range, msg);
            this.origin = msg.getData();
            this.codecCode = meta.getCodec();
            this.uncompressedSize = msg.getUncompressedSize() > 0 ? msg.getUncompressedSize() : 2L * origin.size();
        }

        @Override
        public byte[] getData() {
            if (problem != null) {
                throw new DecompressionException("Error occurred while decoding a message", problem,
                        origin.toByteArray(), codecCode);
            }
            return data;
        }

        @Override
        public boolean isReady() {
            return isReady;
        }

        public void setError(Throwable th) {
            problem = new IOException("Decompression for " + getPartitionSession() + " error", th);
            releaseRange(OffsetsRange.of(getOffset()));
            isReady = true;
            readyHandler.run();
        }

        public long allocate() {
            if (isStopped) {
                problem = new IOException("" + getPartitionSession() + " is already closed");
                isReady = true;
                return 0;
            }

            allocatedTotal.addAndGet(uncompressedSize);
            allocatedMessages.add(this);
            return uncompressedSize;
        }

        public void decode(CodecRegistry registry) {
            if (isStopped) {
                release();
                return;
            }

            try {
                Codec codec = registry.getCodec(codecCode);
                if (codec == null) {
                    logger.warn("[{}] Cannot decode a message because codec {} is not registered", traceID, codecCode);
                    problem = new IOException("Codec " + codecCode + " is not registered");
                    return;
                }

                logger.trace("[{}] Started decoding batch", traceID);
                try (InputStream encoded = origin.newInput(); InputStream decoded = codec.decode(encoded)) {
                    data = ByteStreams.toByteArray(decoded);
                    logger.trace("[{}] Finished decoding batch", traceID);
                } catch (IOException ex) {
                    logger.warn("[{}] Exception was thrown while decoding a message: ", traceID, ex);
                    problem = ex;
                } catch (RuntimeException ex) {
                    logger.warn("[{}] RuntimeException was thrown while decoding a message: ", traceID, ex);
                    problem = new IOException("Cannot decode message", ex);
                }
            } finally {
                if (problem == null) {
                    origin = null;
                } else {
                    data = null;
                }
                isReady = true;
                readyHandler.run();
            }
        }
    }
}
