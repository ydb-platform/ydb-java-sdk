package tech.ydb.coordination.settings;

import java.util.Objects;

public class DescribeSemaphoreChanged {
    private final boolean dataChanged;
    private final boolean ownersChanged;

    public DescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
        this.dataChanged = dataChanged;
        this.ownersChanged = ownersChanged;
    }

    public boolean isDataChanged() {
        return dataChanged;
    }

    public boolean isOwnersChanged() {
        return ownersChanged;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DescribeSemaphoreChanged)) {
            return false;
        }
        DescribeSemaphoreChanged that = (DescribeSemaphoreChanged) o;
        return dataChanged == that.dataChanged && ownersChanged == that.ownersChanged;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataChanged, ownersChanged);
    }

    @Override
    public String toString() {
        return "DescribeSemaphoreChanged{" +
                "dataChanged=" + dataChanged +
                ", ownersChanged=" + ownersChanged +
                '}';
    }
}
