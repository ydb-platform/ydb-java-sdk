package tech.ydb.core.impl.auth;

import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.auth.AuthIdentity;


/**
 * @author Sergey Polovko
 */
class YdbCallCredentials extends CallCredentials {
    static final Metadata.Key<String> AUTH_TICKET =
        Metadata.Key.of("x-ydb-auth-ticket", Metadata.ASCII_STRING_MARSHALLER);

    private static final Logger logger = LoggerFactory.getLogger(YdbCallCredentials.class);

    private final AuthIdentity identity;

    YdbCallCredentials(AuthIdentity identity) {
        this.identity = identity;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        try {
            Metadata headers = new Metadata();
            String token = identity.getToken();
            if (token != null) {
                headers.put(AUTH_TICKET, token);
            }
            applier.apply(headers);
        } catch (RuntimeException ex) {
            logger.error("invalid token", ex);
            applier.fail(Status.UNAUTHENTICATED.withCause(ex));
        }
    }

    @Override
    public void thisUsesUnstableApi() { }
}
