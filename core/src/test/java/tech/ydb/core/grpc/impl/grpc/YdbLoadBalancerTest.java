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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import tech.ydb.core.grpc.impl.grpc.YdbLoadBalancer.EmptyPicker;
import tech.ydb.core.grpc.impl.grpc.YdbLoadBalancer.RandomReadyPicker;
import tech.ydb.core.grpc.impl.grpc.YdbLoadBalancer.Ref;
import tech.ydb.core.grpc.impl.grpc.YdbLoadBalancer.RoundRobinReadyPicker;
import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.CreateSubchannelArgs;
import io.grpc.LoadBalancer.Helper;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.ResolvedAddresses;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.LoadBalancer.SubchannelStateListener;
import io.grpc.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.truth.Truth.assertThat;
import static tech.ydb.core.grpc.impl.grpc.YdbLoadBalancer.STATE_INFO;
import static io.grpc.ConnectivityState.CONNECTING;
import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.SHUTDOWN;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author Alexandr Gorshenin
 * Unit test for {@link YdbLoadBalancer}.
 * based on
 * https://github.com/grpc/grpc-java/blob/v1.42.x/core/src/test/java/io/grpc/util/RoundRobinLoadBalancerTest.java
 */
@RunWith(JUnit4.class)
public class YdbLoadBalancerTest {

    private static final Attributes.Key<String> MAJOR_KEY = Attributes.Key.create("major-key");

    private final List<EquivalentAddressGroup> servers = Lists.newArrayList();
    private final Map<List<EquivalentAddressGroup>, Subchannel> subchannels = Maps.newLinkedHashMap();
    private final Map<Subchannel, SubchannelStateListener> subchannelStateListeners
            = Maps.newLinkedHashMap();
    private final Attributes affinity
            = Attributes.newBuilder().set(MAJOR_KEY, "I got the keys").build();

    private AutoCloseable mocks;
    private MockedStatic<ThreadLocalRandom> threadLocalStaticMock;

    @Captor
    private ArgumentCaptor<SubchannelPicker> pickerCaptor;
    @Captor
    private ArgumentCaptor<ConnectivityState> stateCaptor;
    @Captor
    private ArgumentCaptor<CreateSubchannelArgs> createArgsCaptor;
    @Mock
    private Helper mockHelper;
    @Mock
    private ThreadLocalRandom random;

    @Mock // This LoadBalancer doesn't use any of the arg fields, as verified in tearDown().
    private PickSubchannelArgs mockArgs;

    private void initServers(String dc) {
        for (int i = 0; i < 3; i++) {
            String name = "Free server " + i;
            Attributes attrs = Attributes.EMPTY;
            if (dc != null) {
                name = dc + " server " + i;
                attrs = Attributes.newBuilder()
                        .set(YdbNameResolver.LOCATION_ATTR, dc)
                        .build();
            }

            SocketAddress addr = new FakeSocketAddress(name);
            EquivalentAddressGroup eag = new EquivalentAddressGroup(addr, attrs);
            servers.add(eag);
            Subchannel sc = mock(Subchannel.class);
            subchannels.put(Arrays.asList(eag), sc);
        }
    }

    private Map<List<EquivalentAddressGroup>, Subchannel> subChannels(String dc) {
        if (dc == null) {
            return subchannels;
        }
        Map<List<EquivalentAddressGroup>, Subchannel> filtered = Maps.newLinkedHashMap();
        for (Map.Entry<List<EquivalentAddressGroup>,Subchannel> entry: subchannels.entrySet()) {
            if (!entry.getKey().isEmpty()) {
                EquivalentAddressGroup eog = entry.getKey().get(0);
                String location = eog.getAttributes().get(YdbNameResolver.LOCATION_ATTR);
                if (dc.equalsIgnoreCase(location)) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return filtered;
    }

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        threadLocalStaticMock = Mockito.mockStatic(ThreadLocalRandom.class);
        threadLocalStaticMock.when(ThreadLocalRandom::current).thenReturn(random);

        initServers(null);
        initServers("DC1");
        initServers("DC2");

        when(mockHelper.createSubchannel(any(CreateSubchannelArgs.class))).then((InvocationOnMock invocation) -> {
            CreateSubchannelArgs args = (CreateSubchannelArgs) invocation.getArguments()[0];
            final Subchannel subchannel = subchannels.get(args.getAddresses());
            // This line makes Mockito 3 to work, but I don't know why it is broken
            when(subchannel.getAddresses()).thenReturn(args.getAddresses().get(0));
            when(subchannel.getAllAddresses()).thenReturn(args.getAddresses());
            when(subchannel.getAttributes()).thenReturn(args.getAttributes());
            doAnswer((Answer<Void>) (InvocationOnMock invocation1) -> {
                subchannelStateListeners.put(subchannel, (SubchannelStateListener) invocation1.getArguments()[0]);
                return null;
            }).when(subchannel).start(any(SubchannelStateListener.class));
            return subchannel;
        });
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(mockArgs);
        threadLocalStaticMock.close();
        mocks.close();
    }

    @Test
    public void pickAfterResolved() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        final Subchannel readySubchannel = subchannels.values().iterator().next();
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(affinity).build());
        deliverSubchannelState(readySubchannel, ConnectivityStateInfo.forNonError(READY));

        verify(mockHelper, times(9)).createSubchannel(createArgsCaptor.capture());
        List<List<EquivalentAddressGroup>> capturedAddrs = new ArrayList<>();
        for (CreateSubchannelArgs arg : createArgsCaptor.getAllValues()) {
            capturedAddrs.add(arg.getAddresses());
        }

        assertThat(capturedAddrs).containsAllIn(subchannels.keySet());
        for (Subchannel subchannel : subchannels.values()) {
            verify(subchannel).requestConnection();
            verify(subchannel, never()).shutdown();
        }

        verify(mockHelper, times(2))
                .updateBalancingState(stateCaptor.capture(), pickerCaptor.capture());

        assertEquals(CONNECTING, stateCaptor.getAllValues().get(0));
        assertEquals(READY, stateCaptor.getAllValues().get(1));
        assertThat(getList(pickerCaptor.getValue())).containsExactly(readySubchannel);

        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void pickAfterResolvedLocalDC() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, "dC1");
        Map<List<EquivalentAddressGroup>, Subchannel> localChannels = subChannels("Dc1");

        final Subchannel readySubchannel = localChannels.values().iterator().next();
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(affinity).build());
        deliverSubchannelState(readySubchannel, ConnectivityStateInfo.forNonError(READY));

        verify(mockHelper, times(3)).createSubchannel(createArgsCaptor.capture());
        List<List<EquivalentAddressGroup>> capturedAddrs = new ArrayList<>();
        for (CreateSubchannelArgs arg : createArgsCaptor.getAllValues()) {
            capturedAddrs.add(arg.getAddresses());
        }

        assertThat(capturedAddrs).containsAllIn(localChannels.keySet());
        for (Subchannel subchannel : localChannels.values()) {
            verify(subchannel).requestConnection();
            verify(subchannel, never()).shutdown();
        }

        verify(mockHelper, times(2))
                .updateBalancingState(stateCaptor.capture(), pickerCaptor.capture());

        assertEquals(CONNECTING, stateCaptor.getAllValues().get(0));
        assertEquals(READY, stateCaptor.getAllValues().get(1));
        assertThat(getList(pickerCaptor.getValue())).containsExactly(readySubchannel);

        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void pickAfterResolvedWrongDC() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, "DC3");

        final Subchannel readySubchannel = subchannels.values().iterator().next();
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(affinity).build());
        deliverSubchannelState(readySubchannel, ConnectivityStateInfo.forNonError(READY));

        verify(mockHelper, times(9)).createSubchannel(createArgsCaptor.capture());
        List<List<EquivalentAddressGroup>> capturedAddrs = new ArrayList<>();
        for (CreateSubchannelArgs arg : createArgsCaptor.getAllValues()) {
            capturedAddrs.add(arg.getAddresses());
        }

        assertThat(capturedAddrs).containsAllIn(subchannels.keySet());
        for (Subchannel subchannel : subchannels.values()) {
            verify(subchannel).requestConnection();
            verify(subchannel, never()).shutdown();
        }

        verify(mockHelper, times(2))
                .updateBalancingState(stateCaptor.capture(), pickerCaptor.capture());

        assertEquals(CONNECTING, stateCaptor.getAllValues().get(0));
        assertEquals(READY, stateCaptor.getAllValues().get(1));
        assertThat(getList(pickerCaptor.getValue())).containsExactly(readySubchannel);

        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void pickAfterResolvedRandomLocalDC() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, true, "dC2");
        Map<List<EquivalentAddressGroup>, Subchannel> localChannels = subChannels("Dc2");

        final Subchannel readySubchannel = localChannels.values().iterator().next();
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(affinity).build());
        deliverSubchannelState(readySubchannel, ConnectivityStateInfo.forNonError(READY));

        verify(mockHelper, times(3)).createSubchannel(createArgsCaptor.capture());
        List<List<EquivalentAddressGroup>> capturedAddrs = new ArrayList<>();
        for (CreateSubchannelArgs arg : createArgsCaptor.getAllValues()) {
            capturedAddrs.add(arg.getAddresses());
        }

        assertThat(capturedAddrs).containsAllIn(localChannels.keySet());
        for (Subchannel subchannel : localChannels.values()) {
            verify(subchannel).requestConnection();
            verify(subchannel, never()).shutdown();
        }

        verify(mockHelper, times(2))
                .updateBalancingState(stateCaptor.capture(), pickerCaptor.capture());

        assertEquals(CONNECTING, stateCaptor.getAllValues().get(0));
        assertEquals(READY, stateCaptor.getAllValues().get(1));
        assertThat(getList(pickerCaptor.getValue())).containsExactly(readySubchannel);

        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void pickAfterResolvedUpdatedHosts() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        Subchannel removedSubchannel = mock(Subchannel.class);
        Subchannel oldSubchannel = mock(Subchannel.class);
        Subchannel newSubchannel = mock(Subchannel.class);

        Attributes.Key<String> key = Attributes.Key.create("check-that-it-is-propagated");
        FakeSocketAddress removedAddr = new FakeSocketAddress("removed");
        EquivalentAddressGroup removedEag = new EquivalentAddressGroup(removedAddr);
        FakeSocketAddress oldAddr = new FakeSocketAddress("old");
        EquivalentAddressGroup oldEag1 = new EquivalentAddressGroup(oldAddr);
        EquivalentAddressGroup oldEag2 = new EquivalentAddressGroup(
                oldAddr, Attributes.newBuilder().set(key, "oldattr").build());
        FakeSocketAddress newAddr = new FakeSocketAddress("new");
        EquivalentAddressGroup newEag = new EquivalentAddressGroup(
                newAddr, Attributes.newBuilder().set(key, "newattr").build());

        subchannels.put(Collections.singletonList(removedEag), removedSubchannel);
        subchannels.put(Collections.singletonList(oldEag1), oldSubchannel);
        subchannels.put(Collections.singletonList(newEag), newSubchannel);

        List<EquivalentAddressGroup> currentServers = Lists.newArrayList(removedEag, oldEag1);

        InOrder inOrder = inOrder(mockHelper);

        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(currentServers).setAttributes(affinity)
                        .build());

        inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), pickerCaptor.capture());

        deliverSubchannelState(removedSubchannel, ConnectivityStateInfo.forNonError(READY));
        deliverSubchannelState(oldSubchannel, ConnectivityStateInfo.forNonError(READY));

        inOrder.verify(mockHelper, times(2)).updateBalancingState(eq(READY), pickerCaptor.capture());
        SubchannelPicker picker = pickerCaptor.getValue();
        assertThat(getList(picker)).containsExactly(removedSubchannel, oldSubchannel);

        verify(removedSubchannel, times(1)).requestConnection();
        verify(oldSubchannel, times(1)).requestConnection();

        assertThat(loadBalancer.getSubchannels()).containsExactly(removedSubchannel,
                oldSubchannel);

        // This time with Attributes
        List<EquivalentAddressGroup> latestServers = Lists.newArrayList(oldEag2, newEag);

        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(latestServers).setAttributes(affinity).build());

        verify(newSubchannel, times(1)).requestConnection();
        verify(oldSubchannel, times(1)).updateAddresses(Arrays.asList(oldEag2));
        verify(removedSubchannel, times(1)).shutdown();

        deliverSubchannelState(removedSubchannel, ConnectivityStateInfo.forNonError(SHUTDOWN));
        deliverSubchannelState(newSubchannel, ConnectivityStateInfo.forNonError(READY));

        assertThat(loadBalancer.getSubchannels()).containsExactly(oldSubchannel,
                newSubchannel);

        verify(mockHelper, times(3)).createSubchannel(any(CreateSubchannelArgs.class));
        inOrder.verify(mockHelper, times(2)).updateBalancingState(eq(READY), pickerCaptor.capture());

        picker = pickerCaptor.getValue();
        assertThat(getList(picker)).containsExactly(oldSubchannel, newSubchannel);

        // test going from non-empty to empty
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder()
                        .setAddresses(Collections.<EquivalentAddressGroup>emptyList())
                        .setAttributes(affinity)
                        .build());

        inOrder.verify(mockHelper).updateBalancingState(eq(TRANSIENT_FAILURE), pickerCaptor.capture());
        assertEquals(PickResult.withNoResult(), pickerCaptor.getValue().pickSubchannel(mockArgs));

        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void pickAfterStateChange() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        InOrder inOrder = inOrder(mockHelper);
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(Attributes.EMPTY)
                        .build());
        Subchannel subchannel = loadBalancer.getSubchannels().iterator().next();
        Ref<ConnectivityStateInfo> subchannelStateInfo = subchannel.getAttributes().get(
                STATE_INFO);

        inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), isA(EmptyPicker.class));
        assertThat(subchannelStateInfo.value).isEqualTo(ConnectivityStateInfo.forNonError(IDLE));

        deliverSubchannelState(subchannel,
                ConnectivityStateInfo.forNonError(READY));
        inOrder.verify(mockHelper).updateBalancingState(eq(READY), pickerCaptor.capture());
        assertThat(pickerCaptor.getValue()).isInstanceOf(RoundRobinReadyPicker.class);
        assertThat(subchannelStateInfo.value).isEqualTo(
                ConnectivityStateInfo.forNonError(READY));

        Status error = Status.UNKNOWN.withDescription("¯\\_(ツ)_//¯");
        deliverSubchannelState(subchannel,
                ConnectivityStateInfo.forTransientFailure(error));
        assertThat(subchannelStateInfo.value.getState()).isEqualTo(TRANSIENT_FAILURE);
        assertThat(subchannelStateInfo.value.getStatus()).isEqualTo(error);
        inOrder.verify(mockHelper).refreshNameResolution();
        inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), pickerCaptor.capture());
        assertThat(pickerCaptor.getValue()).isInstanceOf(EmptyPicker.class);

        deliverSubchannelState(subchannel,
                ConnectivityStateInfo.forNonError(IDLE));
        inOrder.verify(mockHelper).refreshNameResolution();
        assertThat(subchannelStateInfo.value.getState()).isEqualTo(TRANSIENT_FAILURE);
        assertThat(subchannelStateInfo.value.getStatus()).isEqualTo(error);

        verify(subchannel, times(2)).requestConnection();
        verify(mockHelper, times(9)).createSubchannel(any(CreateSubchannelArgs.class));
        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void ignoreShutdownSubchannelStateChange() {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        InOrder inOrder = inOrder(mockHelper);
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(Attributes.EMPTY)
                        .build());
        inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), isA(EmptyPicker.class));

        loadBalancer.shutdown();
        for (Subchannel sc : loadBalancer.getSubchannels()) {
            verify(sc).shutdown();
            // When the subchannel is being shut down, a SHUTDOWN connectivity state is delivered
            // back to the subchannel state listener.
            deliverSubchannelState(sc, ConnectivityStateInfo.forNonError(SHUTDOWN));
        }

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void stayTransientFailureUntilReady() {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        InOrder inOrder = inOrder(mockHelper);
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(Attributes.EMPTY)
                        .build());

        inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), isA(EmptyPicker.class));

        // Simulate state transitions for each subchannel individually.
        for (Subchannel sc : loadBalancer.getSubchannels()) {
            Status error = Status.UNKNOWN.withDescription("connection broken");
            deliverSubchannelState(
                    sc,
                    ConnectivityStateInfo.forTransientFailure(error));
            inOrder.verify(mockHelper).refreshNameResolution();
            deliverSubchannelState(
                    sc,
                    ConnectivityStateInfo.forNonError(CONNECTING));
            Ref<ConnectivityStateInfo> scStateInfo = sc.getAttributes().get(
                    STATE_INFO);
            assertThat(scStateInfo.value.getState()).isEqualTo(TRANSIENT_FAILURE);
            assertThat(scStateInfo.value.getStatus()).isEqualTo(error);
        }
        inOrder.verify(mockHelper).updateBalancingState(eq(TRANSIENT_FAILURE), isA(EmptyPicker.class));
        inOrder.verifyNoMoreInteractions();

        Subchannel subchannel = loadBalancer.getSubchannels().iterator().next();
        deliverSubchannelState(subchannel, ConnectivityStateInfo.forNonError(READY));
        Ref<ConnectivityStateInfo> subchannelStateInfo = subchannel.getAttributes().get(
                STATE_INFO);
        assertThat(subchannelStateInfo.value).isEqualTo(ConnectivityStateInfo.forNonError(READY));
        inOrder.verify(mockHelper).updateBalancingState(eq(READY), isA(RoundRobinReadyPicker.class));

        verify(mockHelper, times(9)).createSubchannel(any(CreateSubchannelArgs.class));
        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void refreshNameResolutionWhenSubchannelConnectionBroken() {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        InOrder inOrder = inOrder(mockHelper);
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(Attributes.EMPTY)
                        .build());

        verify(mockHelper, times(9)).createSubchannel(any(CreateSubchannelArgs.class));
        inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), isA(EmptyPicker.class));

        // Simulate state transitions for each subchannel individually.
        for (Subchannel sc : loadBalancer.getSubchannels()) {
            verify(sc).requestConnection();
            deliverSubchannelState(sc, ConnectivityStateInfo.forNonError(CONNECTING));
            Status error = Status.UNKNOWN.withDescription("connection broken");
            deliverSubchannelState(sc, ConnectivityStateInfo.forTransientFailure(error));
            inOrder.verify(mockHelper).refreshNameResolution();
            deliverSubchannelState(sc, ConnectivityStateInfo.forNonError(READY));
            inOrder.verify(mockHelper).updateBalancingState(eq(READY), isA(RoundRobinReadyPicker.class));
            // Simulate receiving go-away so READY subchannels transit to IDLE.
            deliverSubchannelState(sc, ConnectivityStateInfo.forNonError(IDLE));
            inOrder.verify(mockHelper).refreshNameResolution();
            verify(sc, times(2)).requestConnection();
            inOrder.verify(mockHelper).updateBalancingState(eq(CONNECTING), isA(EmptyPicker.class));
        }

        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void pickerRoundRobin() throws Exception {
        Subchannel subchannel = mock(Subchannel.class);
        Subchannel subchannel1 = mock(Subchannel.class);
        Subchannel subchannel2 = mock(Subchannel.class);

        RoundRobinReadyPicker picker = new RoundRobinReadyPicker(Collections.unmodifiableList(
                Lists.newArrayList(subchannel, subchannel1, subchannel2)),
                1 /* startIndex */);

        assertThat(picker.getList()).containsExactly(subchannel, subchannel1, subchannel2);

        assertEquals(subchannel1, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel2, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel1, picker.pickSubchannel(mockArgs).getSubchannel());
    }

    @Test
    public void pickerRandomChoice() throws Exception {
        Subchannel subchannel = mock(Subchannel.class);
        Subchannel subchannel1 = mock(Subchannel.class);
        Subchannel subchannel2 = mock(Subchannel.class);
        Subchannel subchannel3 = mock(Subchannel.class);

        RandomReadyPicker picker = new RandomReadyPicker(Collections.unmodifiableList(
                Lists.newArrayList(subchannel, subchannel1, subchannel2, subchannel3)));

        when(random.nextInt(anyInt())).thenReturn(1, 0, 3, 1, 3, 2);

        assertThat(picker.getList()).containsExactly(subchannel, subchannel1, subchannel2, subchannel3);

        assertEquals(subchannel1, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel3, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel1, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel3, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(subchannel2, picker.pickSubchannel(mockArgs).getSubchannel());

        verify(random, times(6)).nextInt(eq(4));
    }

    @Test
    public void pickerEmptyList() throws Exception {
        SubchannelPicker picker = new EmptyPicker(Status.UNKNOWN);

        assertEquals(null, picker.pickSubchannel(mockArgs).getSubchannel());
        assertEquals(Status.UNKNOWN,
                picker.pickSubchannel(mockArgs).getStatus());
    }

    @Test
    public void nameResolutionErrorWithNoChannels() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        Status error = Status.NOT_FOUND.withDescription("nameResolutionError");
        loadBalancer.handleNameResolutionError(error);
        verify(mockHelper).updateBalancingState(eq(TRANSIENT_FAILURE), pickerCaptor.capture());
        LoadBalancer.PickResult pickResult = pickerCaptor.getValue().pickSubchannel(mockArgs);
        assertNull(pickResult.getSubchannel());
        assertEquals(error, pickResult.getStatus());
        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void nameResolutionErrorWithActiveChannels() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        final Subchannel readySubchannel = subchannels.values().iterator().next();
        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(affinity).build());
        deliverSubchannelState(readySubchannel, ConnectivityStateInfo.forNonError(READY));
        loadBalancer.handleNameResolutionError(Status.NOT_FOUND.withDescription("nameResolutionError"));

        verify(mockHelper, times(9)).createSubchannel(any(CreateSubchannelArgs.class));
        verify(mockHelper, times(2))
                .updateBalancingState(stateCaptor.capture(), pickerCaptor.capture());

        Iterator<ConnectivityState> stateIterator = stateCaptor.getAllValues().iterator();
        assertEquals(CONNECTING, stateIterator.next());
        assertEquals(READY, stateIterator.next());

        LoadBalancer.PickResult pickResult = pickerCaptor.getValue().pickSubchannel(mockArgs);
        assertEquals(readySubchannel, pickResult.getSubchannel());
        assertEquals(Status.OK.getCode(), pickResult.getStatus().getCode());

        LoadBalancer.PickResult pickResult2 = pickerCaptor.getValue().pickSubchannel(mockArgs);
        assertEquals(readySubchannel, pickResult2.getSubchannel());
        verifyNoMoreInteractions(mockHelper);
    }

    @Test
    public void subchannelStateIsolation() throws Exception {
        YdbLoadBalancer loadBalancer = new YdbLoadBalancer(mockHelper, false, null);

        Iterator<Subchannel> subchannelIterator = subchannels.values().iterator();
        Subchannel sc1 = subchannelIterator.next();
        Subchannel sc2 = subchannelIterator.next();
        Subchannel sc3 = subchannelIterator.next();

        loadBalancer.handleResolvedAddresses(
                ResolvedAddresses.newBuilder().setAddresses(servers).setAttributes(Attributes.EMPTY)
                        .build());
        verify(sc1, times(1)).requestConnection();
        verify(sc2, times(1)).requestConnection();
        verify(sc3, times(1)).requestConnection();

        deliverSubchannelState(sc1, ConnectivityStateInfo.forNonError(READY));
        deliverSubchannelState(sc2, ConnectivityStateInfo.forNonError(READY));
        deliverSubchannelState(sc3, ConnectivityStateInfo.forNonError(READY));
        deliverSubchannelState(sc2, ConnectivityStateInfo.forNonError(IDLE));
        deliverSubchannelState(sc3, ConnectivityStateInfo.forTransientFailure(Status.UNAVAILABLE));

        verify(mockHelper, times(6))
                .updateBalancingState(stateCaptor.capture(), pickerCaptor.capture());
        Iterator<ConnectivityState> stateIterator = stateCaptor.getAllValues().iterator();
        Iterator<SubchannelPicker> pickers = pickerCaptor.getAllValues().iterator();
        // The picker is incrementally updated as subchannels become READY
        assertEquals(CONNECTING, stateIterator.next());
        assertThat(pickers.next()).isInstanceOf(EmptyPicker.class);
        assertEquals(READY, stateIterator.next());
        assertThat(getList(pickers.next())).containsExactly(sc1);
        assertEquals(READY, stateIterator.next());
        assertThat(getList(pickers.next())).containsExactly(sc1, sc2);
        assertEquals(READY, stateIterator.next());
        assertThat(getList(pickers.next())).containsExactly(sc1, sc2, sc3);
        // The IDLE subchannel is dropped from the picker, but a reconnection is requested
        assertEquals(READY, stateIterator.next());
        assertThat(getList(pickers.next())).containsExactly(sc1, sc3);
        verify(sc2, times(2)).requestConnection();
        // The failing subchannel is dropped from the picker, with no requested reconnect
        assertEquals(READY, stateIterator.next());
        assertThat(getList(pickers.next())).containsExactly(sc1);
        verify(sc3, times(1)).requestConnection();
        assertThat(stateIterator.hasNext()).isFalse();
        assertThat(pickers.hasNext()).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void readyPicker_emptyList() {
        // ready picker list must be non-empty
        (new RoundRobinReadyPicker(Collections.<Subchannel>emptyList(), 0)).getClass();
    }

    @Test(expected = IllegalArgumentException.class)
    public void randomPicker_emptyList() {
        // ready picker list must be non-empty
        (new RandomReadyPicker(Collections.<Subchannel>emptyList())).getClass();
    }

    @Test
    public void internalPickerComparisons() {
        EmptyPicker emptyOk1 = new EmptyPicker(Status.OK);
        EmptyPicker emptyOk2 = new EmptyPicker(Status.OK.withDescription("different OK"));
        EmptyPicker emptyErr = new EmptyPicker(Status.UNKNOWN.withDescription("¯\\_(ツ)_//¯"));

        Iterator<Subchannel> subchannelIterator = subchannels.values().iterator();
        Subchannel sc1 = subchannelIterator.next();
        Subchannel sc2 = subchannelIterator.next();
        RoundRobinReadyPicker roundRobin1 = new RoundRobinReadyPicker(Arrays.asList(sc1, sc2), 0);
        RoundRobinReadyPicker roundRobin2 = new RoundRobinReadyPicker(Arrays.asList(sc1), 0);
        RoundRobinReadyPicker roundRobin3 = new RoundRobinReadyPicker(Arrays.asList(sc2, sc1), 1);
        RoundRobinReadyPicker roundRobin4 = new RoundRobinReadyPicker(Arrays.asList(sc1, sc2), 1);
        RoundRobinReadyPicker roundRobin5 = new RoundRobinReadyPicker(Arrays.asList(sc2, sc1), 0);

        RandomReadyPicker random1 = new RandomReadyPicker(Arrays.asList(sc1, sc2));
        RandomReadyPicker random2 = new RandomReadyPicker(Arrays.asList(sc1));
        RandomReadyPicker random3 = new RandomReadyPicker(Arrays.asList(sc2, sc1));

        assertTrue(emptyOk1.isEquivalentTo(emptyOk2));
        assertFalse(emptyOk1.isEquivalentTo(emptyErr));

        assertFalse(roundRobin1.isEquivalentTo(roundRobin2));
        assertTrue(roundRobin1.isEquivalentTo(roundRobin3));
        assertTrue(roundRobin3.isEquivalentTo(roundRobin4));
        assertTrue(roundRobin4.isEquivalentTo(roundRobin5));

        assertFalse(random1.isEquivalentTo(random2));
        assertFalse(random2.isEquivalentTo(random3));
        assertTrue(random1.isEquivalentTo(random3));

        assertFalse(roundRobin1.isEquivalentTo(random1));
        assertFalse(roundRobin2.isEquivalentTo(random2));
        assertFalse(roundRobin1.isEquivalentTo(random2));

        assertFalse(emptyOk1.isEquivalentTo(roundRobin1));
        assertFalse(roundRobin1.isEquivalentTo(emptyOk1));
        assertFalse(emptyOk1.isEquivalentTo(random1));
        assertFalse(random1.isEquivalentTo(emptyOk1));

        assertTrue(emptyOk1.toString().startsWith("EmptyPicker{status=Status{code=OK"));
        assertTrue(roundRobin1.toString().startsWith("RoundRobinReadyPicker{list=[Mock for Subchannel"));
        assertTrue(random1.toString().startsWith("RandomReadyPicker{list=[Mock for Subchannel"));
    }

    private static List<Subchannel> getList(SubchannelPicker picker) {
        if (picker instanceof RoundRobinReadyPicker) {
            return ((RoundRobinReadyPicker) picker).getList();
        }
        if (picker instanceof RandomReadyPicker) {
            return ((RandomReadyPicker) picker).getList();
        }
        return Collections.<Subchannel>emptyList();
    }

    private void deliverSubchannelState(Subchannel subchannel, ConnectivityStateInfo newState) {
        subchannelStateListeners.get(subchannel).onSubchannelState(newState);
    }

    private static class FakeSocketAddress extends SocketAddress {

        final String name;

        FakeSocketAddress(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "FakeSocketAddress-" + name;
        }
    }
}
