package tech.ydb.core.impl;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;

import com.google.protobuf.Any;
import io.grpc.MethodDescriptor;
import org.junit.Assert;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedTransport implements GrpcTransport {
    private final String database;
    private final ScheduledExecutorService scheduler;
    private final Queue<CompletableFuture<Result<DiscoveryProtos.ListEndpointsResponse>>> discovery;

    public MockedTransport(ScheduledExecutorService scheduler, String database) {
        this.scheduler = scheduler;
        this.database = database;
        this.discovery = new ConcurrentLinkedQueue<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request
    ) {
        if (method == DiscoveryServiceGrpc.getListEndpointsMethod()) {
            CompletableFuture<Result<DiscoveryProtos.ListEndpointsResponse>> future = new CompletableFuture<>();
            discovery.offer(future);
            return (CompletableFuture) future;
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <ReqT, RespT> GrpcReadStream<RespT> readStreamCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request
    ) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <ReqT, RespT> GrpcReadWriteStream<RespT, ReqT> readWriteStreamCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings
    ) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void close() {
    }

    public void checkDiscoveryCallCount(int count) {
        Assert.assertEquals("discovery mock queue size!", count, discovery.size());
    }

    public void completeNextDiscovery(String selfLocation, EndpointRecord... endpoints) {
        CompletableFuture<Result<DiscoveryProtos.ListEndpointsResponse>> future = discovery.poll();
        Assert.assertNotNull("discovery mock queue is empty!", future);

        DiscoveryProtos.ListEndpointsResult.Builder builder = DiscoveryProtos.ListEndpointsResult.newBuilder();
        for (EndpointRecord e : endpoints) {
            DiscoveryProtos.EndpointInfo.Builder b = DiscoveryProtos.EndpointInfo.newBuilder();
            b.setAddress(e.getHost());
            b.setPort(e.getPort());
            b.setNodeId(e.getNodeId());
            if (e.getLocation() != null) {
                b.setLocation(b.getLocation());
            }

            builder.addEndpoints(b.build());
        }
        builder.setSelfLocation(selfLocation);

        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setReady(true)
                .setResult(Any.pack(builder.build()))
                .setId("discovery-id")
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .build();

        future.complete(Result.success(
                DiscoveryProtos.ListEndpointsResponse.newBuilder().setOperation(operation).build()
        ));
    }

    public void completeNextDiscovery(Status status) {
        CompletableFuture<Result<DiscoveryProtos.ListEndpointsResponse>> future = discovery.poll();
        Assert.assertNotNull("discovery mock queue is empty!", future);
        future.complete(Result.fail(status));
    }

    public void completeNextDiscovery(Throwable th) {
        CompletableFuture<Result<DiscoveryProtos.ListEndpointsResponse>> future = discovery.poll();
        Assert.assertNotNull("discovery mock queue is empty!", future);
        future.completeExceptionally(th);
    }
}