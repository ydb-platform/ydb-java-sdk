package tech.ydb.core.impl.call;

import java.util.concurrent.CompletableFuture;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.tracing.Span;
import tech.ydb.proto.discovery.DiscoveryProtos.ListEndpointsRequest;
import tech.ydb.proto.discovery.DiscoveryProtos.ListEndpointsResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Invalid calls of start() must be rejected without affecting the stream
 */
public class StreamCallStartTest {
    private final GrpcStatusHandler statusHandler = mock(GrpcStatusHandler.class);

    @SuppressWarnings("unchecked")
    private static ClientCall<ListEndpointsRequest, ListEndpointsResponse> mockCall() {
        return mock(ClientCall.class);
    }

    private ReadStreamCall<ListEndpointsRequest, ListEndpointsResponse> readStream(
            ClientCall<ListEndpointsRequest, ListEndpointsResponse> call) {
        return new ReadStreamCall<>(
                "trace",
                "endpoint",
                call,
                GrpcFlows.SIMPLE_FLOW,
                ListEndpointsRequest.getDefaultInstance(),
                new Metadata(),
                statusHandler,
                Span.NOOP
        );
    }

    private ReadWriteStreamCall<ListEndpointsResponse, ListEndpointsRequest> readWriteStream(
            ClientCall<ListEndpointsRequest, ListEndpointsResponse> call) {
        return new ReadWriteStreamCall<>(
                "trace",
                "endpoint",
                call,
                GrpcFlows.SIMPLE_FLOW,
                new Metadata(),
                new AuthCallOptions(),
                statusHandler
        );
    }

    @Test
    public void readStreamSecondStartTest() {
        ClientCall<ListEndpointsRequest, ListEndpointsResponse> call = mockCall();
        ReadStreamCall<ListEndpointsRequest, ListEndpointsResponse> stream = readStream(call);

        CompletableFuture<Status> status = stream.start(message -> {
        });
        verify(call).start(eq(stream), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> stream.start(message -> {
        }));
        assertEquals("Read stream call is already started", exception.getMessage());

        // the running call is not affected
        verify(call).start(any(), any());
        verify(call, never()).cancel(any(), any());
        assertFalse(status.isDone());
    }

    @Test
    public void readStreamNullObserverTest() {
        ClientCall<ListEndpointsRequest, ListEndpointsResponse> call = mockCall();
        ReadStreamCall<ListEndpointsRequest, ListEndpointsResponse> stream = readStream(call);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stream.start(null));
        assertEquals("Observer must be not empty", exception.getMessage());
        verify(call, never()).cancel(any(), any());

        // the stream is still usable
        CompletableFuture<Status> status = stream.start(message -> {
        });
        verify(call).start(eq(stream), any());
        assertFalse(status.isDone());
    }

    @Test
    public void readWriteStreamSecondStartTest() {
        ClientCall<ListEndpointsRequest, ListEndpointsResponse> call = mockCall();
        ReadWriteStreamCall<ListEndpointsResponse, ListEndpointsRequest> stream = readWriteStream(call);

        CompletableFuture<Status> status = stream.start(message -> {
        });
        verify(call).start(eq(stream), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> stream.start(message -> {
        }));
        assertEquals("Read write stream call is already started", exception.getMessage());

        // the running call is not affected
        verify(call).start(any(), any());
        verify(call, never()).cancel(any(), any());
        assertFalse(status.isDone());
    }

    @Test
    public void readWriteStreamNullObserverTest() {
        ClientCall<ListEndpointsRequest, ListEndpointsResponse> call = mockCall();
        ReadWriteStreamCall<ListEndpointsResponse, ListEndpointsRequest> stream = readWriteStream(call);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stream.start(null));
        assertEquals("Observer must be not empty", exception.getMessage());
        verify(call, never()).cancel(any(), any());

        // the stream is still usable
        CompletableFuture<Status> status = stream.start(message -> {
        });
        verify(call).start(eq(stream), any());
        assertFalse(status.isDone());
    }
}
