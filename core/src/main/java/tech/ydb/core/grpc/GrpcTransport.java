package tech.ydb.core.grpc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import io.grpc.MethodDescriptor;

import tech.ydb.core.Result;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.core.utils.URITools;


/**
 * @author Sergey Polovko
 * @author Evgeniy Pshenitsin
 * @author Nikolay Perfilov
 */
public interface GrpcTransport extends AutoCloseable {
    // TODO: <ReqT extends Message, RespT extends Message>
    <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request);

    <ReqT, RespT> GrpcReadStream<RespT> readStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request);

    <ReqT, RespT> GrpcReadWriteStream<RespT, ReqT> readWriteStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings);

    String getDatabase();

    ScheduledExecutorService getScheduler();

    default Tracer getTracer() {
        return NoopTracer.getInstance();
    }

    default String getServerAddress() {
        return "ydb.server";
    }

    default int getServerPort() {
        return 2135;
    }

    default String getTransportScheme() {
        return "grpc";
    }

    @Override
    void close();

    static GrpcTransportBuilder forHost(HostAndPort hostAndPort, String database) {
        return new GrpcTransportBuilder(null, hostAndPort, database);
    }

    static GrpcTransportBuilder forHost(String host, int port, String database) {
        return new GrpcTransportBuilder(null, HostAndPort.fromParts(host, port), database);
    }

    static GrpcTransportBuilder forEndpoint(String endpoint, String database) {
        Preconditions.checkNotNull(endpoint, "endpoint is null");
        Preconditions.checkNotNull(database, "database is null");
        return new GrpcTransportBuilder(endpoint, null, database);
    }

    // [<protocol>://]<host>[:<port>]/<database-path> - main form
    // [<protocol>://]<host>[:<port>]/?database=<database-path> - deprecated mode
    static GrpcTransportBuilder forConnectionString(String connectionString) {
        Preconditions.checkNotNull(connectionString, "connection string is null");
        String endpoint;
        String database;
        String scheme;
        try {
            URI uri = new URI(connectionString.contains("://") ? connectionString : "grpc://" + connectionString);
            endpoint = uri.getAuthority();
            Preconditions.checkNotNull(endpoint, "no endpoint in connection string");
            database = uri.getPath();
            Map<String, List<String>> params = URITools.splitQuery(uri);
            List<String> databaseList = params.get("database");
            if (databaseList != null && !databaseList.isEmpty()) {
                // depracted mode has high priority than main
                database = databaseList.get(0);
            }

            Preconditions.checkArgument(database != null && !database.isEmpty(), "no database in connection string");
            scheme = uri.getScheme();
        } catch (URISyntaxException | RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse connection string '" + connectionString +
                    "'. Expected format: [<protocol>://]<host>[:<port>]/?database=<database-path>", e);
        }
        GrpcTransportBuilder builder = new GrpcTransportBuilder(endpoint, null, database);
        if (scheme.equals("grpcs")) {
            builder.withSecureConnection();
        } else if (!scheme.equals("grpc")) {
            throw new IllegalArgumentException("Unknown protocol '" + scheme + "' in connection string");
        }
        return builder;
    }
}
