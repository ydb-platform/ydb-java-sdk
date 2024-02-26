package tech.ydb.coordination.description;

import tech.ydb.proto.coordination.SessionResponse;

public class SemaphoreChangedEvent {
    private final boolean dataChanged;
    private final boolean ownersChanged;

    public SemaphoreChangedEvent(SessionResponse.DescribeSemaphoreChanged event) {
        this.dataChanged = event.getDataChanged();
        this.ownersChanged = event.getOwnersChanged();
    }

    public boolean isDataChanged() {
        return dataChanged;
    }

    public boolean isOwnersChanged() {
        return ownersChanged;
    }
}
