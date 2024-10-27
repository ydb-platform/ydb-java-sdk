package tech.ydb.auth;

import java.nio.file.Path;

import tech.ydb.core.impl.auth.GrpcAuthRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OAuth2AuthHelper {
    private OAuth2AuthHelper() { }

    public static AuthIdentity configFileIdentity(Path file, GrpcAuthRpc rpc) {
        return OAuth2TokenExchangeProvider
                .fromFile(file.toFile())
                .build()
                .createAuthIdentity(rpc);
    }
}
