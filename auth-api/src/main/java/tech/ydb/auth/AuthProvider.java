package tech.ydb.auth;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface AuthProvider<T> {
    AuthIdentity createAuthIdentity(RpcTransportFactory<T> rpcFactory);
}
