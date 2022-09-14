package tech.ydb.core.auth;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface AuthProvider {
    AuthIdentity createAuthIdentity(AuthRpc rpc);
}
