package tech.ydb.query.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.test.junit4.YdbHelperRule;

public class QueryTracingTest {
    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static GrpcTransport transport;
    private static RecordingTracer tracer;

    private QueryClient queryClient;

    @BeforeClass
    public static void initTransport() {
        tracer = new RecordingTracer();
        transport = GrpcTransport.forEndpoint(YDB.endpoint(), YDB.database())
                .withAuthProvider(new TokenAuthProvider(YDB.authToken()))
                .withTracer(tracer)
                .build();
    }

    @AfterClass
    public static void closeTransport() {
        transport.close();
    }

    @Before
    public void initClient() {
        tracer.reset();
        queryClient = QueryClient.newClient(transport).build();
    }

    @After
    public void closeClient() {
        queryClient.close();
    }

    @Test
    public void queryServiceSpansAreRecorded() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();

            Result<QueryTransaction> txResult = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txResult.getStatus().expectSuccess();
            Result<QueryInfo> commitResult = txResult.getValue().commit().join();
            commitResult.getStatus().expectSuccess();

            Result<QueryTransaction> txResult2 = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txResult2.getStatus().expectSuccess();
            Status rollbackStatus = txResult2.getValue().rollback().join();
            rollbackStatus.expectSuccess();
        }

        Assert.assertTrue(tracer.hasClosedSpan("ydb.CreateSession"));
        Assert.assertTrue(tracer.hasClosedSpan("ydb.ExecuteQuery"));
        Assert.assertTrue(tracer.hasClosedSpan("ydb.Commit"));
        Assert.assertTrue(tracer.hasClosedSpan("ydb.Rollback"));
    }

    private static final class RecordingTracer implements Tracer {
        private final List<RecordingSpan> spans = Collections.synchronizedList(new ArrayList<RecordingSpan>());

        @Override
        public Span startSpan(String spanName, SpanKind spanKind) {
            RecordingSpan span = new RecordingSpan(spanName, spanKind);
            spans.add(span);
            return span;
        }

        void reset() {
            spans.clear();
        }

        boolean hasClosedSpan(String spanName) {
            synchronized (spans) {
                for (RecordingSpan span : spans) {
                    if (span.name.equals(spanName) && span.closed) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static final class RecordingSpan implements Span {
        private final String name;
        private final SpanKind kind;
        private volatile boolean closed = false;

        RecordingSpan(String name, SpanKind kind) {
            this.name = name;
            this.kind = kind;
        }

        @Override
        public String getId() {
            return name + ":" + kind;
        }

        @Override
        public void setAttribute(String key, String value) {
            // not needed for this test
        }

        @Override
        public void setAttribute(String key, long value) {
            // not needed for this test
        }

        @Override
        public Span recordException(Throwable error) {
            return this;
        }

        @Override
        public void end() {
            this.closed = true;
        }
    }
}
