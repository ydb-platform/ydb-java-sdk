package tech.ydb.topic.utils;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegister;
import tech.ydb.topic.description.TopicCodec;

/**
 * @author Nikolay Perfilov
 */
public class ProtoUtils {

    private ProtoUtils() { }

    public static int toProto(TopicCodec codec) {
       /* switch (codec) {
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
        }*/
        return codec.getId();
    }

    public static TopicCodec codecFromProto(int codec) {
        TopicCodec topicCodec = CodecRegister.getInstance().get(codec);
        if(topicCodec != null ) {
            return topicCodec;
        }

        if(CodecRegister.getInstance().isCustomCodec(codec)) {
            throw new RuntimeException("Unknown codec value from proto: " + codec);
        } else {
            throw new RuntimeException("Not registered custom codec");
        }
    }
}
