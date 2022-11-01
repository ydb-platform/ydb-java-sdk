package tech.ydb.tests.integration;

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

    private static final String DEFAULT_YDB_IMAGE = "cr.yandex/yc/yandex-docker-local-ydb:latest";
    private static final String DOCKER_DATABASE = "/local";
    private static final String PEM_PATH = "/ydb_certs/ca.pem";

    private final int grpcsPort; // Secure connection
    private final int grpcPort;  // Non secure connection

    YdbDockerContainer(String image) {
        super(image);

        PortsGenerator gen = new PortsGenerator();
        grpcsPort = gen.findAvailablePort();
        grpcPort = gen.findAvailablePort();

        addExposedPort(grpcPort); // don't expose by default

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
        return copyFileFromContainer(PEM_PATH, is -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);
            return baos.toByteArray();
        });
    }

    public String database() {
        return DOCKER_DATABASE;
    }

    public static YdbDockerContainer createAndStart() {
        String customImage = System.getProperty("YDB_IMAGE", DEFAULT_YDB_IMAGE);
        YdbDockerContainer container = new YdbDockerContainer(customImage);
        container.start();

        return container;
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
