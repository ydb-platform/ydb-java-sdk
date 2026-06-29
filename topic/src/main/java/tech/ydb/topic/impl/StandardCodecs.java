package tech.ydb.topic.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.description.Codec;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class StandardCodecs {
    private static final Logger logger = LoggerFactory.getLogger(StandardCodecs.class);

    private static final Codec RAW;
    private static final Codec GZIP;
    private static final Codec ZSTD;
    private static final Codec LZOP;

    private static final List<Codec> AVAILABLE;

    static {
        Codec rawCodec = null;
        Codec gzipCodec = null;
        Codec zstdCodec = null;
        Codec lzopCodec = null;

        try {
            rawCodec = RawCodec.getInstance();
        } catch (Throwable th) {
            logger.error("cannot initialize RAW codec", th);
        }

        try {
            gzipCodec = GzipCodec.getInstance();
        } catch (Throwable th) {
            logger.error("cannot initialize GZIP codec", th);
        }

        try {
            zstdCodec = ZstdCodec.getInstance();
        } catch (Throwable th) {
            logger.debug("cannot initialize ZSTD codec, trying backward compatible", th);
            try {
                zstdCodec = ZstdBackwardCodec.getInstance();
            } catch (Throwable th2) {
                logger.debug("cannot initialize ZSTD codec", th2);
            }
        }

        try {
            lzopCodec = LzopCodec.getInstance();
        } catch (Throwable th) {
            logger.debug("cannot initialize LZOP codec", th);
        }

        RAW = rawCodec;
        GZIP = gzipCodec;
        ZSTD = zstdCodec;
        LZOP = lzopCodec;

        AVAILABLE = Collections.unmodifiableList(
                Arrays.asList(RAW, GZIP, ZSTD, LZOP).stream().filter(c -> c != null).collect(Collectors.toList())
        );
    }

    private StandardCodecs() { }

    public static Codec getRawCodec() {
        return RAW;
    }

    public static Codec getGzipCodec() {
        return GZIP;
    }

    public static Codec getZstdCodec() {
        return ZSTD;
    }

    public static Codec getLzopCodec() {
        return LZOP;
    }

    public static Collection<Codec> getAvailableCodecs() {
        return AVAILABLE;
    }
}
