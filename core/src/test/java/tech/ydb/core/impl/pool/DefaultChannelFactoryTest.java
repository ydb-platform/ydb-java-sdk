package tech.ydb.core.impl.pool;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.CertificateException;

import com.google.common.io.ByteStreams;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingChannelBuilder2;
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
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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

        Mockito.when(channelBuilderMock.negotiationType(ArgumentMatchers.any()))
                .thenReturn(channelBuilderMock);
        Mockito.when(channelBuilderMock.maxInboundMessageSize(ArgumentMatchers.anyInt()))
                .thenReturn(channelBuilderMock);
        Mockito.when(channelBuilderMock.withOption(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(channelBuilderMock);
        Mockito.when(channelBuilderMock.intercept(ArgumentMatchers.any(ClientInterceptor.class)))
                .thenReturn(channelBuilderMock);
        Mockito.when(channelBuilderMock.nameResolverFactory(ArgumentMatchers.any()))
                .thenReturn(channelBuilderMock);
        Mockito.when(channelBuilderMock.keepAliveTime(ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenReturn(channelBuilderMock);

        Mockito.when(channelBuilderMock.build()).thenReturn(channelMock);
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
        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(0));

        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(1));

        Mockito.verify(channelBuilderMock, Mockito.times(0)).negotiationType(NegotiationType.TLS);
        Mockito.verify(channelBuilderMock, Mockito.times(1)).negotiationType(NegotiationType.PLAINTEXT);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        Mockito.verify(channelBuilderMock, Mockito.times(0)).enableRetry();
        Mockito.verify(channelBuilderMock, Mockito.times(1)).disableRetry();
    }

    @Test
    public void defaultSslFactory() {
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                .withSecureConnection()
                .withGrpcRetry(true);

        ManagedChannelFactory factory = ChannelFactoryLoader.load().buildFactory(builder);
        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(0));

        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(1));

        Mockito.verify(channelBuilderMock, Mockito.times(1)).negotiationType(NegotiationType.TLS);
        Mockito.verify(channelBuilderMock, Mockito.times(0)).negotiationType(NegotiationType.PLAINTEXT);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        Mockito.verify(channelBuilderMock, Mockito.times(1)).enableRetry();
        Mockito.verify(channelBuilderMock, Mockito.times(0)).disableRetry();
    }

    @Test
    public void customChannelInitializer() {
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                .withUseDefaultGrpcResolver(true);

        ManagedChannelFactory factory = ShadedNettyChannelFactory
                .withInterceptor(ForwardingChannelBuilder2::useTransportSecurity)
                .buildFactory(builder);

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(0));

        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(1));

        Mockito.verify(channelBuilderMock, Mockito.times(1)).negotiationType(NegotiationType.PLAINTEXT);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        Mockito.verify(channelBuilderMock, Mockito.times(0))
                .defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        Mockito.verify(channelBuilderMock, Mockito.times(1)).withOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        Mockito.verify(channelBuilderMock, Mockito.times(1)).useTransportSecurity();
    }

    @Test
    public void addChannelInitializerTest() {
        GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                .withUseDefaultGrpcResolver(true)
                .addChannelInitializer(ci -> ci.usePlaintext())
                .addChannelInitializer(ci -> ci.userAgent("TEST"));

        ManagedChannelFactory factory = ShadedNettyChannelFactory.build()
                .buildFactory(builder);

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(0));

        Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(1));

        Mockito.verify(channelBuilderMock, Mockito.times(1)).negotiationType(NegotiationType.PLAINTEXT);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        Mockito.verify(channelBuilderMock, Mockito.times(0))
                .defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        Mockito.verify(channelBuilderMock, Mockito.times(1)).withOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        Mockito.verify(channelBuilderMock, Mockito.times(1)).usePlaintext();
        Mockito.verify(channelBuilderMock, Mockito.times(1)).userAgent("TEST");
    }

    @Test
    public void customSslFactory() throws CertificateException, IOException {
        SelfSignedCertificate selfSignedCert = new SelfSignedCertificate(MOCKED_HOST);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(Files.newInputStream(selfSignedCert.certificate().toPath()), baos);

            GrpcTransportBuilder builder = GrpcTransport.forHost(MOCKED_HOST, MOCKED_PORT, "/Root")
                    .withSecureConnection(baos.toByteArray())
                    .withGrpcRetry(false);

            ManagedChannelFactory factory = ChannelFactoryLoader.load().buildFactory(builder);

            Assert.assertSame(channelMock, factory.newManagedChannel(MOCKED_HOST, MOCKED_PORT, null));

        } finally {
            selfSignedCert.delete();
        }

        channelStaticMock.verify(FOR_ADDRESS, Mockito.times(1));

        Mockito.verify(channelBuilderMock, Mockito.times(1)).negotiationType(NegotiationType.TLS);
        Mockito.verify(channelBuilderMock, Mockito.times(0)).negotiationType(NegotiationType.PLAINTEXT);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .maxInboundMessageSize(ShadedNettyChannelFactory.INBOUND_MESSAGE_SIZE);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .defaultLoadBalancingPolicy(ShadedNettyChannelFactory.DEFAULT_BALANCER_POLICY);
        Mockito.verify(channelBuilderMock, Mockito.times(1))
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        Mockito.verify(channelBuilderMock, Mockito.times(0)).enableRetry();
        Mockito.verify(channelBuilderMock, Mockito.times(1)).disableRetry();
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
