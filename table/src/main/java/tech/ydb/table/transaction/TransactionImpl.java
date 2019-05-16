package tech.ydb.table.transaction;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.table.OperationsTray;
import tech.ydb.table.YdbTable;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.RollbackTxSettings;


/**
 * @author Sergey Polovko
 */
public class TransactionImpl implements Transaction {

    private final String sessionId;
    private final String txId;
    private final TableRpc tableRpc;
    private final OperationsTray operationsTray;

    public TransactionImpl(String sessionId, String txId, TableRpc tableRpc, OperationsTray operationsTray) {
        this.sessionId = sessionId;
        this.txId = txId;
        this.tableRpc = tableRpc;
        this.operationsTray = operationsTray;
    }

    @Override
    public String getId() {
        return txId;
    }

    @Override
    public CompletableFuture<Status> commit(CommitTxSettings settings) {
        YdbTable.CommitTransactionRequest request = YdbTable.CommitTransactionRequest.newBuilder()
            .setSessionId(sessionId)
            .setTxId(txId)
            .build();

        return tableRpc.commitTransaction(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("commitTransaction()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
        YdbTable.RollbackTransactionRequest request = YdbTable.RollbackTransactionRequest.newBuilder()
            .setSessionId(sessionId)
            .setTxId(txId)
            .build();

        return tableRpc.rollbackTransaction(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("rollbackTransaction()").getOperation());
            });
    }
}
