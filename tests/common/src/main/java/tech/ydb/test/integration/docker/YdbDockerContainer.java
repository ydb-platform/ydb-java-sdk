package tech.ydb.test.integration.docker;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.ResourceReaper;

import tech.ydb.core.Operations;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.discovery.DiscoveryProtos;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;
import tech.ydb.test.integration.YdbEnvironment;

/**
 *
 * @author Alexandr Gorshenin
 */
public class YdbDockerContainer extends GenericContainer<YdbDockerContainer> {
    private static final Logger logger = LoggerFactory.getLogger(YdbDockerContainer.class);

    private final YdbEnvironment env;
    private final int grpcsPort; // Secure connection
    private final int grpcPort;  // Non secure connection

    YdbDockerContainer(YdbEnvironment env) {
        super(env.dockerImage());

        this.env = env;

        PortsGenerator gen = new PortsGenerator();
        grpcsPort = gen.findAvailablePort();
        grpcPort = gen.findAvailablePort();

        addExposedPort(grpcPort);
        addExposedPort(grpcsPort);

        // Host ports and container ports MUST BE equal - ydb implementation limitation
        addFixedExposedPort(grpcsPort, grpcsPort);
        addFixedExposedPort(grpcPort, grpcPort);

        withEnv("GRPC_PORT", String.valueOf(grpcPort));
        withEnv("GRPC_TLS_PORT", String.valueOf(grpcsPort));
        withEnv("YDB_USE_IN_MEMORY_PDISKS", "true");

        withReuse(env.dockerReuse());

        String id = "ydb-" + UUID.randomUUID();

        withLabel("com.docker.ydb.id", id);

        withCreateContainerCmdModifier(modifier -> modifier
                .withName(id)
                .withHostName(getHost()));

        waitingFor(new AbstractWaitStrategy() {
            @Override
            protected void waitUntilReady() {
                // Wait 30 second for start of ydb
                Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
                    getRateLimiter().doWhenReady(YdbDockerContainer.this::checkReady);
                    logger.info("YDB container is ready");
                    return true;
                });
            }
        });

        // Register container cleaner
        ResourceReaper.instance().registerLabelsFilterForCleanup(Collections.singletonMap(
            "com.docker.ydb.id", id
        ));
    }

    public String nonSecureEndpoint() {
        return String.format("%s:%s", getHost(), grpcPort);
    }

    public String secureEndpoint() {
        return String.format("%s:%s", getHost(), grpcsPort);
    }

    public byte[] pemCert() {
        return copyFileFromContainer(env.dockerPemPath(), is -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);
            return baos.toByteArray();
        });
    }

    public String database() {
        return env.dockerDatabase();
    }

    private void checkReady() {
        try (GrpcTransport transport = GrpcTransport.forEndpoint(nonSecureEndpoint(), database()).build()) {
            // Run discovery to check that the container is ready

            DiscoveryProtos.ListEndpointsResult result = transport.unaryCall(
                    DiscoveryServiceGrpc.getListEndpointsMethod(),
                    GrpcRequestSettings.newBuilder().build(),
                    DiscoveryProtos.ListEndpointsRequest.newBuilder().setDatabase(database()).build()
            ).thenApply(Operations.resultUnwrapper(
                    DiscoveryProtos.ListEndpointsResponse::getOperation,
                    DiscoveryProtos.ListEndpointsResult.class)
            ).join().getValue();

            logger.debug("discovery returns {} endpoints and self location {}",
                    result.getEndpointsCount(), result.getSelfLocation());
        } catch (Exception e) {
            logger.info("execution problem {}", e.getMessage());
            throw new RuntimeException("YDB container isn't ready", e);
        }
    }
}
