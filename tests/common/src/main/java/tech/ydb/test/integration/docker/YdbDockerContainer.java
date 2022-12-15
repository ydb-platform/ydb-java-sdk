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

import tech.ydb.ValueProtos;
import tech.ydb.core.Operations;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.YdbTable;
import tech.ydb.table.v1.TableServiceGrpc;
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
            logger.info("create session");
            String sessionID = transport.unaryCall(
                    TableServiceGrpc.getCreateSessionMethod(),
                    GrpcRequestSettings.newBuilder().build(),
                    YdbTable.CreateSessionRequest.newBuilder().build()
            ).thenApply(Operations.resultUnwrapper(
                    YdbTable.CreateSessionResponse::getOperation,
                    YdbTable.CreateSessionResult.class)
            ).join().getValue().getSessionId();

            logger.debug("chekc ready create session -> {}", sessionID);
            try {
                logger.info("create test table");
                String testTable = database() + "/docker_init_table";
                YdbTable.ColumnMeta column = YdbTable.ColumnMeta.newBuilder()
                        .setName("id")
                        .setType(ValueProtos.Type.newBuilder()
                                .setTypeId(ValueProtos.Type.PrimitiveTypeId.INT64)
                                .build())
                        .build();

                Status status = transport.unaryCall(
                        TableServiceGrpc.getCreateTableMethod(),
                        GrpcRequestSettings.newBuilder().build(),
                        YdbTable.CreateTableRequest.newBuilder()
                                .setPath(testTable)
                                .addColumns(column)
                                .addPrimaryKey("id")
                                .build()
                ).join().getStatus();
                logger.info("create table -> {}", status);
                status.expectSuccess("can't create test table");
            } finally {
                logger.info("delete session");
                Status status = transport.unaryCall(
                        TableServiceGrpc.getDeleteSessionMethod(),
                        GrpcRequestSettings.newBuilder().build(),
                        YdbTable.DeleteSessionRequest.newBuilder().setSessionId(sessionID).build()
                ).join().getStatus();
                logger.info("delete session -> {}", status);
            }
        } catch (Exception e) {
            logger.info("execution problem {}", e.getMessage());
            throw new RuntimeException("YDB container isn't ready", e);
        }
    }
}
