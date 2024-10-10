package tech.ydb.test.integration;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbEnvironmentMock extends YdbEnvironment {
    private String database = null;
    private String endpoint = null;
    private String pemCert = null;
    private String token = null;
    private String features = null;
    private boolean useTLS = false;
    private boolean dockerReuse = false;
    private boolean dockerIsolation = false;
    private boolean disabledTests = false;

    public YdbEnvironmentMock withDatabase(String value) {
        this.database = value;
        return this;
    }

    public YdbEnvironmentMock withEndpoint(String value) {
        this.endpoint = value;
        return this;
    }

    public YdbEnvironmentMock withPemCert(String value) {
        this.pemCert = value;
        return this;
    }

    public YdbEnvironmentMock withToken(String value) {
        this.token = value;
        return this;
    }

    public YdbEnvironmentMock withUseTLS(boolean value) {
        this.useTLS = value;
        return this;
    }

    public YdbEnvironmentMock withDockerReuse(boolean value) {
        this.dockerReuse = value;
        return this;
    }

    public YdbEnvironmentMock withFeatures(String features) {
        this.features = features;
        return this;
    }

    public YdbEnvironmentMock withDockerIsolation(boolean value) {
        this.dockerIsolation = value;
        return this;
    }

    public YdbEnvironmentMock withTestDisabled(boolean value) {
        this.disabledTests = value;
        return this;
    }

    @Override
    public String ydbDatabase() {
        return database;
    }

    @Override
    public String ydbEndpoint() {
        return endpoint;
    }

    @Override
    public String ydbPemCert() {
        return pemCert;
    }

    @Override
    public String ydbAuthToken() {
        return token;
    }

    @Override
    public boolean ydbUseTls() {
        return useTLS;
    }

    @Override
    public boolean dockerReuse() {
        return dockerReuse;
    }

    @Override
    public String dockerFeatures() {
        return features;
    }

    @Override
    public boolean useDockerIsolation() {
        return dockerIsolation;
    }

    @Override
    public boolean disableIntegrationTests() {
        return disabledTests;
    }
}
