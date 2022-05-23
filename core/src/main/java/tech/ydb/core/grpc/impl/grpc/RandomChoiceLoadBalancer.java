/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.ydb.core.grpc.impl.grpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.ConnectivityState.CONNECTING;
import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.SHUTDOWN;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;

/**
 * A {@link LoadBalancer} that provides random choice load-balancing over the {@link
 * EquivalentAddressGroup}s from the {@link io.grpc.NameResolver}.
 * copy of RoundRobinLoadBalancer (version 1.26.0) with random choice changes and without stickiness logic -
 * https://github.com/grpc/grpc-java/blob/master/core/src/main/java/io/grpc/util/RoundRobinLoadBalancer.java
 * @author Nikolay Perfilov
 */
final class RandomChoiceLoadBalancer extends LoadBalancer {
    private final static Logger logger = LoggerFactory.getLogger(RandomChoiceLoadBalancer.class);

    @VisibleForTesting
    static final Attributes.Key<Ref<ConnectivityStateInfo>> STATE_INFO =
            Attributes.Key.create("state-info");

    private final Helper helper;
    private final Map<EquivalentAddressGroup, Subchannel> subchannels =
            new HashMap<>();

    private ConnectivityState currentState;
    private RandomChoicePicker currentPicker = new EmptyPicker(EMPTY_OK);

    RandomChoiceLoadBalancer(Helper helper) {
        this.helper = checkNotNull(helper, "helper");
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        List<EquivalentAddressGroup> servers = resolvedAddresses.getAddresses();
        Set<EquivalentAddressGroup> currentAddrs = subchannels.keySet();
        Map<EquivalentAddressGroup, EquivalentAddressGroup> latestAddrs = stripAttrs(servers);
        Set<EquivalentAddressGroup> removedAddrs = setsDifference(currentAddrs, latestAddrs.keySet());
        logger.debug(String.format("handle resolved address - %d latest addresses",  latestAddrs.size()));

        for (Map.Entry<EquivalentAddressGroup, EquivalentAddressGroup> latestEntry :
                latestAddrs.entrySet()) {
            EquivalentAddressGroup strippedAddressGroup = latestEntry.getKey();
            EquivalentAddressGroup originalAddressGroup = latestEntry.getValue();
            Subchannel existingSubchannel = subchannels.get(strippedAddressGroup);
            if (existingSubchannel != null) {
                // EAG's Attributes may have changed.
                existingSubchannel.updateAddresses(Collections.singletonList(originalAddressGroup));
                continue;
            }
            // Create new subchannels for new addresses.

            // NB(lukaszx0): we don't merge `attributes` with `subchannelAttr` because subchannel
            // doesn't need them. They're describing the resolved server list but we're not taking
            // any action based on this information.
            Attributes.Builder subchannelAttrs = Attributes.newBuilder()
                    // NB(lukaszx0): because attributes are immutable we can't set new value for the key
                    // after creation but since we can mutate the values we leverage that and set
                    // AtomicReference which will allow mutating state info for given channel.
                    .set(STATE_INFO,
                            new Ref<>(ConnectivityStateInfo.forNonError(IDLE)));

            final Subchannel subchannel = checkNotNull(
                    helper.createSubchannel(CreateSubchannelArgs.newBuilder()
                            .setAddresses(originalAddressGroup)
                            .setAttributes(subchannelAttrs.build())
                            .build()),
                    "subchannel");
            subchannel.start(new SubchannelStateListener() {
                @Override
                public void onSubchannelState(ConnectivityStateInfo state) {
                    processSubchannelState(subchannel, state);
                }
            });
            subchannels.put(strippedAddressGroup, subchannel);
            subchannel.requestConnection();
        }

        ArrayList<Subchannel> removedSubchannels = new ArrayList<>();
        for (EquivalentAddressGroup addressGroup : removedAddrs) {
            removedSubchannels.add(subchannels.remove(addressGroup));
        }

        // Update the picker before shutting down the subchannels, to reduce the chance of the race
        // between picking a subchannel and shutting it down.
        updateBalancingState();

        // Shutdown removed subchannels
        for (Subchannel removedSubchannel : removedSubchannels) {
            shutdownSubchannel(removedSubchannel);
        }
    }

    @Override
    public void handleNameResolutionError(Status error) {
        logger.error("handle name resolution error");
        // ready pickers aren't affected by status changes
        updateBalancingState(TRANSIENT_FAILURE,
                currentPicker instanceof ReadyPicker ? currentPicker : new EmptyPicker(error));
    }

    private void processSubchannelState(Subchannel subchannel, ConnectivityStateInfo stateInfo) {
        if (subchannels.get(stripAttrs(subchannel.getAddresses())) != subchannel) {
            return;
        }
        if (stateInfo.getState() == IDLE) {
            subchannel.requestConnection();
        }
        getSubchannelStateInfoRef(subchannel).value = stateInfo;
        updateBalancingState();
    }

    private void shutdownSubchannel(Subchannel subchannel) {
        subchannel.shutdown();
        getSubchannelStateInfoRef(subchannel).value =
                ConnectivityStateInfo.forNonError(SHUTDOWN);
    }

    @Override
    public void shutdown() {
        for (Subchannel subchannel : getSubchannels()) {
            shutdownSubchannel(subchannel);
        }
    }

    private static final Status EMPTY_OK = Status.OK.withDescription("no subchannels ready");

    /**
     * Updates picker with the list of active subchannels (state == READY).
     */
    @SuppressWarnings("ReferenceEquality")
    private void updateBalancingState() {
        List<Subchannel> activeList = filterNonFailingSubchannels(getSubchannels());
        if (activeList.isEmpty()) {
            // No READY subchannels, determine aggregate state and error status
            boolean isConnecting = false;
            Status aggStatus = EMPTY_OK;
            for (Subchannel subchannel : getSubchannels()) {
                ConnectivityStateInfo stateInfo = getSubchannelStateInfoRef(subchannel).value;
                // This subchannel IDLE is not because of channel IDLE_TIMEOUT,
                // in which case LB is already shutdown.
                // RCLB will request connection immediately on subchannel IDLE.
                if (stateInfo.getState() == CONNECTING || stateInfo.getState() == IDLE) {
                    isConnecting = true;
                }
                if (aggStatus == EMPTY_OK || !aggStatus.isOk()) {
                    aggStatus = stateInfo.getStatus();
                }
            }
            updateBalancingState(isConnecting ? CONNECTING : TRANSIENT_FAILURE,
                    // If all subchannels are TRANSIENT_FAILURE, return the Status associated with
                    // an arbitrary subchannel, otherwise return OK.
                    new EmptyPicker(aggStatus));
        } else {
            updateBalancingState(READY, new ReadyPicker(activeList));
        }
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("update balancing state list - %d active connections: %s", activeList.size(),
                    activeList.stream().map(s -> s.getAddresses().toString()).collect(Collectors.joining(","))));
        } else if(logger.isDebugEnabled()) {
            logger.debug(String.format("update balancing state list - %d active connections.", activeList.size()));
        }
    }

    private void updateBalancingState(ConnectivityState state, RandomChoicePicker picker) {
        if (state != currentState || !picker.isEquivalentTo(currentPicker)) {
            helper.updateBalancingState(state, picker);
            currentState = state;
            currentPicker = picker;
        }
    }

    /**
     * Filters out non-ready subchannels.
     */
    private static List<Subchannel> filterNonFailingSubchannels(
            Collection<Subchannel> subchannels) {
        List<Subchannel> readySubchannels = new ArrayList<>(subchannels.size());
        for (Subchannel subchannel : subchannels) {
            if (isReady(subchannel)) {
                readySubchannels.add(subchannel);
            }
        }
        return readySubchannels;
    }

    /**
     * Converts list of {@link EquivalentAddressGroup} to {@link EquivalentAddressGroup} set and
     * remove all attributes. The values are the original EAGs.
     */
    private static Map<EquivalentAddressGroup, EquivalentAddressGroup> stripAttrs(
            List<EquivalentAddressGroup> groupList) {
        Map<EquivalentAddressGroup, EquivalentAddressGroup> addrs = new HashMap<>(groupList.size() * 2);
        for (EquivalentAddressGroup group : groupList) {
            addrs.put(stripAttrs(group), group);
        }
        return addrs;
    }

    private static EquivalentAddressGroup stripAttrs(EquivalentAddressGroup eag) {
        return new EquivalentAddressGroup(eag.getAddresses());
    }

    @VisibleForTesting
    Collection<Subchannel> getSubchannels() {
        return subchannels.values();
    }

    private static Ref<ConnectivityStateInfo> getSubchannelStateInfoRef(
            Subchannel subchannel) {
        return checkNotNull(subchannel.getAttributes().get(STATE_INFO), "STATE_INFO");
    }

    // package-private to avoid synthetic access
    static boolean isReady(Subchannel subchannel) {
        return getSubchannelStateInfoRef(subchannel).value.getState() == READY;
    }

    private static <T> Set<T> setsDifference(Set<T> a, Set<T> b) {
        Set<T> aCopy = new HashSet<>(a);
        aCopy.removeAll(b);
        return aCopy;
    }

    // Only subclasses are ReadyPicker or EmptyPicker
    private abstract static class RandomChoicePicker extends SubchannelPicker {
        abstract boolean isEquivalentTo(RandomChoicePicker picker);
    }

    @VisibleForTesting
    static final class ReadyPicker extends RandomChoicePicker {
        private final List<Subchannel> list; // non-empty
        private final Random random;

        ReadyPicker(List<Subchannel> list) {
            Preconditions.checkArgument(!list.isEmpty(), "empty list");
            this.list = list;
            this.random = new Random();
        }

        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
            return PickResult.withSubchannel(chooseSubchannel());
        }

        private Subchannel chooseSubchannel() {
            return list.get(random.nextInt(list.size()));
        }

        @VisibleForTesting
        List<Subchannel> getList() {
            return list;
        }

        @Override
        boolean isEquivalentTo(RandomChoicePicker picker) {
            if (!(picker instanceof ReadyPicker)) {
                return false;
            }
            ReadyPicker other = (ReadyPicker) picker;
            // the lists cannot contain duplicate subchannels
            return other == this || (list.size() == other.list.size() && new HashSet<>(list).containsAll(other.list));
        }
    }

    @VisibleForTesting
    static final class EmptyPicker extends RandomChoicePicker {

        private final Status status;

        EmptyPicker(@Nonnull Status status) {
            this.status = Preconditions.checkNotNull(status, "status");
        }

        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
            return status.isOk() ? PickResult.withNoResult() : PickResult.withError(status);
        }

        @Override
        boolean isEquivalentTo(RandomChoicePicker picker) {
            return picker instanceof EmptyPicker && (Objects.equal(status, ((EmptyPicker) picker).status)
                    || (status.isOk() && ((EmptyPicker) picker).status.isOk()));
        }
    }

    /**
     * A lighter weight Reference than AtomicReference.
     */
    @VisibleForTesting
    static final class Ref<T> {
        T value;

        Ref(T value) {
            this.value = value;
        }
    }
}
