package tech.ydb.topic.description;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Nikolay Perfilov
 */
public class SupportedCodecs {
    private final List<TopicCodec> codecs;

    public SupportedCodecs(Builder builder) {
        this.codecs = ImmutableList.copyOf(builder.codecs);
    }

    public SupportedCodecs(List<TopicCodec> codecs) {
        this.codecs = codecs;
    }

    public List<TopicCodec> getCodecs() {
        return codecs;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private List<TopicCodec> codecs = new ArrayList<>();

        public Builder addCodec(TopicCodec codec) {
            codecs.add(codec);
            return this;
        }

        public Builder setCodecs(List<TopicCodec> supportedCodecs) {
            this.codecs = supportedCodecs;
            return this;
        }

        public SupportedCodecs build() {
            return new SupportedCodecs(this);
        }
    }
}
