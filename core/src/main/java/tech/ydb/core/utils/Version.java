package tech.ydb.core.utils;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class Version {

    public static final String UNKNOWN_VERSION = "unknown-version";

    private Version() {
        //
    }

    public static String getVersion() {
        try {
            Properties prop = new Properties();
            InputStream in = Version.class.getResourceAsStream("/ydb_sdk_version.properties");
            prop.load(in);
            return Optional.ofNullable(prop.getProperty("version")).orElse(UNKNOWN_VERSION);
        } catch (Exception ex) {
            return UNKNOWN_VERSION;
        }
    }
}
