package tech.ydb.topic.write.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    interface EncodedMsg {
        SentMessage getSentMessage();
        long getBufferSize();
        void confirm(WriteAck ack);
        void close(RuntimeException ex);
    }
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private final String debugId;
    private final BufferManager buffer;
    private final Codec codec;
    private final Executor compressionExecutor;
    private final Runnable readyNotify;

    // Messages that are taken into send buffer, are already compressed and are waiting for being sent
    private final Queue<EnqueuedMessage> queue = new ConcurrentLinkedQueue<>();
    // Messages that are currently trying to be sent and haven't received a response from server yet
    private final Deque<EncodedMsg> sent = new ConcurrentLinkedDeque<>();

    private volatile long lastSeqNo = 0;

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

            Throwable problem = next.getProblem();
            if (problem != null) {
                it.remove();

                logger.warn("[{}] Message wasn't sent because encoding problem {}", debugId, problem);
                sent.offer(new ProblemMsg(next.getBufferSize(), problem, next.getAckFuture()));
                continue;
            }

            if (!next.isReady()) {
                break;
            }

            it.remove();

            // Calculate seqNo
            long actualSeqNo = lastSeqNo + 1;
            Long userSeqNo = next.getMeta().getUserSeqNo();
            if (userSeqNo != null) {
                if (userSeqNo < actualSeqNo) {
                    logger.warn("[{}] Message wasn't sent because seqNo {} is less than current seqNo {}", debugId,
                            userSeqNo, actualSeqNo);
                    WriteAck skipAck = new WriteAck(userSeqNo, WriteAck.State.ALREADY_WRITTEN, null, null);
                    sent.offer(new SkippedMsg(next.getBufferSize(), skipAck, next.getAckFuture()));
                    continue;
                }
                actualSeqNo = userSeqNo;
            }

            lastSeqNo = actualSeqNo;
            SentMessage sentMsg = new SentMessage(next, actualSeqNo);
            logger.trace("[{}] prepare sent message with seqNo {}", debugId, actualSeqNo);
            sent.offer(sentMsg);
            return sentMsg;
        }
        return null;
    }

    void confirmAck(WriteAck ack) {
        Iterator<EncodedMsg> sentIt = sent.iterator();
        while (sentIt.hasNext()) {
            EncodedMsg msg = sentIt.next();
            SentMessage sentMsg = msg.getSentMessage();
            if (sentMsg == null) { // error message which wasn't sent
                sentIt.remove();
                buffer.releaseMessage(msg.getBufferSize());
                msg.confirm(ack);
                continue;
            }

            if (ack == null || sentMsg.getSeqNo() > ack.getSeqNo()) {
                return;
            }

            sentIt.remove();
            buffer.releaseMessage(msg.getBufferSize());
            msg.confirm(ack);
        }
    }

    void close(Status status) {
        buffer.close(status);

        while (!queue.isEmpty()) {
            RuntimeException ex = new RuntimeException("Message sending was cancelled with " + status);
            Iterator<EnqueuedMessage> it = queue.iterator();
            while (it.hasNext()) {
                EnqueuedMessage next = it.next();
                next.close(ex);
                it.remove();
            }
        }

        while (!sent.isEmpty()) {
            RuntimeException ex = new RuntimeException("Message had been sent but the writer was stopped with " +
                    status);
            Iterator<EncodedMsg> it = sent.iterator();
            while (it.hasNext()) {
                it.next().close(ex);
                it.remove();
            }
        }
    }

    List<SentMessage> updateSeqNo(long newSeqNo) {
        if (newSeqNo > lastSeqNo) {
            lastSeqNo = newSeqNo;
        }

        // complete all messages with lost acks
        Iterator<EncodedMsg> it = sent.iterator();
        while (it.hasNext()) {
            EncodedMsg msg = it.next();
            SentMessage sentMsg = msg.getSentMessage();
            if (sentMsg != null && sentMsg.getSeqNo() > newSeqNo) {
                break;
            }

            it.remove();
            long seqNo = sentMsg != null ? sentMsg.getSeqNo() : newSeqNo;
            WriteAck lostAck = new WriteAck(seqNo, WriteAck.State.ALREADY_WRITTEN, null, null);
            buffer.releaseMessage(msg.getBufferSize());
            msg.confirm(lostAck);
        }

        List<SentMessage> resend = new ArrayList<>();
        for (EncodedMsg msg : sent) {
            if (msg.getSentMessage() != null) {
                resend.add(msg.getSentMessage());
            }
        }

        return resend;
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
            msg.completeWithData(UnsafeByteOperations.unsafeWrap(message.getData()), msgSize);
            readyNotify.run();
            return msg.getAckFuture();
        }

        // encode message
        try {
            compressionExecutor.execute(() -> encode(message.getData(), msgSize, msg));
        } catch (Throwable ex) {
            logger.warn("[{}] Message wasn't sent because of processing error", debugId, ex);
            msg.completeWithProblem(ex);
            readyNotify.run();
        }

        return msg.getAckFuture();
    }

    private void encode(byte[] data, long msgSize, EnqueuedMessage msg) {
        if (msg.isReady()) {
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

            msg.completeWithData(encoded.toByteString(), bufferSize);
        } catch (Throwable ex) {
            logger.warn("[{}] Message wasn't sent because of encoding error", debugId, ex);
            msg.completeWithProblem(ex);
        }
        readyNotify.run();
    }

    private class SkippedMsg implements EncodedMsg {
        private final long bufferSize;
        private final WriteAck ack;
        private final CompletableFuture<WriteAck> ackFuture;

        SkippedMsg(long bufferSize, WriteAck ack, CompletableFuture<WriteAck> ackFuture) {
            this.bufferSize = bufferSize;
            this.ack = ack;
            this.ackFuture = ackFuture;
        }

        @Override
        public SentMessage getSentMessage() {
            return null;
        }

        @Override
        public long getBufferSize() {
            return bufferSize;
        }

        @Override
        public void confirm(WriteAck ignored) {
            ackFuture.complete(ack);
        }

        @Override
        public void close(RuntimeException ex) {
            ackFuture.completeExceptionally(ex);
        }
    }

    private class ProblemMsg implements EncodedMsg {
        private final long bufferSize;
        private final Throwable problem;
        private final CompletableFuture<WriteAck> ackFuture;

        ProblemMsg(long bufferSize, Throwable problem, CompletableFuture<WriteAck> ackFuture) {
            this.bufferSize = bufferSize;
            this.problem = problem;
            this.ackFuture = ackFuture;
        }

        @Override
        public SentMessage getSentMessage() {
            return null;
        }

        @Override
        public long getBufferSize() {
            return bufferSize;
        }

        @Override
        public void confirm(WriteAck ignored) {
            ackFuture.completeExceptionally(problem);
        }

        @Override
        public void close(RuntimeException ex) {
            ackFuture.completeExceptionally(ex);
        }
    }
}
