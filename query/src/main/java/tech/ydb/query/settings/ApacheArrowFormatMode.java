package tech.ydb.query.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ApacheArrowFormatMode {
    public enum CompressionCodec {
        NONE,
        ZSTD,
        LZ4_FRAME,
    }

    private final CompressionCodec codec;
    private final int compressionLevel;

    private ApacheArrowFormatMode(CompressionCodec codec, int compressionLevel) {
        this.codec = codec;
        this.compressionLevel = compressionLevel;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public CompressionCodec getCodec() {
        return codec;
    }

    public static ApacheArrowFormatMode noCompression() {
        return new ApacheArrowFormatMode(CompressionCodec.NONE, 0);
    }

    public static ApacheArrowFormatMode lz4Frame() {
        // lz4 doesn't support compressionLevel
        return new ApacheArrowFormatMode(CompressionCodec.LZ4_FRAME, 0);
    }

    public static ApacheArrowFormatMode zstd() {
        // default compression level
        return new ApacheArrowFormatMode(CompressionCodec.ZSTD, 3);
    }

    public static ApacheArrowFormatMode zstd(int compressionLevel) {
        return new ApacheArrowFormatMode(CompressionCodec.ZSTD, compressionLevel);
    }
}
