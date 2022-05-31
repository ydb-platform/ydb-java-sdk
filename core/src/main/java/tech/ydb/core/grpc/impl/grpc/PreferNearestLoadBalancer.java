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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.ChannelLogger;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.internal.GrpcAttributes;
import io.grpc.internal.ServiceConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.ConnectivityState.CONNECTING;
import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.SHUTDOWN;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;

/**
 * A {@link LoadBalancer} that provides prefer nearest DC round-robin
 * load-balancing over the {@link
 * EquivalentAddressGroup}s from the {@link io.grpc.NameResolver}. copy of
 * RoundRobinLoadBalancer with prefer local DC changes -
 * https://github.com/grpc/grpc-java/blob/master/core/src/main/java/io/grpc/util/RoundRobinLoadBalancer.java
 *
 * @author Evgeniy Pshenitsin
 */
public class PreferNearestLoadBalancer extends LoadBalancer {

    private final static Logger logger = LoggerFactory.getLogger(PreferNearestLoadBalancer.class);

    static final Attributes.Key<Ref<ConnectivityStateInfo>> STATE_INFO
            = Attributes.Key.create("state-info");

    // package-private to avoid synthetic access
    static final Attributes.Key<Ref<Subchannel>> STICKY_REF = Attributes.Key.create("sticky-ref");

    private final Helper helper;
    private final Map<EquivalentAddressGroup, Subchannel> subchannels
            = new HashMap<>();
    private final Random random;

    private ConnectivityState currentState;
    private RoundRobinPicker currentPicker = new EmptyPicker(EMPTY_OK);

    @Nullable
    private StickinessState stickinessState;

    private final String localDc;

    PreferNearestLoadBalancer(Helper helper, String localDc) {
        this.helper = checkNotNull(helper, "helper");
        this.localDc = localDc;
        this.random = new Random();
    }

    @Override
    public void handleResolvedAddressGroups(List<EquivalentAddressGroup> servers, Attributes attributes) {
        Set<EquivalentAddressGroup> currentAddrs = subchannels.keySet();
        Set<EquivalentAddressGroup> latestAddrs = filteredAddrs(servers, localDc);
        Set<EquivalentAddressGroup> addedAddrs = setsDifference(latestAddrs, currentAddrs);
        Set<EquivalentAddressGroup> removedAddrs = setsDifference(currentAddrs, latestAddrs);

        Map<String, ?> serviceConfig = attributes.get(GrpcAttributes.NAME_RESOLVER_SERVICE_CONFIG);
        if (serviceConfig != null) {
            String stickinessMetadataKey
                    = ServiceConfigUtil.getStickinessMetadataKeyFromServiceConfig(serviceConfig);
            if (stickinessMetadataKey != null) {
                if (stickinessMetadataKey.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                    helper.getChannelLogger().log(
                            ChannelLogger.ChannelLogLevel.WARNING,
                            "Binary stickiness header is not supported. The header \"{0}\" will be ignored",
                            stickinessMetadataKey);
                } else if (stickinessState == null
                        || !stickinessState.key.name().equals(stickinessMetadataKey)) {
                    stickinessState = new StickinessState(stickinessMetadataKey);
                }
            }
        }

        // Create new subchannels for new addresses.
        for (EquivalentAddressGroup addressGroup : addedAddrs) {
            // NB(lukaszx0): we don't merge `attributes` with `subchannelAttr` because subchannel
            // doesn't need them. They're describing the resolved server list but we're not taking
            // any action based on this information.
            Attributes.Builder subchannelAttrs = Attributes.newBuilder()
                    // NB(lukaszx0): because attributes are immutable we can't set new value for the key
                    // after creation but since we can mutate the values we leverage that and set
                    // AtomicReference which will allow mutating state info for given channel.
                    .set(STATE_INFO,
                            new Ref<>(ConnectivityStateInfo.forNonError(IDLE)));

            Ref<Subchannel> stickyRef = null;
            if (stickinessState != null) {
                subchannelAttrs.set(STICKY_REF, stickyRef = new Ref<>(null));
            }

            Subchannel subchannel = checkNotNull(
                    helper.createSubchannel(addressGroup, subchannelAttrs.build()), "subchannel");
            if (stickyRef != null) {
                stickyRef.value = subchannel;
            }

            subchannels.put(addressGroup, subchannel);
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
        // ready pickers aren't affected by status changes
        updateBalancingState(TRANSIENT_FAILURE,
                currentPicker instanceof ReadyPicker ? currentPicker : new EmptyPicker(error));
    }

    @Override
    public void handleSubchannelState(Subchannel subchannel, ConnectivityStateInfo stateInfo) {
        if (subchannels.get(subchannel.getAddresses()) != subchannel) {
            return;
        }
        if (stateInfo.getState() == SHUTDOWN && stickinessState != null) {
            stickinessState.remove(subchannel);
        }
        if (stateInfo.getState() == IDLE) {
            subchannel.requestConnection();
        }
        getSubchannelStateInfoRef(subchannel).value = stateInfo;
        updateBalancingState();
    }

    private void shutdownSubchannel(Subchannel subchannel) {
        subchannel.shutdown();
        getSubchannelStateInfoRef(subchannel).value
                = ConnectivityStateInfo.forNonError(SHUTDOWN);
        if (stickinessState != null) {
            stickinessState.remove(subchannel);
        }
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
                // RRLB will request connection immediately on subchannel IDLE.
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
            int startIndex = random.nextInt(activeList.size());
            updateBalancingState(READY, new ReadyPicker(activeList, startIndex, stickinessState));
        }
    }

    private void updateBalancingState(ConnectivityState state, RoundRobinPicker picker) {
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
     * Converts list of {@link EquivalentAddressGroup} to filtered by DC
     * {@link EquivalentAddressGroup} set and remove all attributes.
     */
    private static Set<EquivalentAddressGroup> filteredAddrs(List<EquivalentAddressGroup> groupList, String localDC) {
        Set<EquivalentAddressGroup> all = new HashSet<>(groupList.size());
        Set<EquivalentAddressGroup> filtered = new HashSet<>(groupList.size());

        for (EquivalentAddressGroup group : groupList) {
            // strip attribures
            EquivalentAddressGroup stripped = new EquivalentAddressGroup(group.getAddresses());
            all.add(stripped);

            String location = group.getAttributes().get(YdbNameResolver.LOCATION_ATTR);
            if (localDC != null && localDC.equalsIgnoreCase(location)) {
                filtered.add(stripped);
            }
        }

        if (filtered.isEmpty()) {
            logger.warn("Local dc nodes not found, switch to fallback mode - use all {} nodes", all.size());
            return all;
        }

        logger.debug("Found {} filtered nodes from {} all", filtered.size(), all.size());
        return filtered;
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

    Map<String, Ref<Subchannel>> getStickinessMapForTest() {
        if (stickinessState == null) {
            return null;
        }
        return stickinessState.stickinessMap;
    }

    /**
     * Holds stickiness related states: The stickiness key, a registry mapping
     * stickiness values to the associated Subchannel Ref, and a map from
     * Subchannel to Subchannel Ref.
     */
    @VisibleForTesting
    static final class StickinessState {

        static final int MAX_ENTRIES = 1000;

        final Metadata.Key<String> key;
        final ConcurrentMap<String, Ref<Subchannel>> stickinessMap
                = new ConcurrentHashMap<>();

        final Queue<String> evictionQueue = new ConcurrentLinkedQueue<>();

        StickinessState(@Nonnull String stickinessKey) {
            this.key = Metadata.Key.of(stickinessKey, Metadata.ASCII_STRING_MARSHALLER);
        }

        /**
         * Returns the subchannel associated to the stickiness value if
         * available in both the registry and the round robin list, otherwise
         * associates the given subchannel with the stickiness key in the
         * registry and returns the given subchannel.
         */
        @Nonnull
        Subchannel maybeRegister(
                String stickinessValue, @Nonnull Subchannel subchannel) {
            final Ref<Subchannel> newSubchannelRef = subchannel.getAttributes().get(STICKY_REF);
            while (true) {
                Ref<Subchannel> existingSubchannelRef
                        = stickinessMap.putIfAbsent(stickinessValue, newSubchannelRef);
                if (existingSubchannelRef == null) {
                    // new entry
                    addToEvictionQueue(stickinessValue);
                    return subchannel;
                } else {
                    // existing entry
                    Subchannel existingSubchannel = existingSubchannelRef.value;
                    if (existingSubchannel != null && isReady(existingSubchannel)) {
                        return existingSubchannel;
                    }
                }
                // existingSubchannelRef is not null but no longer valid, replace it
                if (stickinessMap.replace(stickinessValue, existingSubchannelRef, newSubchannelRef)) {
                    return subchannel;
                }
                // another thread concurrently removed or updated the entry, try again
            }
        }

        private void addToEvictionQueue(String value) {
            String oldValue;
            while (stickinessMap.size() >= MAX_ENTRIES && (oldValue = evictionQueue.poll()) != null) {
                stickinessMap.remove(oldValue);
            }
            evictionQueue.add(value);
        }

        /**
         * Unregister the subchannel from StickinessState.
         */
        void remove(Subchannel subchannel) {
            subchannel.getAttributes().get(STICKY_REF).value = null;
        }

        /**
         * Gets the subchannel associated with the stickiness value if there is.
         */
        @Nullable
        Subchannel getSubchannel(String stickinessValue) {
            Ref<Subchannel> subchannelRef = stickinessMap.get(stickinessValue);
            if (subchannelRef != null) {
                return subchannelRef.value;
            }
            return null;
        }
    }

    // Only subclasses are ReadyPicker or EmptyPicker
    private abstract static class RoundRobinPicker extends SubchannelPicker {

        abstract boolean isEquivalentTo(RoundRobinPicker picker);
    }

    @VisibleForTesting
    static final class ReadyPicker extends RoundRobinPicker {

        private static final AtomicIntegerFieldUpdater<ReadyPicker> indexUpdater
                = AtomicIntegerFieldUpdater.newUpdater(ReadyPicker.class, "index");

        private final List<Subchannel> list; // non-empty
        @Nullable
        private final StickinessState stickinessState;
        @SuppressWarnings("unused")
        private volatile int index;

        ReadyPicker(List<Subchannel> list, int startIndex, @Nullable StickinessState stickinessState) {
            Preconditions.checkArgument(!list.isEmpty(), "empty list");
            this.list = list;
            this.stickinessState = stickinessState;
            this.index = startIndex - 1;
        }

        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
            Subchannel subchannel = null;
            if (stickinessState != null) {
                String stickinessValue = args.getHeaders().get(stickinessState.key);
                if (stickinessValue != null) {
                    subchannel = stickinessState.getSubchannel(stickinessValue);
                    if (subchannel == null || !isReady(subchannel)) {
                        subchannel = stickinessState.maybeRegister(stickinessValue, nextSubchannel());
                    }
                }
            }

            return PickResult.withSubchannel(subchannel != null ? subchannel : nextSubchannel());
        }

        private Subchannel nextSubchannel() {
            int size = list.size();
            int i = indexUpdater.incrementAndGet(this);
            if (i >= size) {
                int oldi = i;
                i %= size;
                indexUpdater.compareAndSet(this, oldi, i);
            }
            return list.get(i);
        }

        @VisibleForTesting
        List<Subchannel> getList() {
            return list;
        }

        @Override
        boolean isEquivalentTo(RoundRobinPicker picker) {
            if (!(picker instanceof ReadyPicker)) {
                return false;
            }
            ReadyPicker other = (ReadyPicker) picker;
            // the lists cannot contain duplicate subchannels
            return other == this || (stickinessState == other.stickinessState
                    && list.size() == other.list.size()
                    && new HashSet<>(list).containsAll(other.list));
        }
    }

    @VisibleForTesting
    static final class EmptyPicker extends RoundRobinPicker {

        private final Status status;

        EmptyPicker(@Nonnull Status status) {
            this.status = Preconditions.checkNotNull(status, "status");
        }

        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
            return status.isOk() ? PickResult.withNoResult() : PickResult.withError(status);
        }

        @Override
        boolean isEquivalentTo(RoundRobinPicker picker) {
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
