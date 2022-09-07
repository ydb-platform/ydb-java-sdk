package tech.ydb.core.grpc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.utils.URITools;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import io.grpc.MethodDescriptor;


/**
 * @author Sergey Polovko
 * @author Evgeniy Pshenitsin
 * @author Nikolay Perfilov
 */
public interface GrpcTransport extends AutoCloseable {

    public String getEndpointByNodeId(int nodeId);

    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request);

    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request,
            StreamObserver<RespT> observer);
    
    public String getDatabase();
    
    @Override
    public void close();

    public static GrpcTransportBuilder forHost(HostAndPort hostAndPort, String database) {
        return new GrpcTransportBuilder(null, hostAndPort, database);
    }

    public static GrpcTransportBuilder forHost(String host, int port, String database) {
        return new GrpcTransportBuilder(null, HostAndPort.fromParts(host, port), database);
    }

    public static GrpcTransportBuilder forEndpoint(String endpoint, String database) {
        Preconditions.checkNotNull(endpoint, "endpoint is null");
        Preconditions.checkNotNull(database, "database is null");
        return new GrpcTransportBuilder(endpoint, null, database);
    }

    // [<protocol>://]<host>[:<port>]/?database=<database-path>
    public static GrpcTransportBuilder forConnectionString(String connectionString) {
        Preconditions.checkNotNull(connectionString, "connection string is null");
        String endpoint;
        String database;
        String scheme;
        try {
            URI uri = new URI(connectionString.contains("://") ? connectionString : "grpc://" + connectionString);
            endpoint = uri.getAuthority();
            Preconditions.checkNotNull(endpoint, "no endpoint in connection string");
            Map<String, List<String>> params = URITools.splitQuery(uri);
            List<String> databaseList = params.get("database");
            Preconditions.checkArgument(databaseList != null && !databaseList.isEmpty(), "no database in connection string");
            database = databaseList.get(0);
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
