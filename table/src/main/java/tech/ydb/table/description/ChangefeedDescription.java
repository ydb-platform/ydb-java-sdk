package tech.ydb.table.description;

import java.time.Duration;
import java.util.Objects;

import tech.ydb.table.settings.Changefeed;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ChangefeedDescription {
    public enum State {
        /**
         * Normal state, from this state changefeed can be disabled
         */
        ENABLED,
        /**
         * No new change records are generated, but the old ones remain available<br>
         * From this state changefeed cannot be switched to any other state
         */
        DISABLED,
        /**
         * An initial scan is being performed. <br> After its completion changefeed will switch to the normal state
         */
        INITIAL_SCAN;
    }

    private final String name;
    private final Changefeed.Mode mode;
    private final Changefeed.Format format;
    private final State state;
    private final boolean virtualTimestamps;
    private final Duration resolvedTimestampsInterval;

    public ChangefeedDescription(
        String name,
        Changefeed.Mode mode,
        Changefeed.Format format,
        State state,
        boolean virtualTimestamps,
        Duration resolvedTimestampsInterval
    ) {
        this.name = name;
        this.mode = mode;
        this.format = format;
        this.state = state;
        this.virtualTimestamps = virtualTimestamps;
        this.resolvedTimestampsInterval = resolvedTimestampsInterval;
    }

    /**
     * @deprecated use constructor with resolvedTimestampsInterval instead
     */
    @Deprecated
    public ChangefeedDescription(
        String name,
        Changefeed.Mode mode,
        Changefeed.Format format,
        State state,
        boolean virtualTimestamps
    ) {
        this(name, mode, format, state, virtualTimestamps, null);
    }

    /**
     * @return Name of the feed
     */
    public String getName() {
        return name;
    }

    /**
     * @return Mode specifies the information that will be written to the feed
     */
    public Changefeed.Mode getMode() {
        return mode;
    }

    /**
     * @return Format of the data
     */
    public Changefeed.Format getFormat() {
        return format;
    }

    /**
     * @return State of the feed
     */
    public State getState() {
        return state;
    }

    /**
     * @return State of emitting of virtual timestamps along with data
     */
    public boolean hasVirtualTimestamps() {
        return virtualTimestamps;
    }

    /**
     * @return Heartbeat interval
     */
    public Duration getResolvedTimestampsInterval() {
        return resolvedTimestampsInterval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mode, format, state, virtualTimestamps, resolvedTimestampsInterval);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChangefeedDescription cd = (ChangefeedDescription) o;
        return Objects.equals(name, cd.name)
                && mode == cd.mode
                && format == cd.format
                && state == cd.state
                && virtualTimestamps == cd.virtualTimestamps
                && Objects.equals(resolvedTimestampsInterval, cd.resolvedTimestampsInterval);
    }

    @Override
    public String toString() {
        return new StringBuilder("Changefeed['").append(name)
                .append("']{state=").append(state)
                .append(", format=").append(format)
                .append(", mode=").append(mode)
                .append(", virtual timestamps=").append(virtualTimestamps)
                .append(", resolved timestamps=").append(
                    resolvedTimestampsInterval != null? resolvedTimestampsInterval : "null")
                .append("}").toString();
    }
}
