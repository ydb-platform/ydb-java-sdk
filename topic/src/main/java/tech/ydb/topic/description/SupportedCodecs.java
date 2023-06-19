package tech.ydb.topic.description;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Nikolay Perfilov
 */
public class SupportedCodecs {
    private final List<Codec> codecs;

    public SupportedCodecs(Builder builder) {
        this.codecs = ImmutableList.copyOf(builder.codecs);
    }

    public List<Codec> getCodecs() {
        return codecs;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private List<Codec> codecs = new ArrayList<>();

        public Builder addCodec(Codec codec) {
            codecs.add(codec);
            return this;
        }

        public Builder setCodecs(List<Codec> supportedCodecs) {
            this.codecs = supportedCodecs;
            return this;
        }

        public SupportedCodecs build() {
            return new SupportedCodecs(this);
        }
    }
}
