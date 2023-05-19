package tech.ydb.core.grpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum GrpcCompression {
    NO_COMPRESSION(null),
    GZIP("gzip");

    private final String compressor;

    GrpcCompression(String compressor) {
        this.compressor = compressor;
    }

    public String compressor() {
        return this.compressor;
    }

}
