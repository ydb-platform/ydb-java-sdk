package tech.ydb.core.impl.call;

import java.util.function.IntConsumer;

import tech.ydb.core.grpc.GrpcFlowControl;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcFlows {
    public static final GrpcFlowControl SIMPLE_FLOW = SimpleCall::new;

    private GrpcFlows() { }

    private static class SimpleCall implements GrpcFlowControl.Call {
        private final IntConsumer req;

        SimpleCall(IntConsumer req) {
            this.req = req;
        }

        @Override
        public void onStart() {
            req.accept(1);
        }

        @Override
        public void onMessageReaded() {
            req.accept(1);
        }
    }
}
