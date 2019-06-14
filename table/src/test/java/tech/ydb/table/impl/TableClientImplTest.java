package tech.ydb.table.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;
import tech.ydb.table.YdbTable.CreateSessionResponse;
import tech.ydb.table.YdbTable.CreateSessionResult;
import org.junit.Assert;
import org.junit.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 */
public class TableClientImplTest {

    @Test
    public void createSessionAndRelease() {
        TableClient client = TableClient.newClient(new TableRpcStub() {

            @Override
            public CompletableFuture<Result<CreateSessionResponse>> createSession(
                YdbTable.CreateSessionRequest request, long deadlineAfter)
            {
                CreateSessionResult result = CreateSessionResult.newBuilder()
                    .setSessionId("session1")
                    .build();
                CreateSessionResponse response = CreateSessionResponse.newBuilder()
                    .setOperation(Operations.packResult(result))
                    .build();
                return completedFuture(Result.success(response));
            }
        })
        .build();

        Session session = client.createSession().join().expect("cannot create session");
        Assert.assertFalse(session.release());

        session.close();
        client.close();
    }
}
