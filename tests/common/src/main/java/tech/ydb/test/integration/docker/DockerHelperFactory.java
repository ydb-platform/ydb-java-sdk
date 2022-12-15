package tech.ydb.test.integration.docker;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.integration.utils.PathProxyTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DockerHelperFactory extends YdbHelperFactory {
    private final YdbEnvironment env;
    private final YdbDockerContainer container;

    public DockerHelperFactory(YdbEnvironment env) {
        this.env = env;
        this.container = new YdbDockerContainer(env);
    }

    @Override
    public YdbHelper createHelper() {
        container.start();

        return new YdbHelper() {
            @Override
            public GrpcTransport createTransport(String path) {
                GrpcTransportBuilder builder = GrpcTransport.forEndpoint(endpoint(), container.database());
                if (env.ydbUseTls()) {
                    builder.withSecureConnection(container.pemCert());
                }

                PathProxyTransport proxy = new PathProxyTransport(builder.build(), path, env.cleanUpTests());
                proxy.init();
                return proxy;
            }

            @Override
            public String endpoint() {
                return env.ydbUseTls() ? container.secureEndpoint() : container.nonSecureEndpoint();
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
                // connection to docker container always is anonimous
                return null;
            }

            @Override
            public byte[] pemCert() {
                return container.pemCert();
            }

            @Override
            public void close() {
                if (!env.dockerReuse()) {
                    container.stop();
                    container.close();
                }
            }
        };
    }
}
