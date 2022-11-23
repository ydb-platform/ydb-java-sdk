package tech.ydb.test.integration;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.scheme.SchemeOperationProtos;
import tech.ydb.scheme.v1.SchemeServiceGrpc;

/**
 *
 * @author Alexandr Gorshenin
 */
public class YdbDockerContainer extends GenericContainer<YdbDockerContainer> {
    private static final Logger log = LoggerFactory.getLogger(YdbDockerContainer.class);

    private final int grpcsPort; // Secure connection
    private final int grpcPort;  // Non secure connection

    YdbDockerContainer() {
        super(YdbTestConstants.YDB_IMAGE);

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

        withCreateContainerCmdModifier(modifier -> modifier
                .withName("ydb-" + UUID.randomUUID())
                .withHostName(getHost()));
        waitingFor(new YdbCanCreateTableWaitStrategy());
    }

    public String nonSecureEndpoint() {
        return String.format("%s:%s", getHost(), grpcPort);
    }

    public String secureEndpoint() {
        return String.format("%s:%s", getHost(), grpcsPort);
    }

    public byte[] pemCert() {
        return copyFileFromContainer(YdbTestConstants.YDB_PEM_PATH, is -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);
            return baos.toByteArray();
        });
    }

    public String database() {
        return YdbTestConstants.YDB_DATABASE;
    }

    private class YdbCanCreateTableWaitStrategy extends AbstractWaitStrategy {
        @Override
        protected void waitUntilReady() {
            // Wait 30 second for start of ydb
            Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(this::checkReady);
                log.info("YDB container is ready");
                return true;
            });
        }

        private void checkReady() {
            // Simple check if ydb server is ready - create a transport and execute simple command
            try (GrpcTransport transport = GrpcTransport.forEndpoint(nonSecureEndpoint(), database()).build()) {
                SchemeOperationProtos.ListDirectoryRequest request = SchemeOperationProtos.ListDirectoryRequest
                        .newBuilder()
                        .build();
                GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();
                transport
                        .unaryCall(SchemeServiceGrpc.getListDirectoryMethod(), settings, request)
                        .join().getStatus().expectSuccess("Can't ls directory");
            } catch (Exception e) {
                log.info("execution problem {}", e.getMessage());
                throw new RuntimeException("YDB container isn't ready", e);
            }
        }
    }
}
