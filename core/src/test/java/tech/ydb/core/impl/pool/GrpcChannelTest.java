package tech.ydb.core.impl.pool;


import io.grpc.ConnectivityState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcChannelTest {
    private static final GrpcTransportBuilder BUILDER = GrpcTransport.forHost("ydb.tech", 1245, "/Local");

    private final AutoCloseable mocks = MockitoAnnotations.openMocks(this);
    private final MockedStatic<ManagedChannelFactory> factoryStaticMock = Mockito.mockStatic(ManagedChannelFactory.class);
    private final ManagedChannelFactory factoryMock = Mockito.mock(ManagedChannelFactory.class);

    @Before
    public void setUp() {
        factoryStaticMock.when(() -> ManagedChannelFactory.fromBuilder(BUILDER)).thenReturn(factoryMock);
        Mockito.when(factoryMock.getConnectTimeoutMs()).thenReturn(500l); // timeout for ready watcher
    }

    @After
    public void tearDown() throws Exception {
        factoryStaticMock.close();
        mocks.close();
    }

    @Test
    public void goodChannels() {
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt()))
                .thenReturn(ManagedChannelMock.good(), ManagedChannelMock.good());

        EndpointRecord endpoint = new EndpointRecord("host1", 1234, 0);

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

        EndpointRecord endpoint = new EndpointRecord("host1", 1234, 0);

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

        EndpointRecord endpoint = new EndpointRecord("host1", 1234, 0);

        GrpcChannel lazy = new GrpcChannel(endpoint, factoryMock, false);
        Assert.assertEquals(endpoint, lazy.getEndpoint());

        RuntimeException ex1 = Assert.assertThrows(RuntimeException.class, lazy::getReadyChannel);
        Assert.assertEquals("Channel Endpoint{host=host1, port=1234, node=0} connecting problem", ex1.getMessage());

        lazy.shutdown();

        GrpcChannel eager = new GrpcChannel(endpoint, factoryMock, true);
        Assert.assertEquals(endpoint, eager.getEndpoint());

        RuntimeException ex2 = Assert.assertThrows(RuntimeException.class, eager::getReadyChannel);
        Assert.assertEquals("Channel Endpoint{host=host1, port=1234, node=0} connecting problem", ex2.getMessage());

        eager.shutdown();
    }
}
