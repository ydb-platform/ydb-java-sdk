package tech.ydb.query.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.settings.AttachSessionSettings;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SessionImpl#attach} sets {@link GrpcRequestSettings#getPessimizationHook()} when the attach
 * stream delivers {@code NODE_SHUTDOWN} with a non-zero node id. The transport applies pessimization
 * on stream completion ({@code postComplete}).
 */
public class SessionImplAttachHintTest {

    private static final AttachSessionSettings ATTACH_SETTINGS = AttachSessionSettings.newBuilder().build();

    /**
     * {@link QueryServiceRpc} reads {@link GrpcTransport#getTracer()} in the constructor; attach tests override
     * {@link QueryServiceRpc#attachSession} and do not use the transport otherwise.
     */
    private static final GrpcTransport DUMMY_TRANSPORT = mock(GrpcTransport.class);

    static {
        when(DUMMY_TRANSPORT.getTracer()).thenReturn(NoopTracer.getInstance());
    }

    @Test
    public void nodeShutdownHint_setsPessimizationHookWhenNodeIdKnown() {
        YdbQuery.CreateSessionResponse createResponse = YdbQuery.CreateSessionResponse.newBuilder()
                .setSessionId("s1")
                .setNodeId(42)
                .build();

        YdbQuery.SessionState msg = YdbQuery.SessionState.newBuilder()
                .setStatus(StatusIds.StatusCode.SUCCESS)
                .setNodeShutdown(YdbQuery.NodeShutdownHint.getDefaultInstance())
                .build();

        TestRpc rpc = new TestRpc(singleMessageStream(msg));
        try (TestSession session = new TestSession(rpc, createResponse)) {
            session.attach(ATTACH_SETTINGS).start(s -> {
            }).join();

            Assert.assertNotNull(rpc.capturedSettings);
            Assert.assertNotNull(rpc.capturedSettings.getPessimizationHook());
            Assert.assertTrue(rpc.capturedSettings.getPessimizationHook().getAsBoolean());
            Assert.assertEquals(StatusCode.BAD_SESSION, session.getLastSessionState().getCode());
        }
    }

    @Test
    public void nodeShutdownHint_doesNotPessimizeWhenNodeIdIsZero() {
        YdbQuery.CreateSessionResponse createResponse = YdbQuery.CreateSessionResponse.newBuilder()
                .setSessionId("s-zero")
                .setNodeId(0)
                .build();

        YdbQuery.SessionState msg = YdbQuery.SessionState.newBuilder()
                .setStatus(StatusIds.StatusCode.SUCCESS)
                .setNodeShutdown(YdbQuery.NodeShutdownHint.getDefaultInstance())
                .build();

        TestRpc rpc = new TestRpc(singleMessageStream(msg));
        try (TestSession session = new TestSession(rpc, createResponse)) {
            session.attach(ATTACH_SETTINGS).start(s -> {
            }).join();

            Assert.assertNotNull(rpc.capturedSettings);
            Assert.assertNotNull(rpc.capturedSettings.getPessimizationHook());
            Assert.assertFalse(rpc.capturedSettings.getPessimizationHook().getAsBoolean());
            Assert.assertEquals(StatusCode.BAD_SESSION, session.getLastSessionState().getCode());
        }
    }

    @Test
    public void sessionShutdownHint_doesNotPessimizeEndpoint() {
        YdbQuery.CreateSessionResponse createResponse = YdbQuery.CreateSessionResponse.newBuilder()
                .setSessionId("s2")
                .setNodeId(99)
                .build();

        YdbQuery.SessionState msg = YdbQuery.SessionState.newBuilder()
                .setStatus(StatusIds.StatusCode.SUCCESS)
                .setSessionShutdown(YdbQuery.SessionShutdownHint.getDefaultInstance())
                .build();

        TestRpc rpc = new TestRpc(singleMessageStream(msg));
        try (TestSession session = new TestSession(rpc, createResponse)) {
            session.attach(ATTACH_SETTINGS).start(s -> {
            }).join();

            Assert.assertNotNull(rpc.capturedSettings);
            Assert.assertNotNull(rpc.capturedSettings.getPessimizationHook());
            Assert.assertFalse(rpc.capturedSettings.getPessimizationHook().getAsBoolean());
            Assert.assertEquals(StatusCode.BAD_SESSION, session.getLastSessionState().getCode());
        }
    }

    private static GrpcReadStream<YdbQuery.SessionState> singleMessageStream(YdbQuery.SessionState message) {
        return new GrpcReadStream<YdbQuery.SessionState>() {
            @Override
            public CompletableFuture<Status> start(Observer<YdbQuery.SessionState> observer) {
                observer.onNext(message);
                return CompletableFuture.completedFuture(Status.SUCCESS);
            }

            @Override
            public void cancel() {
            }
        };
    }

    private static final class TestRpc extends QueryServiceRpc {
        private final GrpcReadStream<YdbQuery.SessionState> attachStream;
        private GrpcRequestSettings capturedSettings;

        TestRpc(GrpcReadStream<YdbQuery.SessionState> attachStream) {
            super(DUMMY_TRANSPORT);
            this.attachStream = attachStream;
        }

        @Override
        public GrpcReadStream<YdbQuery.SessionState> attachSession(
                YdbQuery.AttachSessionRequest request,
                GrpcRequestSettings settings) {
            this.capturedSettings = settings;
            return attachStream;
        }
    }

    private static final class TestSession extends SessionImpl {
        private final AtomicReference<Status> lastState = new AtomicReference<>();

        TestSession(QueryServiceRpc rpc, YdbQuery.CreateSessionResponse response) {
            super(rpc, response);
        }

        @Override
        public void updateSessionState(Status status) {
            lastState.set(status);
        }

        Status getLastSessionState() {
            return lastState.get();
        }

        @Override
        public void close() {
        }
    }
}
