package tech.ydb.auth;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface RpcTransportFactory<T> {
    String getEndpoint();
    String getDatabase();

    T createRpcTransport();
}
