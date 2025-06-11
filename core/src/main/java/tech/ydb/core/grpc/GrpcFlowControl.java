package tech.ydb.core.grpc;

import java.util.function.IntConsumer;

/**
 *
 * @author Aleksandr Gorshenin
 */
@FunctionalInterface
public interface GrpcFlowControl {
    interface Call {
        void onStart();
        void onMessageReaded();
    }

    Call newCall(IntConsumer req);
}
