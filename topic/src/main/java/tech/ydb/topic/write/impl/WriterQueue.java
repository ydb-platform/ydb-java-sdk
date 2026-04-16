package tech.ydb.topic.write.impl;

import java.io.OutputStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriterQueue {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private final String debugId;
    private final BufferManager buffer;
    private final Codec codec;
    private final Executor compressionExecutor;
    private final Runnable readyNotify;

    // Messages that are taken into send buffer, are already compressed and are waiting for being sent
    private final Queue<EnqueuedMessage> queue = new ConcurrentLinkedQueue<>();
    // Messages that are currently trying to be sent and haven't received a response from server yet
    private final Deque<SentMessage> sent = new ConcurrentLinkedDeque<>();

    private final AtomicLong lastSeqNo = new AtomicLong(0);

    // Future for flush method
    private volatile EnqueuedMessage lastAcceptedMessage = null;

    public WriterQueue(String debugId, WriterSettings settings, CodecRegistry codecRegistry,
            Executor compressionExecutor, Runnable readyNotify) {
        this.debugId = debugId;
        this.buffer = new BufferManager(debugId, settings);

        this.codec = codecRegistry.getCodec(settings.getCodec());
        if (codec == null) {
            throw new IllegalArgumentException("Unsupported codec: " + settings.getCodec());
        }
        this.compressionExecutor = compressionExecutor;
        this.readyNotify = readyNotify;
    }

    CompletableFuture<Void> flush() {
        EnqueuedMessage local = lastAcceptedMessage;
        if (local == null) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> flushFuture = new CompletableFuture<>();
        // ackFuture can be failed, but flushFuture must be always successful
        local.getAckFuture().whenComplete((ack, th) -> flushFuture.complete(null));
        return flushFuture;
    }

    SentMessage nextMessageToSend() {
        Iterator<EnqueuedMessage> it = queue.iterator();
        while (it.hasNext()) {
            EnqueuedMessage next = it.next();

            if (next.hasProblem()) {
                it.remove();
                buffer.releaseMessage(next.getBufferSize());
                next.getAckFuture().completeExceptionally(next.getProblem());
                continue;
            }

            if (!next.isReady()) {
                break;
            }

            it.remove();

            // Calculate seqNo
            long seqNo = lastSeqNo.incrementAndGet();
            Long userSeqNo = next.getMeta().getUserSeqNo();
            if (userSeqNo != null) {
                if (userSeqNo < seqNo) {
                    buffer.releaseMessage(next.getBufferSize());
                    String error = "[" + debugId + "] Message wasn't sent because seqNo " + userSeqNo
                            + " is less than current seqNo " + seqNo;
                    logger.warn(error);
                    next.getAckFuture().completeExceptionally(new IllegalArgumentException(error));
                    continue;
                }
                seqNo = userSeqNo;
                lastSeqNo.set(seqNo);
            }

            SentMessage sentMsg = new SentMessage(next, seqNo);
            logger.debug("[{}] prepare sent message with seqNo {}", debugId, seqNo);
            sent.offer(sentMsg);
            return sentMsg;
        }
        return null;
    }

    void confirmAck(WriteAck ack) {
        Iterator<SentMessage> it = sent.iterator();
        while (it.hasNext()) {
            SentMessage msg = it.next();
            if (msg.getSeqNo() > ack.getSeqNo()) {
                return;
            }

            it.remove();
            buffer.releaseMessage(msg.getBufferSize());
            msg.getAckFuture().complete(ack);
        }
    }

    void close(Status status) {
        buffer.close(status);

        while (!queue.isEmpty()) {
            RuntimeException ex = new RuntimeException("Message sending was cancelled with status " + status);
            Iterator<EnqueuedMessage> it = queue.iterator();
            while (it.hasNext()) {
                EnqueuedMessage next = it.next();
                next.setError(ex);
                next.getAckFuture().completeExceptionally(ex);
                it.remove();
            }
        }

        while (!sent.isEmpty()) {
            RuntimeException ex = new RuntimeException("Message had been sent but the writer was stopped with status " +
                    status);
            Iterator<SentMessage> it = sent.iterator();
            while (it.hasNext()) {
                it.next().getAckFuture().completeExceptionally(ex);
                it.remove();
            }
        }
    }

    Iterator<SentMessage> updateSeqNo(long newSeqNo) {
        if (newSeqNo > lastSeqNo.get()) {
            lastSeqNo.set(newSeqNo);
        }

        // complete all messages with lost acks
        WriteAck lostAck = new WriteAck(newSeqNo, WriteAck.State.ALREADY_WRITTEN, null, null);
        Iterator<SentMessage> it = sent.iterator();
        while (it.hasNext()) {
            SentMessage msg = it.next();
            if (msg.getSeqNo() > newSeqNo) {
                break;
            }

            it.remove();
            buffer.releaseMessage(msg.getBufferSize());
            msg.getAckFuture().complete(lostAck);
        }

        return sent.iterator();
    }

    CompletableFuture<WriteAck> enqueue(Message message, YdbTransaction tx) throws QueueOverflowException,
            InterruptedException {
        long msgSize = Math.min(message.getData().length, buffer.getMaxSize());
        buffer.acquire(msgSize);
        return accept(message, tx, msgSize);
    }

    CompletableFuture<WriteAck> tryEnqueue(Message message, YdbTransaction tx) throws QueueOverflowException {
        long msgSize = Math.min(message.getData().length, buffer.getMaxSize());
        buffer.tryAcquire(msgSize);
        return accept(message, tx, msgSize);
    }

    CompletableFuture<WriteAck> tryEnqueue(Message message, YdbTransaction tx, long timeout, TimeUnit unit)
            throws QueueOverflowException, InterruptedException, TimeoutException {
        long msgSize = Math.min(message.getData().length, buffer.getMaxSize());
        buffer.tryAcquire(msgSize, timeout, unit);
        return accept(message, tx, msgSize);
    }


    private CompletableFuture<WriteAck> accept(Message message, YdbTransaction tx, long msgSize) {
        EnqueuedMessage msg = new EnqueuedMessage(new MessageMeta(message, tx), msgSize);
        lastAcceptedMessage = msg;
        queue.add(msg);

        if (codec.getId() == Codec.RAW) {
            // fast track without compression
            msg.setData(UnsafeByteOperations.unsafeWrap(message.getData()), msgSize);
            readyNotify.run();
            return msg.getAckFuture();
        }

        // encode message
        try {
            compressionExecutor.execute(() -> encode(message.getData(), msgSize, msg));
        } catch (Throwable ex) {
            logger.warn("[{}] Message wasn't sent because of processing error", debugId, ex);
            msg.setError(ex);
            readyNotify.run();
        }

        return msg.getAckFuture();
    }

    private void encode(byte[] data, long msgSize, EnqueuedMessage msg) {
        if (msg.hasProblem()) {
            return;
        }

        logger.trace("[{}] Started encoding message", debugId);
        try (ByteString.Output encoded = ByteString.newOutput()) {
            try (OutputStream os = codec.encode(encoded)) {
                os.write(data, 0, data.length);
            }

            logger.trace("[{}] Message compressed from {} to {} bytes", debugId, msgSize, encoded.size());

            long bufferSize = msgSize;
            if (msgSize > encoded.size()) { // if compressed lenght is less than uncompression - update buffer size
                bufferSize = encoded.size();
                buffer.updateMessageSize(msgSize, bufferSize);
            }

            msg.setData(encoded.toByteString(), bufferSize);
        } catch (Throwable ex) {
            logger.warn("[{}] Message wasn't sent because of encoding error", debugId, ex);
            msg.setError(ex);
        }
        readyNotify.run();
    }
}
