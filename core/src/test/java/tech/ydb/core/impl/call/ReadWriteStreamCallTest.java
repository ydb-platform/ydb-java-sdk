package tech.ydb.core.impl.call;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.StringValue;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.impl.auth.AuthCallOptions;

public class ReadWriteStreamCallTest {
    private static final GrpcStatusHandler NOOP_HANDLER = new GrpcStatusHandler() {
        @Override
        public void accept(io.grpc.Status status, Metadata trailers) {
        }

        @Override
        public void postComplete() {
        }
    };

    @Test
    public void pendingMessagesAreSentWhenReadyTest() {
        FakeCall call = new FakeCall();
        ReadWriteStreamCall<StringValue, StringValue> stream = createStream(call);
        stream.start(msg -> {
        });

        stream.sendNext(StringValue.of("m1"));
        stream.sendNext(StringValue.of("m2"));
        Assert.assertEquals(Collections.emptyList(), call.sent);

        call.isReady = true;
        stream.onReady();
        Assert.assertEquals(Arrays.asList("m1", "m2"), call.sent);

        stream.sendNext(StringValue.of("m3"));
        Assert.assertEquals(Arrays.asList("m1", "m2", "m3"), call.sent);
    }

    @Test
    public void closeDropsPendingMessagesTest() {
        FakeCall call = new FakeCall();
        ReadWriteStreamCall<StringValue, StringValue> stream = createStream(call);
        stream.start(msg -> {
        });

        stream.sendNext(StringValue.of("m1"));
        stream.sendNext(StringValue.of("m2"));
        stream.close();
        Assert.assertTrue(call.isHalfClosed);

        call.isReady = true;
        stream.onReady();
        stream.sendNext(StringValue.of("m3"));
        Assert.assertEquals(Collections.emptyList(), call.sent);
    }

    @Test
    public void cancelDropsPendingMessagesTest() {
        FakeCall call = new FakeCall();
        ReadWriteStreamCall<StringValue, StringValue> stream = createStream(call);
        stream.start(msg -> {
        });

        stream.sendNext(StringValue.of("m1"));
        stream.cancel();
        Assert.assertTrue(call.isCancelled);

        call.isReady = true;
        stream.onReady();
        stream.sendNext(StringValue.of("m2"));
        Assert.assertEquals(Collections.emptyList(), call.sent);
    }

    @Test
    public void serverCloseDropsPendingMessagesTest() {
        FakeCall call = new FakeCall();
        ReadWriteStreamCall<StringValue, StringValue> stream = createStream(call);
        CompletableFuture<Status> status = stream.start(msg -> {
        });

        stream.sendNext(StringValue.of("m1"));
        stream.onClose(io.grpc.Status.UNAVAILABLE, null);
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, status.join().getCode());

        call.isReady = true;
        stream.onReady();
        stream.sendNext(StringValue.of("m2"));
        Assert.assertEquals(Collections.emptyList(), call.sent);
    }

    private static ReadWriteStreamCall<StringValue, StringValue> createStream(FakeCall call) {
        return new ReadWriteStreamCall<>(
                "test",
                "localhost:2135",
                call,
                GrpcFlows.SIMPLE_FLOW,
                new Metadata(),
                new AuthCallOptions(),
                NOOP_HANDLER
        );
    }

    /**
     * Unlike the real gRPC call, this one stays ready and accepts messages after halfClose or cancel, so every
     * message that is still queued by the stream at that moment would be sent by the next flush
     */
    private static class FakeCall extends ClientCall<StringValue, StringValue> {
        private final List<String> sent = new ArrayList<>();
        private volatile boolean isReady = false;
        private volatile boolean isHalfClosed = false;
        private volatile boolean isCancelled = false;

        @Override
        public void start(Listener<StringValue> listener, Metadata headers) {
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
            isCancelled = true;
        }

        @Override
        public void halfClose() {
            isHalfClosed = true;
        }

        @Override
        public void sendMessage(StringValue message) {
            sent.add(message.getValue());
        }

        @Override
        public boolean isReady() {
            return isReady;
        }
    }
}
