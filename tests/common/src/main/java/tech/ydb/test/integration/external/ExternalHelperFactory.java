package tech.ydb.test.integration.external;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExternalHelperFactory extends YdbHelperFactory {

    private final YdbEnvironment env;

    public ExternalHelperFactory(YdbEnvironment env) {
        this.env = env;
    }

    @Override
    public YdbHelper createHelper() {
        return new YdbHelper() {
            @Override
            public GrpcTransport createTransport() {
                GrpcTransportBuilder builder = GrpcTransport.forEndpoint(endpoint(), database());

                if (authToken() != null) {
                    builder.withAuthProvider(new TokenAuthProvider(authToken()));
                }
                if (useTls()) {
                    builder.withSecureConnection(pemCert());
                }

                return builder.build();
            }

            @Override
            public String endpoint() {
                return env.ydbEndpoint();
            }

            @Override
            public String database() {
                return env.ydbDatabase();
            }

            @Override
            public boolean useTls() {
                return env.ydbUseTls();
            }

            @Override
            public String authToken() {
                return env.ydbAuthToken();
            }

            @Override
            public byte[] pemCert() {
                if (env.ydbPemCert() != null) {
                    try {
                        return Files.asByteSource(new File(env.ydbPemCert())).read();
                    } catch (IOException ex) {
                        logger.warn("can't read pem cert {}", env.ydbPemCert(), ex);
                    }
                }
                return null;
            }

            @Override
            public void close() {
                // Nothing
            }

            @Override
            public String getStdErr() {
                return "EXTERNAL";
            }
        };
    }
}
