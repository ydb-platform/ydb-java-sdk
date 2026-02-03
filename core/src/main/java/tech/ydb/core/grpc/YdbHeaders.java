package tech.ydb.core.grpc;

import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;


/**
 * @author Sergey Polovko
 */
public class YdbHeaders {
    public static final Metadata.Key<String> DATABASE =
        Metadata.Key.of("x-ydb-database", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> TRACE_ID =
        Metadata.Key.of("x-ydb-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> BUILD_INFO =
        Metadata.Key.of("x-ydb-sdk-build-info", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> YDB_CLIENT_CAPABILITIES =
        Metadata.Key.of("x-ydb-client-capabilities", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> YDB_SERVER_HINTS =
        Metadata.Key.of("x-ydb-server-hints", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> AUTH_TICKET =
        Metadata.Key.of("x-ydb-auth-ticket", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> APPLICATION_NAME =
        Metadata.Key.of("x-ydb-application-name", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> CLIENT_PROCESS_ID =
        Metadata.Key.of("x-ydb-client-pid", Metadata.ASCII_STRING_MARSHALLER);

    private YdbHeaders() { }

    public static ClientInterceptor createMetadataInterceptor(GrpcTransportBuilder builder) {
        Metadata extraHeaders = new Metadata();
        extraHeaders.put(YdbHeaders.DATABASE, builder.getDatabase());
        extraHeaders.put(YdbHeaders.BUILD_INFO, builder.getVersionString());
        String appName = builder.getApplicationName();
        if (appName != null) {
            extraHeaders.put(YdbHeaders.APPLICATION_NAME, appName);
        }
        String clientPid = builder.getClientProcessId();
        if (clientPid != null) {
            extraHeaders.put(YdbHeaders.CLIENT_PROCESS_ID, clientPid);
        }
        return MetadataUtils.newAttachHeadersInterceptor(extraHeaders);
    }
}
