package tech.ydb.query.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.SpanScope;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.test.junit4.YdbHelperRule;

public class QueryTracingTest {
    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static GrpcTransport transport;
    private static RecordingTracer tracer;

    private QueryClient queryClient;
    private TableClient tableClient;

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
        tableClient = null;
    }

    @After
    public void closeClient() {
        if (tableClient != null) {
            tableClient.close();
        }
        if (queryClient != null) {
            queryClient.close();
        }
    }

    @Test
    public void createSessionSpanIsRecorded() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            Assert.assertNotNull(session.getId());
            Assert.assertEquals(1, tracer.countClosedSpan("ydb.CreateSession"));
        }
    }

    @Test
    public void executeQuerySpanIsRecorded() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();
        }

        Assert.assertEquals(1, tracer.countClosedSpan("ydb.ExecuteQuery"));
    }

    @Test
    public void commitSpanIsRecordedInQueryTransaction() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            Result<QueryTransaction> txResult = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txResult.getStatus().expectSuccess();
            Result<QueryInfo> queryResult = txResult.getValue().createQuery("SELECT 1").execute().join();
            queryResult.getStatus().expectSuccess();
            Result<QueryInfo> commitResult = txResult.getValue().commit().join();
            commitResult.getStatus().expectSuccess();
        }

        Assert.assertEquals(1, tracer.countClosedSpan("ydb.Commit"));
    }

    @Test
    public void rollbackSpanIsRecordedInQueryTransaction() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            Result<QueryTransaction> txResult = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txResult.getStatus().expectSuccess();
            Result<QueryInfo> queryResult = txResult.getValue().createQuery("SELECT 1").execute().join();
            queryResult.getStatus().expectSuccess();
            Status rollbackStatus = txResult.getValue().rollback().join();
            rollbackStatus.expectSuccess();
        }

        Assert.assertEquals(1, tracer.countClosedSpan("ydb.Rollback"));
    }

    @Test
    public void createSessionAndExecuteQuerySpansAreRecordedInTableProxy() {
        tableClient = QueryClient.newTableClient(transport).build();
        try (Session session = tableClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            Result<DataQueryResult> result = session.executeDataQuery(
                    "SELECT 1",
                    TxControl.serializableRw().setCommitTx(true),
                    Params.empty()
            ).join();
            result.getStatus().expectSuccess();
        }

        Assert.assertEquals(1, tracer.countClosedSpan("ydb.CreateSession"));
        Assert.assertEquals(1, tracer.countClosedSpan("ydb.ExecuteQuery"));
    }

    @Test
    public void executeQuerySpansAreRecordedInTableProxyTransaction() {
        tableClient = QueryClient.newTableClient(transport).build();
        try (Session session = tableClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            TableTransaction tx1 = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
            Result<DataQueryResult> q1 = tx1.executeDataQuery("SELECT 1").join();
            q1.getStatus().expectSuccess();
            Status commitStatus = tx1.commit().join();
            commitStatus.expectSuccess();

            TableTransaction tx2 = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
            Result<DataQueryResult> q2 = tx2.executeDataQuery("SELECT 1").join();
            q2.getStatus().expectSuccess();
            Status rollbackStatus = tx2.rollback().join();
            rollbackStatus.expectSuccess();
        }

        Assert.assertEquals(1, tracer.countClosedSpan("ydb.CreateSession"));
        Assert.assertEquals(2, tracer.countClosedSpan("ydb.ExecuteQuery"));
        Assert.assertEquals(1, tracer.countClosedSpan("ydb.Commit"));
        Assert.assertEquals(1, tracer.countClosedSpan("ydb.Rollback"));
    }

    @Test
    public void querySpanIsChildOfApplicationSpan() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            Span appParent = tracer.startSpan("app.parent", SpanKind.INTERNAL);
            try (SpanScope ignored = appParent.makeCurrent()) {
                session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();
            } finally {
                appParent.end();
            }
        }

        Assert.assertEquals(1, tracer.countClosedSpanWithParent("ydb.ExecuteQuery", "app.parent"));
    }

    @Test
    public void retrySpanIsParentOfRpcSpans() {
        AtomicInteger attempt = new AtomicInteger();
        SessionRetryContext retryContext = SessionRetryContext.create(queryClient)
                .maxRetries(5)
                .backoffSlot(Duration.ofMillis(1))
                .fastBackoffSlot(Duration.ofMillis(1))
                .build();

        Span appParent = tracer.startSpan("app.parent.retry", SpanKind.INTERNAL);
        try (SpanScope ignored = appParent.makeCurrent()) {
            Status status = retryContext.supplyStatus(session -> {
                if (attempt.getAndIncrement() == 0) {
                    return CompletableFuture.completedFuture(Status.of(StatusCode.OVERLOADED));
                }
                return session.createQuery("SELECT 1", TxMode.NONE).execute().thenApply(Result::getStatus);
            }).join();
            status.expectSuccess();
        } finally {
            appParent.end();
        }

        Assert.assertEquals(2, tracer.countClosedSpan("ydb.ExecuteWithRetry"));
        Assert.assertEquals(1, tracer.countClosedSpanWithParent("ydb.CreateSession", "ydb.ExecuteWithRetry"));
        Assert.assertEquals(1, tracer.countClosedSpanWithParent("ydb.ExecuteQuery", "ydb.ExecuteWithRetry"));
    }

    @Test
    public void tableProxyRetrySpanIsParentOfRpcSpans() {
        AtomicInteger attempt = new AtomicInteger();
        tableClient = QueryClient.newTableClient(transport).build();
        tech.ydb.table.SessionRetryContext retryContext = tech.ydb.table.SessionRetryContext.create(tableClient)
                .maxRetries(5)
                .backoffSlot(Duration.ofMillis(1))
                .fastBackoffSlot(Duration.ofMillis(1))
                .build();

        Span appParent = tracer.startSpan("app.parent.table.retry", SpanKind.INTERNAL);
        try (SpanScope ignored = appParent.makeCurrent()) {
            Status status = retryContext.supplyStatus(session -> {
                if (attempt.getAndIncrement() == 0) {
                    return CompletableFuture.completedFuture(Status.of(StatusCode.OVERLOADED));
                }
                TableTransaction tx = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                Result<DataQueryResult> queryResult = tx.executeDataQuery("SELECT 1", Params.empty()).join();
                queryResult.getStatus().expectSuccess();
                return tx.commit();
            }).join();
            status.expectSuccess();
        } finally {
            appParent.end();
        }

        Assert.assertEquals(2, tracer.countClosedSpan("ydb.ExecuteWithRetry"));
        Assert.assertEquals(1, tracer.countClosedSpanWithParent("ydb.CreateSession", "ydb.ExecuteWithRetry"));
        Assert.assertEquals(1, tracer.countClosedSpanWithParent("ydb.ExecuteQuery", "ydb.ExecuteWithRetry"));
        Assert.assertEquals(1, tracer.countClosedSpanWithParent("ydb.Commit", "ydb.ExecuteWithRetry"));
    }

    private static final class RecordingTracer implements Tracer {
        private final List<RecordingSpan> spans = Collections.synchronizedList(new ArrayList<>());
        private final ThreadLocal<RecordingSpan> currentSpan = new ThreadLocal<>();

        @Override
        public Span startSpan(String spanName, SpanKind spanKind) {
            RecordingSpan span = new RecordingSpan(this, spanName, spanKind, currentSpan.get());
            spans.add(span);
            return span;
        }

        void reset() {
            spans.clear();
        }

        int countClosedSpan(String spanName) {
            int count = 0;
            synchronized (spans) {
                for (RecordingSpan span : spans) {
                    if (span.name.equals(spanName) && span.closed) {
                        count++;
                    }
                }
            }
            return count;
        }

        int countClosedSpanWithParent(String spanName, String parentSpanName) {
            int count = 0;
            synchronized (spans) {
                for (RecordingSpan span : spans) {
                    if (span.closed
                            && span.name.equals(spanName)
                            && span.parent != null
                            && span.parent.name.equals(parentSpanName)) {
                        count++;
                    }
                }
            }
            return count;
        }

        SpanScope makeSpanCurrent(RecordingSpan span) {
            RecordingSpan previous = currentSpan.get();
            currentSpan.set(span);
            return () -> {
                if (previous == null) {
                    currentSpan.remove();
                } else {
                    currentSpan.set(previous);
                }
            };
        }
    }

    private static final class RecordingSpan implements Span {
        private final RecordingTracer tracer;
        private final String name;
        private final SpanKind kind;
        private final RecordingSpan parent;
        private volatile boolean closed = false;

        RecordingSpan(RecordingTracer tracer, String name, SpanKind kind, RecordingSpan parent) {
            this.tracer = tracer;
            this.name = name;
            this.kind = kind;
            this.parent = parent;
        }

        @Override
        public String getId() {
            return name + ":" + kind;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public SpanScope makeCurrent() {
            return tracer.makeSpanCurrent(this);
        }

        @Override
        public void end() {
            this.closed = true;
        }
    }
}
