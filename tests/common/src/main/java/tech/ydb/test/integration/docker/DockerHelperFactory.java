package tech.ydb.test.integration.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.TestcontainersConfiguration;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.integration.utils.PortsGenerator;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DockerHelperFactory extends YdbHelperFactory {
    private static final Logger logger = LoggerFactory.getLogger(DockerHelperFactory.class);
    private final YdbEnvironment env;
    private final YdbDockerContainer container;

    public DockerHelperFactory(YdbEnvironment env) {
        this(env, new YdbDockerContainer(env, new PortsGenerator()));
    }

    public DockerHelperFactory(YdbEnvironment env, YdbDockerContainer container) {
        this.env = env;
        this.container = container;
        this.container.init();
    }

    @Override
    public YdbHelper createHelper() {
        logger.warn("container start");
        container.start();

        return new YdbHelper() {
            @Override
            public GrpcTransport createTransport() {
                GrpcTransportBuilder builder = GrpcTransport.forEndpoint(endpoint(), container.database());
                if (env.ydbUseTls()) {
                    builder.withSecureConnection(container.pemCert());
                }
                return builder.build();
            }

            @Override
            public String endpoint() {
                EndpointRecord endpoint = env.ydbUseTls() ? container.secureEndpoint() : container.nonSecureEndpoint();
                return endpoint.getHostAndPort();
            }

            @Override
            public String database() {
                return container.database();
            }

            @Override
            public boolean useTls() {
                return env.ydbUseTls();
            }

            @Override
            public String authToken() {
                // connection to docker container is always anonymous
                return null;
            }

            @Override
            public byte[] pemCert() {
                return container.pemCert();
            }

            @Override
            public void close() {
                if (env.dockerReuse() && TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
                    return;
                }

                container.stop();
                container.close();
            }

            @Override
            public String getStdErr() {
                return container.getLogs();
            }
        };
    }
}
