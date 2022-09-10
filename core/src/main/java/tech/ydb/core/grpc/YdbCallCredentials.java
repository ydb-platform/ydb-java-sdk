package tech.ydb.core.grpc;

import java.util.concurrent.Executor;

import tech.ydb.core.auth.AuthIdentity;

import io.grpc.CallCredentials;
import io.grpc.Metadata;


/**
 * @author Sergey Polovko
 */
public class YdbCallCredentials extends CallCredentials {

    private final AuthIdentity identity;

    public YdbCallCredentials(AuthIdentity identity) {
        this.identity = identity;
    }

    @Override
    public void applyRequestMetadata(
        RequestInfo requestInfo,
        Executor appExecutor,
        MetadataApplier applier)
    {
        String token = identity.getToken();
        if (token != null && !token.isEmpty()) {
            Metadata headers = new Metadata();
            headers.put(YdbHeaders.AUTH_TICKET, token);
            applier.apply(headers);
        }
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
