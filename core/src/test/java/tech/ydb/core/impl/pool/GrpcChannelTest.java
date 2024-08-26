package tech.ydb.core.impl.pool;


import io.grpc.ConnectivityState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.grpc.GrpcTransportBuilder;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcChannelTest {
    private final ManagedChannelFactory factoryMock = Mockito.mock(ManagedChannelFactory.class);
    private final GrpcTransportBuilder builderMock = Mockito.mock(GrpcTransportBuilder.class);

    @Before
    public void setUp() {
        Mockito.when(builderMock.getManagedChannelFactory()).thenReturn(factoryMock);
        Mockito.when(factoryMock.getConnectTimeoutMs()).thenReturn(500l); // timeout for ready watcher
    }

    @Test
    public void goodChannels() {
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt()))
                .thenReturn(ManagedChannelMock.good(), ManagedChannelMock.good());

        EndpointRecord endpoint = new EndpointRecord("host1", 1234);

        GrpcChannel channel = new GrpcChannel(endpoint, factoryMock);
        Assert.assertEquals(endpoint, channel.getEndpoint());
        Assert.assertNotNull(channel.getReadyChannel());
        channel.shutdown();
        channel.shutdown(); // double shutdown is ok
    }

    @Test
    public void slowChannels() {
        ConnectivityState[] states = new ConnectivityState[] {
                ConnectivityState.CONNECTING,
                ConnectivityState.TRANSIENT_FAILURE,
                ConnectivityState.IDLE,
                ConnectivityState.CONNECTING,
                ConnectivityState.TRANSIENT_FAILURE,
                ConnectivityState.CONNECTING,
                ConnectivityState.TRANSIENT_FAILURE,
                ConnectivityState.CONNECTING,
                ConnectivityState.READY,
        };

        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt()))
                .thenReturn(new ManagedChannelMock(ConnectivityState.IDLE).nextStates(states))
                .thenReturn(new ManagedChannelMock(ConnectivityState.IDLE).nextStates(states));

        EndpointRecord endpoint = new EndpointRecord("host1", 1234);

        GrpcChannel channel = new GrpcChannel(endpoint, factoryMock);
        Assert.assertEquals(endpoint, channel.getEndpoint());
        Assert.assertNotNull(channel.getReadyChannel());
        channel.shutdown();
    }

    @Test
    public void badChannels() {
        ConnectivityState[] states = new ConnectivityState[] {
                ConnectivityState.CONNECTING,
                ConnectivityState.TRANSIENT_FAILURE,
                ConnectivityState.CONNECTING,
                ConnectivityState.TRANSIENT_FAILURE,
                ConnectivityState.SHUTDOWN,
        };

        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt()))
                .thenReturn(new ManagedChannelMock(ConnectivityState.IDLE).nextStates(states))
                .thenReturn(new ManagedChannelMock(ConnectivityState.IDLE).nextStates(states));

        EndpointRecord endpoint = new EndpointRecord("host1", 1234);

        GrpcChannel channel = new GrpcChannel(endpoint, factoryMock);
        Assert.assertEquals(endpoint, channel.getEndpoint());

        RuntimeException ex1 = Assert.assertThrows(RuntimeException.class, channel::getReadyChannel);
        Assert.assertEquals("Channel Endpoint{host=host1, port=1234, node=0, location=null} connecting problem",
                ex1.getMessage());

        channel.shutdown();
    }
}
