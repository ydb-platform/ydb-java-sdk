package tech.ydb.topic.utils;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;

/**
 * Class for convert codec from ydb proto to vice versa
 *
 * @author Nikolay Perfilov
 */
public class ProtoUtils {

    private ProtoUtils() { }

    /**
     * Convert codec id from SDK to YDB proto data
     *
     * @param codec codec identifier
     * @return ydb proto id
     */
    public static int toProto(int codec) {
        switch (codec) {
            case Codec.RAW:
                return YdbTopic.Codec.CODEC_RAW_VALUE;
            case Codec.GZIP:
                return YdbTopic.Codec.CODEC_GZIP_VALUE;
            case Codec.LZOP:
                return YdbTopic.Codec.CODEC_LZOP_VALUE;
            case Codec.ZSTD:
                return YdbTopic.Codec.CODEC_ZSTD_VALUE;
            case Codec.CUSTOM:
                return YdbTopic.Codec.CODEC_CUSTOM_VALUE;
            default:
                if (codec  > 10000) {
                    return codec;
                } else {
                    throw new RuntimeException("Cannot convert codec to proto. Unknown codec value: " + codec);
                }
        }
    }

    /**
     * Convert proto codec to SDK id
     *
     * @param codec codec identifier form proto
     * @return SDK id
     */
    public static int codecFromProto(int codec) {
        switch (codec) {
            case YdbTopic.Codec.CODEC_RAW_VALUE:
                return Codec.RAW;
            case YdbTopic.Codec.CODEC_GZIP_VALUE:
                return Codec.GZIP;
            case YdbTopic.Codec.CODEC_LZOP_VALUE:
                return Codec.LZOP;
            case YdbTopic.Codec.CODEC_ZSTD_VALUE:
                return Codec.ZSTD;
            case YdbTopic.Codec.CODEC_CUSTOM_VALUE:
                return Codec.CUSTOM;
            default:
                if (codec  > 10000) {
                    return codec;
                } else {
                    throw new RuntimeException("Unknown codec value from proto: " + codec);
                }
        }
    }
}
