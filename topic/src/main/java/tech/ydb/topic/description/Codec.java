package tech.ydb.topic.description;

/**
 * @author Nikolay Perfilov
 */
public class Codec {
    public static final int RAW = 1;
    public static final int GZIP = 2;
    public static final int LZOP = 3;
    public static final int ZSTD = 4;
    public static final int CUSTOM = 10000;

    private Codec() {
    }

}
