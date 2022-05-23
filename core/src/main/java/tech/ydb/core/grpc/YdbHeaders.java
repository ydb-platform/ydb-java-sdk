package tech.ydb.core.grpc;

import io.grpc.Metadata;


/**
 * @author Sergey Polovko
 */
public class YdbHeaders {
    private YdbHeaders() {}

    static final Metadata.Key<String> AUTH_TICKET =
        Metadata.Key.of("x-ydb-auth-ticket", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> DATABASE =
        Metadata.Key.of("x-ydb-database", Metadata.ASCII_STRING_MARSHALLER);

    static final Metadata.Key<String> TRACE_ID =
        Metadata.Key.of("x-ydb-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> BUILD_INFO =
        Metadata.Key.of("x-ydb-sdk-build-info", Metadata.ASCII_STRING_MARSHALLER);

}
