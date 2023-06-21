package tech.ydb.core.settings;

/**
 * @author Kirill Kurdyukov
 */
public class LongOperationSettings extends OperationSettings {

    private final Mode mode;

    public LongOperationSettings(LongOperationBuilder<?> builder) {
        super(builder);

        this.mode = builder.mode;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public static class LongOperationBuilder<Self extends LongOperationBuilder<?>> extends OperationBuilder<Self> {

        private Mode mode = Mode.ASYNC;

        public Self setOperationMode(Mode mode) {
            this.mode = mode;

            return self();
        }

        @Override
        public OperationSettings build() {
            return new OperationSettings(this);
        }
    }
}
