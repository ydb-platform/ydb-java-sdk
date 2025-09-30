package tech.ydb.table.description;

import javax.annotation.Nullable;

/**
 * @author Kirill Kurdyukov
 */
public class SequenceDescription {

    private final String name;
    @Nullable
    private final Long minValue;
    @Nullable
    private final Long maxValue;
    @Nullable
    private final Long startValue;
    @Nullable
    private final Long cache;
    @Nullable
    private final Long increment;
    @Nullable
    private final Boolean cycle;

    private SequenceDescription(Builder builder) {
        this.name = builder.name;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.startValue = builder.startValue;
        this.cache = builder.cache;
        this.increment = builder.increment;
        this.cycle = builder.cycle;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Long getMinValue() {
        return minValue;
    }

    @Nullable
    public Long getMaxValue() {
        return maxValue;
    }

    @Nullable
    public Long getStartValue() {
        return startValue;
    }

    @Nullable
    public Long getCache() {
        return cache;
    }

    @Nullable
    public Long getIncrement() {
        return increment;
    }

    @Nullable
    public Boolean getCycle() {
        return cycle;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "sequence_default";
        private Long minValue;
        private Long maxValue;
        private Long startValue;
        private Long cache;
        private Long increment;
        private Boolean cycle;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setMinValue(@Nullable Long minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder setMaxValue(@Nullable Long maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder setStartValue(@Nullable Long startValue) {
            this.startValue = startValue;
            return this;
        }

        public Builder setCache(@Nullable Long cache) {
            this.cache = cache;
            return this;
        }

        public Builder setIncrement(@Nullable Long increment) {
            this.increment = increment;
            return this;
        }

        public Builder setCycle(@Nullable Boolean cycle) {
            this.cycle = cycle;
            return this;
        }

        public SequenceDescription build() {
            if (name == null) {
                throw new IllegalStateException("name is required");
            }

            return new SequenceDescription(this);
        }
    }
}
