package tech.ydb.core.impl.pool;


import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.google.common.io.ByteStreams;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.buffer.ByteBufAllocator;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DefaultChannelFactoryTest {
    private final static String MOCKED_HOST = "ydb.tech";
    private final static int MOCKED_PORT = 3345;
    private final static MockedStatic.Verification FOR_ADDRESS = () -> NettyChannelBuilder
            .forAddress(MOCKED_HOST, MOCKED_PORT);

    private AutoCloseable mocks;
    private MockedStatic<NettyChannelBuilder> channelStaticMock;
    private final NettyChannelBuilder channelBuilderMock = Mockito.mock(NettyChannelBuilder.class);
    private final ManagedChannel channelMock = Mockito.mock(ManagedChannel.class);

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        channelStaticMock = Mockito.mockStatic(NettyChannelBuilder.class);
        channelStaticMock.when(FOR_ADDRESS).thenReturn(channelBuilderMock);

        when(channelBuilderMock.negotiationType(any())).thenReturn(channelBuilderMock);
        when(channelBuilderMock.maxInboundMessageSize(anyInt())).thenReturn(channelBuilderMock);
        when(channelBuilderMock.withOption(any(), any())).thenReturn(channelBuilderMock);
        when(channelBuilderMock.intercept((ClientInterceptor)any())).thenReturn(channelBuilderMock);
        when(channelBuilderMock.nameResolverFactory(any())).thenReturn(channelBuilderMock);

        when(channelBuilderMock.build()).thenReturn(channelMock);
    }

    @After
    public void tearDown() throws Exception {
        channelStaticMock.close();
        mocks.close();
    }

    @Test
    public void defaultParams() {
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root");
        ManagedChannelFactory factory = ChannelFactoryLoader.load().buildFactory(builder);
        channelStaticMock.verify(FOR_ADDRESS, times(0));

        Assert.assertEquals(30_000l, factory.getConnectTimeoutMs());
        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, times(1));

        verify(channelBuilderMock, times(0)).negotiationType(NegotiationType.TLS);
        verify(channelBuilderMock, times(1)).negotiationType(NegotiationType.PLAINTEXT);
        verify(channelBuilderMock, times(1)).maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        verify(channelBuilderMock, times(1)).defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        verify(channelBuilderMock, times(1)).withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        verify(channelBuilderMock, times(0)).enableRetry();
        verify(channelBuilderMock, times(1)).disableRetry();
    }

    @Test
    public void defaultSslFactory() {
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                .withSecureConnection()
                .withGrpcRetry(true)
                .withConnectTimeout(Duration.ofMinutes(1));

        ManagedChannelFactory factory = ChannelFactoryLoader.load().buildFactory(builder);
        channelStaticMock.verify(FOR_ADDRESS, times(0));

        Assert.assertEquals(60000l, factory.getConnectTimeoutMs());
        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, times(1));

        verify(channelBuilderMock, times(1)).negotiationType(NegotiationType.TLS);
        verify(channelBuilderMock, times(0)).negotiationType(NegotiationType.PLAINTEXT);
        verify(channelBuilderMock, times(1)).maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        verify(channelBuilderMock, times(1)).defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        verify(channelBuilderMock, times(1)).withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        verify(channelBuilderMock, times(1)).enableRetry();
        verify(channelBuilderMock, times(0)).disableRetry();
    }

    @Test
    public void customChannelInitializer() {
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                .withUseDefaultGrpcResolver(true);

        ManagedChannelFactory factory = ShadedNettyChannelFactory
                .withInterceptor(cb -> cb.withOption(ChannelOption.TCP_NODELAY, Boolean.TRUE))
                .buildFactory(builder);

        channelStaticMock.verify(FOR_ADDRESS, times(0));

        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, times(1));

        verify(channelBuilderMock, times(1)).negotiationType(NegotiationType.PLAINTEXT);
        verify(channelBuilderMock, times(1)).maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        verify(channelBuilderMock, times(0)).defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        verify(channelBuilderMock, times(1)).withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        verify(channelBuilderMock, times(1)).withOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
    }

    @Test
    public void customSslFactory() throws CertificateException, IOException {
        SelfSignedCertificate selfSignedCert = new SelfSignedCertificate(MOCKED_HOST);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(new FileInputStream(selfSignedCert.certificate()), baos);

            GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                    .withSecureConnection(baos.toByteArray())
                    .withGrpcRetry(false)
                    .withConnectTimeout(4, TimeUnit.SECONDS);

            ManagedChannelFactory factory = ChannelFactoryLoader.load().buildFactory(builder);

            Assert.assertEquals(4000l, factory.getConnectTimeoutMs());
            Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        } finally {
            selfSignedCert.delete();
        }

        channelStaticMock.verify(FOR_ADDRESS, times(1));

        verify(channelBuilderMock, times(1)).negotiationType(NegotiationType.TLS);
        verify(channelBuilderMock, times(0)).negotiationType(NegotiationType.PLAINTEXT);
        verify(channelBuilderMock, times(1)).maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        verify(channelBuilderMock, times(1)).defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        verify(channelBuilderMock, times(1)).withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        verify(channelBuilderMock, times(0)).enableRetry();
        verify(channelBuilderMock, times(1)).disableRetry();
    }

    @Test
    public void invalidSslCert() {
        byte[] cert = new byte[] { 0x01, 0x02, 0x03 };
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                .withSecureConnection(cert);

        ManagedChannelFactory factory = ChannelFactoryLoader.load().buildFactory(builder);

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        Assert.assertEquals("cannot create ssl context", ex.getMessage());
        Assert.assertNotNull(ex.getCause());
        Assert.assertTrue(ex.getCause() instanceof IllegalArgumentException);
        Assert.assertEquals("Input stream does not contain valid certificates.", ex.getCause().getMessage());
    }
}
