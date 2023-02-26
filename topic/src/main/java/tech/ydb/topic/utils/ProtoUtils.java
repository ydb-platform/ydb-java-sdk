package tech.ydb.topic.utils;

import tech.ydb.topic.YdbTopic;
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
                return  YdbTopic.Codec.CODEC_GZIP_VALUE;
            case LZOP:
                return  YdbTopic.Codec.CODEC_LZOP_VALUE;
            case ZSTD:
                return  YdbTopic.Codec.CODEC_ZSTD_VALUE;
            case CUSTOM:
                return  YdbTopic.Codec.CODEC_CUSTOM_VALUE;
            default:
                throw new IllegalArgumentException("Unknown codec value: " + codec);
        }
    }
}
