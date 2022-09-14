package tech.ydb.core.grpc;

import java.util.concurrent.Executor;

import tech.ydb.core.auth.AuthIdentity;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Sergey Polovko
 */
public class YdbCallCredentials extends CallCredentials {
    private static final Logger logger = LoggerFactory.getLogger(YdbCallCredentials.class);

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
        try {
            String token = identity.getToken();
            if (token != null && !token.isEmpty()) {
                Metadata headers = new Metadata();
                headers.put(YdbHeaders.AUTH_TICKET, token);
                applier.apply(headers);
            }
        } catch (RuntimeException ex) {
            logger.error("invalid token", ex);
            applier.fail(Status.UNAUTHENTICATED);
        }
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
