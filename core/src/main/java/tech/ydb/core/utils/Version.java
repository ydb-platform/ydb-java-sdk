package tech.ydb.core.utils;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class Version {

    public static final String UNKNOWN_VERSION = "unknown-version";

    private Version() {
        //
    }

    public static Optional<String> getVersion() {
        try (InputStream in = Version.class.getResourceAsStream("/ydb_sdk_version.properties")) {
            Properties prop = new Properties();
            prop.load(in);
            return Optional.ofNullable(prop.getProperty("version"));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
