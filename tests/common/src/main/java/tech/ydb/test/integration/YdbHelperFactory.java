package tech.ydb.test.integration;

import org.testcontainers.DockerClientFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class YdbHelperFactory {
    private static final YdbHelperFactory INSTANCE = createYdbHelper();

    public static YdbHelperFactory getInstance() {
        return INSTANCE;
    }

    public abstract YdbHelper createHelper(String path);

    private static YdbHelperFactory createYdbHelper() {
        // Check external db
        String ydbDatabase = System.getenv("YDB_DATABASE");
        String ydbEndpoint = System.getenv("YDB_ENDPOINT");
        if (ydbEndpoint != null && !ydbEndpoint.isEmpty() && ydbDatabase != null && !ydbDatabase.isEmpty()) {
            return new ExternalYdb(ydbEndpoint, ydbDatabase);
        }

        // check is docker is availabled
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return new LocalDockerYdb();
        }

        return new DisabledYdb();
    }

    private static class DisabledYdb extends YdbHelperFactory {
        @Override
        public YdbHelper createHelper(String path) {
            return null;
        }
    }

    private static class ExternalYdb extends YdbHelperFactory {
        private final String endpoint;
        private final String database;

        ExternalYdb(String endpoint, String database) {
            this.endpoint = endpoint;
            this.database = database;
        }

        @Override
        public YdbHelper createHelper(String path) {
            return null;
        }
    }

    private static class LocalDockerYdb extends YdbHelperFactory {
        @Override
        public YdbHelper createHelper(String path) {
            return null;
        }
    }
}
