package ru.yandex.ydb.core.grpc;

import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

import ru.yandex.ydb.core.auth.AuthProvider;


/**
 * @author Sergey Polovko
 */
public class YdbCallCredentials extends CallCredentials {

    private final AuthProvider authProvider;

    public YdbCallCredentials(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void applyRequestMetadata(
        RequestInfo requestInfo,
        Executor appExecutor,
        MetadataApplier applier)
    {
        Metadata headers = new Metadata();
        headers.put(YdbHeaders.AUTH_TICKET, authProvider.getToken());
        applier.apply(headers);
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
