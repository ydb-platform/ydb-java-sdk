package tech.ydb.test.integration;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbEnvironmentTest {
    private final static String ENV_PARAM = "TEST_VAR1";
    private final static String NOT_EXIST_ENV_PARAM = "TEST_VAR2";
    private final static String EMPTY_ENV_PARAM = "TEST_VAR3";
    private final static String BOOLEAN_ENV_PARAM = "TEST_VAR4";
    private final static String WRONG_BOOLEAN_ENV_PARAM = "TEST_VAR5";

    @Test
    public void envDefaultsTests() {
        YdbEnvironment env = new YdbEnvironment();

        // Correct string value from ENV
        Assert.assertEquals("Check exist env param", "var1", env.readParam(ENV_PARAM, "default1"));

        // Incorrect string value from ENV
        Assert.assertEquals("Check not exist env param", "default2", env.readParam(NOT_EXIST_ENV_PARAM, "default2"));
        Assert.assertNull("Check not exist env param" ,env.readParam(NOT_EXIST_ENV_PARAM, null));

        // Empty string value from ENV
        Assert.assertEquals("Check empty env param", "default3", env.readParam(EMPTY_ENV_PARAM, "default3"));
        Assert.assertNull("Check empty env param", env.readParam(EMPTY_ENV_PARAM, null));

        // Correct boolean value from ENV
        Assert.assertTrue("Check exist env param", env.readParam(BOOLEAN_ENV_PARAM, true));
        Assert.assertTrue("Check exist env param", env.readParam(BOOLEAN_ENV_PARAM, false));

        // Incorrect boolean value from ENV
        Assert.assertFalse("Check exist env param", env.readParam(WRONG_BOOLEAN_ENV_PARAM, false));
        Assert.assertFalse("Check exist env param", env.readParam(WRONG_BOOLEAN_ENV_PARAM, true));

        // Empty boolean value from ENV
        Assert.assertFalse("Check exist env param", env.readParam(EMPTY_ENV_PARAM, false));
        Assert.assertTrue("Check exist env param", env.readParam(EMPTY_ENV_PARAM, true));
    }

    @Test
    public void propertiesReadTests() {
        try {
            System.setProperty(ENV_PARAM, "prop1");
            System.setProperty(NOT_EXIST_ENV_PARAM, "prop2");
            System.setProperty(EMPTY_ENV_PARAM, "true");
            System.setProperty(BOOLEAN_ENV_PARAM, "false");

            YdbEnvironment env = new YdbEnvironment();

            // Correct string value - env has higher priority
            Assert.assertEquals("Check exist env param", "var1", env.readParam(ENV_PARAM, "default1"));

            // Incorrect string value - use properites
            Assert.assertEquals("Check not exist env param", "prop2", env.readParam(NOT_EXIST_ENV_PARAM, "default2"));
            Assert.assertEquals("Check not exist env param", "prop2" ,env.readParam(NOT_EXIST_ENV_PARAM, null));

            // Empty string value from ENV
            Assert.assertEquals("Check empty env param", "true", env.readParam(EMPTY_ENV_PARAM, "default3"));
            Assert.assertEquals("Check empty env param", "true", env.readParam(EMPTY_ENV_PARAM, null));

            // Correct boolean value - env has hight priority
            Assert.assertTrue("Check exist env param", env.readParam(BOOLEAN_ENV_PARAM, false));

            // Incorrect boolean value - use properites
            Assert.assertTrue("Check exist env param", env.readParam(EMPTY_ENV_PARAM, false));
        } finally {
            System.getProperties().remove(ENV_PARAM);
            System.getProperties().remove(NOT_EXIST_ENV_PARAM);
            System.getProperties().remove(EMPTY_ENV_PARAM);
            System.getProperties().remove(BOOLEAN_ENV_PARAM);
        }
    }

    @Test
    public void rewriteAllParams() {
        Map<String, String> params = new HashMap<>();
        params.put("YDB_ENDPOINT", "endpoint");
        params.put("YDB_DATABASE", "database");
        params.put("YDB_TOKEN", "token");
        params.put("YDB_PEM_CERT", "pemCert");
        params.put("YDB_USE_TLS", "tRuE");

        params.put("YDB_DOCKER_IMAGE", "otherImager");
        params.put("YDB_DOCKER_DATABASE", "/remote");
        params.put("YDB_DOCKER_PEM_PATH", "/certs/ca.pem");
        params.put("YDB_DOCKER_REUSE", "false");

        params.put("YDB_CLEAN_UP", "tue");
        params.put("YDB_DISABLE_INTEGRATION_TESTS", "false");

        try {
            for (Map.Entry<String, String> entry: params.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }

            YdbEnvironment env = new YdbEnvironment();

            Assert.assertEquals("check YDB_ENDPOINT", "endpoint", env.ydbEndpoint());
            Assert.assertEquals("check YDB_DATABASE", "database", env.ydbDatabase());
            Assert.assertEquals("check YDB_TOKEN", "token", env.ydbAuthToken());
            Assert.assertEquals("check YDB_USE_TLS", true, env.ydbUseTls());
            Assert.assertEquals("check YDB_PEM_CERT", "pemCert", env.ydbPemCert());

            Assert.assertEquals("check YDB_DOCKER_IMAGE", "otherImager", env.dockerImage());
            Assert.assertEquals("check YDB_DOCKER_DATABASE", "/remote", env.dockerDatabase());
            Assert.assertEquals("check YDB_DOCKER_PEM_PATH", "/certs/ca.pem", env.dockerPemPath());
            Assert.assertEquals("check YDB_DOCKER_REUSE", false, env.dockerReuse());

            Assert.assertEquals("check YDB_CLEAN_UP", false, env.cleanUpTests());
            // ENV has higher priority
            Assert.assertEquals("check YDB_DISABLE_INTEGRATION_TESTS", true, env.disableIntegrationTests());
        } finally {
            for (String key: params.keySet()) {
                System.getProperties().remove(key);
            }
        }
    }
}
