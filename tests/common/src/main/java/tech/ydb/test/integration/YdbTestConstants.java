package tech.ydb.test.integration;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbTestConstants {
    public static final String YDB_IMAGE = init("YDB_DOCKER_IMAGE", "cr.yandex/yc/yandex-docker-local-ydb:latest");
    public static final String YDB_DATABASE = init("YDB_DOCKER_DATABASE", "/local");
    public static final String YDB_PEM_PATH = init("YDB_DOCKER_PEM_PATH", "/ydb_certs/ca.pem");

    private YdbTestConstants() { }

    private static String init(String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && env.isEmpty()) {
            return env;
        }
        return System.getProperty(env, defaultValue);
    }
}
