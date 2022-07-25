package tech.ydb.table.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;
import tech.ydb.table.YdbTable.CreateSessionResponse;
import tech.ydb.table.YdbTable.CreateSessionResult;
import tech.ydb.table.YdbTable.DeleteSessionResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 */
public class TableClientImpl2Test {

    private int expectPoolSize;
    private TableClient client;

    @Before
    public void init() {
        client = TableClient.newClient(new TableRpcStub() {

            @Override
            public CompletableFuture<Result<CreateSessionResponse>> createSession(
                    YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
                CreateSessionResult result = CreateSessionResult.newBuilder()
                        .setSessionId("session1")
                        .build();
                CreateSessionResponse response = CreateSessionResponse.newBuilder()
                        .setOperation(TableClientImplTest.resultOperation(result))
                        .build();
                return completedFuture(Result.success(response));
            }

            @Override
            public CompletableFuture<Result<DeleteSessionResponse>> deleteSession(
                    YdbTable.DeleteSessionRequest request, GrpcRequestSettings settings) {

                DeleteSessionResponse response = DeleteSessionResponse.newBuilder()
                        .setOperation(TableClientImplTest.successOperation())
                        .build();
                return completedFuture(Result.success(response));
            }
        }).build();

        this.expectPoolSize = 50;
        Assert.assertEquals(expectPoolSize, client.getSessionPoolStats().getMaxSize()); // default pool size

        checkPoolStatus(0, 0);
    }

    @After
    public void close() {
        client.close();
        checkPoolStatus(0, 0);
    }

    @Test
    public void createSessionAndRelease() {
        Session session = client.createSession().join().expect("cannot create session");
        checkPoolStatus(0, 0);

        Assert.assertFalse(session.release());
        checkPoolStatus(0, 0);

        session.close().join().expect("cannot close session");
        checkPoolStatus(0, 0);
    }

    @Test
    public void getOrCreateSessionPoisoned() {
        Session session = nextSession();
        checkPoolStatus(1, 0);

        Assert.assertTrue(session.release());
        checkPoolStatus(0, 1);

        session.close().join().expect("cannot close session");

        // Session was closed - we MUST NOT have idle objects in pool
        checkPoolStatus(0, 0);

        // Session was closed - we MUST NOT return the same object when acquiring
        Assert.assertNotSame("Poisoned object from pool",
                session, nextSession());
    }

    @Test
    public void getOrCreateSessionAndReleaseInARow() {
        Session session1 = nextSession();
        checkPoolStatus(1, 0);

        Session session2 = nextSession();
        checkPoolStatus(2, 0);

        Assert.assertTrue(session1.release());
        checkPoolStatus(1, 1);

        Assert.assertTrue(session2.release());
        checkPoolStatus(0, 2);

        // LIFO
        Assert.assertSame(session2, nextSession());
        checkPoolStatus(1, 1);

        Assert.assertSame(session1, nextSession());
        checkPoolStatus(2, 0);

        Assert.assertTrue(session2.release());
        checkPoolStatus(1, 1);

        Assert.assertTrue(session1.release());
        checkPoolStatus(0, 2);

        Assert.assertSame(session1, nextSession());
        checkPoolStatus(1, 1);

        Assert.assertSame(session2, nextSession());
        checkPoolStatus(2, 0);
    }


    @Test
    public void getOrCreateSessionAndReleaseRepeat() {
        for (int i = 0; i < expectPoolSize * 2; i++) {
            Session session = client.getOrCreateSession(Duration.ZERO).join()
                    .expect("cannot create session after " + i + " iterations");
            checkPoolStatus(1, 0);
            Assert.assertTrue(session.release());
            checkPoolStatus(0, 1);
        }
    }

    @Test
    public void getOrCreateSessionAndReleaseMultipleTimes() {
        Session session = nextSession();
        checkPoolStatus(1, 0);
        for (int i = 0; i < expectPoolSize * 2; i++) {
            Assert.assertTrue(session.release());
            checkPoolStatus(0, 1);
        }

        Assert.assertSame(session, nextSession());
        checkPoolStatus(1, 0);
    }

    @Test
    public void getOrCreateSessionAndCloseRepeat() {
        for (int i = 0; i < expectPoolSize * 2; i++) {
            Session session = client.getOrCreateSession(Duration.ZERO).join()
                    .expect("cannot create session after " + i + " iterations");
            checkPoolStatus(1, 0);
            session.close().join().expect("cannot close session");
            checkPoolStatus(0, 0);
        }
    }

    @Test
    public void getOrCreateSessionAndCloseMultipleTimes() {
        Session session = nextSession();
        checkPoolStatus(1, 0);
        for (int i = 0; i < expectPoolSize * 2; i++) {
            session.close().join().expect("cannot close session");
            checkPoolStatus(0, 0);
        }

        Session session2 = client.getOrCreateSession(Duration.ZERO).join().expect("cannot create session 2");
        checkPoolStatus(1, 0);
        Assert.assertNotSame(session, session2);
    }

    private Session nextSession() {
        return client.getOrCreateSession(Duration.ZERO).join().expect("cannot create session");
    }

    private void checkPoolStatus(int expectAcquired, int expectIdle) {
        Assert.assertEquals(expectAcquired, client.getSessionPoolStats().getAcquiredCount());
        Assert.assertEquals(expectIdle, client.getSessionPoolStats().getIdleCount());
    }
}
