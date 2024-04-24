package tech.ydb.test.integration;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import tech.ydb.test.integration.docker.DockerHelperFactory;
import tech.ydb.test.integration.docker.ProxedDockerHelperFactory;
import tech.ydb.test.integration.external.ExternalHelperFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class YdbHelperFactory {
    protected static final Logger logger = LoggerFactory.getLogger(YdbHelperFactory.class);

    public static YdbHelperFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @VisibleForTesting
    static YdbHelperFactory createYdbHelper(YdbEnvironment env) {
        if (env.disableIntegrationTests()) {
            logger.info("ydb helper is disabled");
            return new DisabledFactory();
        }

        // Check external db
        String ydbDatabase = env.ydbDatabase();
        String ydbEndpoint = env.ydbEndpoint();
        if (ydbEndpoint != null && !ydbEndpoint.isEmpty() && ydbDatabase != null && !ydbDatabase.isEmpty()) {
            logger.info("create external ydb helper with endpoint {} and database {}", ydbEndpoint, ydbDatabase);
            return new ExternalHelperFactory(env);
        }

        // check if docker is availabled
        if (DockerClientFactory.instance().isDockerAvailable()) {
            logger.info("setup docker-based ydb helper");
            if (env.useDockerIsolation()) {
                return new ProxedDockerHelperFactory(env);
            } else {
                return new DockerHelperFactory(env);
            }
        }

        logger.info("ydb helper is disabled");
        return new DisabledFactory();
    }

    public abstract YdbHelper createHelper();

    public boolean isEnabled() {
        return true;
    }

    private static class SingletonHelper {
        private static final YdbHelperFactory INSTANCE = createYdbHelper(new YdbEnvironment());
    }

    private static class DisabledFactory extends YdbHelperFactory {
        @Override
        public YdbHelper createHelper() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }
}
