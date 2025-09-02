package tech.ydb.core.impl.call;

import io.grpc.Metadata;
import io.grpc.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface GrpcStatusHandler {
    void accept(Status status, Metadata trailers);

    void postComplete();
}
