package tech.ydb.test.integration;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbEnvironmentMock extends YdbEnvironment {
    private final Map<String, String> params = new HashMap<>();

    public YdbEnvironmentMock with(String key, String value) {
        params.put(key, value);
        return this;
    }

    @Override
    String readParam(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    @Override
    Boolean readParam(String key, boolean defaultValue) {
        if (params.containsKey(key)) {
            return Boolean.valueOf(params.get(key));
        }
        return defaultValue;
    }

    public static YdbEnvironmentMock create() {
        return new YdbEnvironmentMock();
    }
}
