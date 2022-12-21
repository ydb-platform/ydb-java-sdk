package tech.ydb.test.integration.utils;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.integration.YdbHelper;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class ProxyYdbHelper implements YdbHelper {

    protected abstract YdbHelper origin();

    private YdbHelper checked() {
        YdbHelper check = origin();
        if (check == null) {
            throw new NullPointerException("Can't proxy method of null");
        }
        return check;
    }

    @Override
    public GrpcTransport createTransport(String path) {
        return checked().createTransport(path);
    }

    @Override
    public String endpoint() {
        return checked().endpoint();
    }

    @Override
    public String database() {
        return checked().database();
    }

    @Override
    public boolean useTls() {
        return checked().useTls();
    }

    @Override
    public byte[] pemCert() {
        return checked().pemCert();
    }

    @Override
    public String authToken() {
        return checked().authToken();
    }

    @Override
    public void close() {
        // Usally origin helper must be closed by its owner
    }
}
