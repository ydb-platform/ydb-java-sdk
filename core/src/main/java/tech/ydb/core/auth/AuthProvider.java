package tech.ydb.core.auth;

import tech.ydb.auth.AuthRpcProvider;

/**
 * @deprecated
 * Use {@link tech.ydb.auth.AuthProvider} instead.
 */
@Deprecated
public interface AuthProvider extends AuthRpcProvider<AuthRpc> {
    @Override
    AuthIdentity createAuthIdentity(AuthRpc rpc);
}
