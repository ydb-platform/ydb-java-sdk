package tech.ydb.core.operation;

import tech.ydb.OperationProtos;

/**
 * @author Kirill Kurdyukov
 */
public enum OperationMode {
    SYNC, ASYNC;

    public OperationProtos.OperationParams.OperationMode toProto() {
        switch (this) {
            case SYNC:
                return OperationProtos.OperationParams.OperationMode.SYNC;
            case ASYNC:
                return OperationProtos.OperationParams.OperationMode.ASYNC;
            default:
                throw new RuntimeException("Unsupported operation mode");
        }
    }
}
