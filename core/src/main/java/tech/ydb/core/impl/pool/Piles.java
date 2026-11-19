package tech.ydb.core.impl.pool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tech.ydb.proto.bridge.YdbBridgeCommon;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class Piles implements PriorityPicker {
    public static final Piles EMPTY = new Piles(Collections.emptyList());

    private static final int PRIMARY_PRIORITY = 0;
    private static final int SYNCHRONIZED_PRIORITY = 1000;
    private static final int OTHER_PRIORITY = 2000;

    private final Map<String, Integer> piles = new HashMap<>();

    public Piles(Collection<YdbBridgeCommon.PileState> pileStates) {
        for (YdbBridgeCommon.PileState pile: pileStates) {
            switch (pile.getState()) {
                case PRIMARY:
                case PROMOTED:
                    piles.put(pile.getPileName(), PRIMARY_PRIORITY);
                    break;
                case SYNCHRONIZED:
                    piles.put(pile.getPileName(), SYNCHRONIZED_PRIORITY);
                    break;
                case NOT_SYNCHRONIZED:
                case UNSPECIFIED:
                case SUSPENDED:
                case DISCONNECTED:
                case UNRECOGNIZED:
                default:
                    piles.put(pile.getPileName(), OTHER_PRIORITY);
                    break;
            }
        }
    }

    @Override
    public int getPriority(EndpointRecord endpoint) {
        if (endpoint.getPile() == null) {
            return PRIMARY_PRIORITY;
        }
        return piles.getOrDefault(endpoint.getPile(), OTHER_PRIORITY);
    }
}
