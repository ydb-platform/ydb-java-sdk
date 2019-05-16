package tech.ydb.table;

import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.rpc.RpcFactory;
import tech.ydb.core.rpc.RpcTransport;
import tech.ydb.table.rpc.OperationRpc;
import tech.ydb.table.rpc.SchemeRpc;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.rpc.grpc.GrpcOperationRpc;
import tech.ydb.table.rpc.grpc.GrpcSchemeRpc;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;

import static java.util.Objects.requireNonNull;


/**
 * @author Sergey Polovko
 */
public class TableServiceBuilder {

    private RpcFactory<TableRpc> tableRpcFactory = GrpcTableRpc::create;
    private RpcFactory<SchemeRpc> schemeRpcFactory = GrpcSchemeRpc::create;
    private RpcFactory<OperationRpc> operationRpcFactory = GrpcOperationRpc::create;

    private final RpcTransport transport;
    private final boolean transportOwned;
    private int maxSessionsPooled = 10;

    private TableServiceBuilder(RpcTransport transport, boolean transportOwned) {
        this.transport = transport;
        this.transportOwned = transportOwned;
    }

    public static TableServiceBuilder useTransport(@WillNotClose RpcTransport transport) {
        return new TableServiceBuilder(requireNonNull(transport, "transport"), false);
    }

    public static TableServiceBuilder ownTransport(@WillClose RpcTransport transport) {
        return new TableServiceBuilder(requireNonNull(transport, "transport"), true);
    }

    public TableServiceBuilder withTableRpc(RpcFactory<TableRpc> factory) {
        this.tableRpcFactory = requireNonNull(factory, "factory");
        return this;
    }

    public TableServiceBuilder withSchemeRpc(RpcFactory<SchemeRpc> factory) {
        this.schemeRpcFactory = requireNonNull(factory, "factory");
        return this;
    }

    public TableServiceBuilder withOperationRpc(RpcFactory<OperationRpc> factory) {
        this.operationRpcFactory = requireNonNull(factory, "factory");
        return this;
    }

    public TableServiceBuilder withMaxSessionsPooled(int maxSessionPooled) {
        if (maxSessionPooled < 0) {
            throw new IllegalArgumentException("maxSessionsPooled(" + maxSessionPooled + ") must be >= 0");
        }
        this.maxSessionsPooled = maxSessionPooled;
        return this;
    }

    public TableService build() {
        TableRpc tableRpc = requireNonNull(
            tableRpcFactory.create(transport),
            "invalid transport for TableRcp");

        SchemeRpc schemeRpc = requireNonNull(
            schemeRpcFactory.create(transport),
            "invalid transport for SchemeRcp");

        OperationRpc operationRpc = requireNonNull(
            operationRpcFactory.create(transport),
            "invalid transport for OperationRcp");

        return new TableServiceImpl(
            tableRpc, schemeRpc, operationRpc,
            transportOwned ? transport : null,
            maxSessionsPooled);
    }
}
