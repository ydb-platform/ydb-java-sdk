package tech.ydb.query.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ApacheArrowFormat {
    private static final ApacheArrowFormat NONE = new ApacheArrowFormat(CompressionCodec.NONE, 0);
    // lz4 doesn't support compressionLevel
    private static final ApacheArrowFormat LZ4 = new ApacheArrowFormat(CompressionCodec.LZ4_FRAME, 0);
    // default compression level
    private static final ApacheArrowFormat ZSTD = new ApacheArrowFormat(CompressionCodec.ZSTD, 3);

    public enum CompressionCodec {
        NONE,
        ZSTD,
        LZ4_FRAME,
    }

    private final CompressionCodec codec;
    private final int compressionLevel;

    private ApacheArrowFormat(CompressionCodec codec, int compressionLevel) {
        this.codec = codec;
        this.compressionLevel = compressionLevel;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public CompressionCodec getCodec() {
        return codec;
    }

    public static ApacheArrowFormat noCompression() {
        return NONE;
    }

    public static ApacheArrowFormat lz4Frame() {
        return LZ4;
    }

    public static ApacheArrowFormat zstd() {
        return ZSTD;
    }

    public static ApacheArrowFormat zstd(int compressionLevel) {
        return new ApacheArrowFormat(CompressionCodec.ZSTD, compressionLevel);
    }
}
