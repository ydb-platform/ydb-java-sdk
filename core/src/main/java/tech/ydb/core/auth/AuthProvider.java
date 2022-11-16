package tech.ydb.core.auth;

/**
 *
 * @author Aleksandr Gorshenin
 */
@Deprecated
public interface AuthProvider extends tech.ydb.auth.AuthProvider<AuthRpc> {
    @Override
    AuthIdentity createAuthIdentity(AuthRpc rpc);
}
