package tech.ydb.test.integration.docker;


import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.testcontainers.utility.TestcontainersConfiguration;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ProxedDockerHelperFactory extends YdbHelperFactory {
    private final YdbEnvironment env;
    private final YdbDockerContainer container;

    public ProxedDockerHelperFactory(YdbEnvironment env) {
        this(env, new YdbDockerContainer(env, null));
    }

    public ProxedDockerHelperFactory(YdbEnvironment env, YdbDockerContainer container) {
        this.env = env;
        this.container = container;
        this.container.init();
    }

    @Override
    public YdbHelper createHelper() {
        container.start();

        final ManagedChannel channel = Grpc
                .newChannelBuilder(container.nonSecureEndpoint().getHostAndPort(), InsecureChannelCredentials.create())
                .build();
        final GrpcProxyServer server = new GrpcProxyServer(channel, 0);

        return new YdbHelper() {
            @Override
            public GrpcTransport createTransport() {
                return GrpcTransport.forEndpoint(endpoint(), container.database()).build();
            }

            @Override
            public String endpoint() {
                return server.endpoint();
            }

            @Override
            public String database() {
                return container.database();
            }

            @Override
            public boolean useTls() {
                // connection to grpc proxy is always insecure
                return false;
            }

            @Override
            public String authToken() {
                // connection to grpc proxy is always anonymous
                return null;
            }

            @Override
            public byte[] pemCert() {
                // connection to grpc proxy is always insecure
                return null;
            }

            @Override
            public void close() {
                server.close();

                if (env.dockerReuse() && TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
                    return;
                }

                container.stop();
                container.close();
            }
        };
    }
}

