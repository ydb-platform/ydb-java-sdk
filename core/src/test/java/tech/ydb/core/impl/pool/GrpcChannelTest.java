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

        GrpcChannel lazy = new GrpcChannel(endpoint, factoryMock, false);
        Assert.assertEquals(endpoint, lazy.getEndpoint());
        Assert.assertNotNull(lazy.getReadyChannel());
        lazy.shutdown();
        lazy.shutdown(); // double shutdown is ok

        GrpcChannel eager = new GrpcChannel(endpoint, factoryMock, true);
        Assert.assertEquals(endpoint, eager.getEndpoint());
        Assert.assertNotNull(eager.getReadyChannel());
        eager.shutdown();
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

        GrpcChannel lazy = new GrpcChannel(endpoint, factoryMock, false);
        Assert.assertEquals(endpoint, lazy.getEndpoint());
        Assert.assertNotNull(lazy.getReadyChannel());
        lazy.shutdown();

        GrpcChannel eager = new GrpcChannel(endpoint, factoryMock, true);
        Assert.assertEquals(endpoint, eager.getEndpoint());
        Assert.assertNotNull(eager.getReadyChannel());
        eager.shutdown();
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

        GrpcChannel lazy = new GrpcChannel(endpoint, factoryMock, false);
        Assert.assertEquals(endpoint, lazy.getEndpoint());

        RuntimeException ex1 = Assert.assertThrows(RuntimeException.class, lazy::getReadyChannel);
        Assert.assertEquals("Channel Endpoint{host=host1, port=1234, node=0, location=null} connecting problem",
                ex1.getMessage());

        lazy.shutdown();

        GrpcChannel eager = new GrpcChannel(endpoint, factoryMock, true);
        Assert.assertEquals(endpoint, eager.getEndpoint());

        RuntimeException ex2 = Assert.assertThrows(RuntimeException.class, eager::getReadyChannel);
        Assert.assertEquals("Channel Endpoint{host=host1, port=1234, node=0, location=null} connecting problem",
                ex2.getMessage());

        eager.shutdown();
    }
}
