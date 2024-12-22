package tech.ydb.test.integration;

import com.google.common.annotations.VisibleForTesting;
import org.testcontainers.shaded.com.google.common.base.Supplier;
import org.testcontainers.shaded.com.google.common.base.Suppliers;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbEnvironment {
    private static final String YDB_DEFAULT_IMAGE = "ydbplatform/local-ydb:latest";

    private final Supplier<String> ydbEndpoint = createParam("YDB_ENDPOINT", null);
    private final Supplier<String> ydbDatabase = createParam("YDB_DATABASE", null);
    private final Supplier<String> ydbAuthToken = createParam("YDB_TOKEN", null);
    private final Supplier<String> ydbPemCert = createParam("YDB_PEM_CERT", null);
    private final Supplier<Boolean> ydbUseTls = createParam("YDB_USE_TLS", false);

    private final Supplier<String> dockerImage = createParam("YDB_DOCKER_IMAGE", YDB_DEFAULT_IMAGE);
    private final Supplier<String> dockerDatabase = createParam("YDB_DOCKER_DATABASE", "/local");
    private final Supplier<String> dockerPemPath = createParam("YDB_DOCKER_PEM_PATH", "/ydb_certs/ca.pem");
    private final Supplier<String> dockerFeatures = createParam("YDB_DOCKER_FEATURE_FLAGS", "");
    private final Supplier<Boolean> dockerReuse = createParam("YDB_DOCKER_REUSE", true);

    private final Supplier<Boolean> cleanUpTests = createParam("YDB_CLEAN_UP", true);
    private final Supplier<Boolean> disableIntegrationTests = createParam("YDB_DISABLE_INTEGRATION_TESTS", false);
    private final Supplier<Boolean> useDockerIsolation = createParam("YDB_DOCKER_ISOLATION", false);

    public String ydbEndpoint() {
        return ydbEndpoint.get();
    }

    public String ydbDatabase() {
        return ydbDatabase.get();
    }

    public String ydbAuthToken() {
        return ydbAuthToken.get();
    }

    public String ydbPemCert() {
        return ydbPemCert.get();
    }

    public boolean ydbUseTls() {
        return ydbUseTls.get();
    }

    public String dockerImage() {
        return dockerImage.get();
    }

    public String dockerDatabase() {
        return dockerDatabase.get();
    }

    public String dockerPemPath() {
        return dockerPemPath.get();
    }

    public boolean dockerReuse() {
        return dockerReuse.get();
    }

    public String dockerFeatures() {
        return dockerFeatures.get();
    }

    public boolean cleanUpTests() {
        return cleanUpTests.get();
    }

    public boolean disableIntegrationTests() {
        return disableIntegrationTests.get();
    }

    public boolean useDockerIsolation() {
        return useDockerIsolation.get();
    }

    private Supplier<String> createParam(String key, String defaultValue) {
        return Suppliers.memoize(() -> readParam(key, defaultValue));
    }

    private Supplier<Boolean> createParam(String key, boolean defaultValue) {
        return Suppliers.memoize(() -> readParam(key, defaultValue));
    }

    @VisibleForTesting
    static String readParam(String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return System.getProperty(key, defaultValue);
    }

    @VisibleForTesting
    static Boolean readParam(String key, boolean defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) {
            return Boolean.valueOf(env);
        }

        env = System.getProperty(key);
        if (env != null && !env.isEmpty()) {
            return Boolean.valueOf(env);
        }

        return defaultValue;
    }
}
