package tech.ydb.topic.utils;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;

/**
 * @author Nikolay Perfilov
 */
public class ProtoUtils {

    private ProtoUtils() { }

    public static int toProto(Codec codec) {
        switch (codec) {
            case RAW:
                return YdbTopic.Codec.CODEC_RAW_VALUE;
            case GZIP:
                return YdbTopic.Codec.CODEC_GZIP_VALUE;
            case LZOP:
                return YdbTopic.Codec.CODEC_LZOP_VALUE;
            case ZSTD:
                return YdbTopic.Codec.CODEC_ZSTD_VALUE;
            case CUSTOM:
                return YdbTopic.Codec.CODEC_CUSTOM_VALUE;
            default:
                throw new RuntimeException("Cannot convert codec to proto. Unknown codec value: " + codec);
        }
    }

    public static Codec codecFromProto(int codec) {
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
                throw new RuntimeException("Unknown codec value from proto: " + codec);
        }
    }
}
