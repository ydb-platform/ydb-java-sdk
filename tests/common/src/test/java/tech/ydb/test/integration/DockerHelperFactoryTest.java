package tech.ydb.test.integration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.core.CreateContainerCmdModifier;
import org.testcontainers.utility.ThrowingFunction;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.integration.docker.DockerHelperFactory;
import tech.ydb.test.integration.docker.YdbDockerContainer;
import tech.ydb.test.integration.utils.PortsGenerator;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DockerHelperFactoryTest {
    private final class YdbMockContainer extends YdbDockerContainer {
        private final byte[] pemCert = new byte[] { 0x04, 0x03, 0x02, 0x01 };
        private final String pemPath;

        private int starts = 0;
        private int stops = 0;

        public YdbMockContainer(YdbEnvironment env, PortsGenerator portsGenerator) {
            super(env, portsGenerator);
            this.pemPath = env.dockerPemPath();
        }

        @Override
        public void start() {
            starts += 1;
        }

        @Override
        public void stop() {
            stops += 1;
        }

        @Override
        public void close() {
            // Nothing
        }

        @Override
        public Integer getMappedPort(int port) {
            return 100 + port;
        }

        @Override
        public String getHost() {
            return "mocked";
        }

        @Override
        public <T> T copyFileFromContainer(String containerPath, ThrowingFunction<InputStream, T> function) {
            if (!pemPath.equals(containerPath)) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream("".getBytes())) {
                    return function.apply(bais);
                } catch (Exception ex) {
                    throw new AssertionError("mock error", ex);
                }
            }
            Assert.assertEquals("check pem path", pemPath, containerPath);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(pemCert)) {
                return function.apply(bais);
            } catch (Exception ex) {
                throw new AssertionError("mock error", ex);
            }
        }
    }

    private final DockerMock dockerMock = new DockerMock();
    private final GrpcTransportMock transportMock = new GrpcTransportMock();

    @After
    public void cleanup() {
        dockerMock.close();
        transportMock.close();
    }

    private void assertCreateContainerCmdModifiers(YdbMockContainer container) {
        CreateContainerCmd cmd = Mockito.mock(CreateContainerCmd.class);
        Mockito.when(cmd.withName(Mockito.any())).thenReturn(cmd);
        Mockito.when(cmd.withHostName(Mockito.any())).thenReturn(cmd);

        for (CreateContainerCmdModifier modifier: container.getCreateContainerCmdModifiers()) {
            modifier.modify(cmd);
        }

        Mockito.verify(cmd, Mockito.times(1)).withName("ydb-" + DockerMock.UUID_MOCKED); // from random UUID
        Mockito.verify(cmd, Mockito.times(1)).withHostName("mocked"); // from mock
    }

    @Test
    public void defaultDockerContainerTests() {
        dockerMock.setup(Boolean.TRUE);
        transportMock.setup("/local");

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);
        Mockito.when(ports.findAvailablePort()).thenReturn(/* Secure */ 10, /* Insecure */ 11);

        YdbEnvironmentMock env = new YdbEnvironmentMock();
        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertFalse("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:111", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 1, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 1, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertFalse("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:111", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 2, container.stops);
    }

    @Test
    public void tlsDockerContainerTests() {
        dockerMock.setup(Boolean.TRUE);
        transportMock.setup("/local");

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);
        Mockito.when(ports.findAvailablePort()).thenReturn(/* Secure */ 22, /* Insecure */ 33);

        YdbEnvironmentMock env = new YdbEnvironmentMock()
                .withUseTLS(true)
                .withToken("SIMPLE_TOKEN")
                .withFeatures("enable_views")
                .withDockerReuse(false);

        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:122", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 1, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 1, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:122", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 2, container.stops);
    }

    @Test
    public void reuseDockerContainerTests() {
        dockerMock.setup(Boolean.TRUE, Boolean.TRUE);
        transportMock.setup("/local");

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);
        Mockito.when(ports.findAvailablePort()).thenReturn(/* Secure */ 41, /* Insecure */ 44);

        YdbEnvironmentMock env = new YdbEnvironmentMock()
                .withUseTLS(true)
                .withDockerReuse(true);

        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:141", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:141", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);
    }

    @Test
    public void dockerIsolationContainerTests() {
        dockerMock.setup(Boolean.TRUE, Boolean.TRUE);
        transportMock.setup("/local");

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);

        YdbEnvironmentMock env = new YdbEnvironmentMock()
                .withUseTLS(true)
                .withDockerReuse(true)
                .withDockerIsolation(true);

        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:2235", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            assertCreateContainerCmdModifiers(container);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:2235", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());

            try (GrpcTransport transport = helper.createTransport()) {
                Assert.assertEquals("/local", transport.getDatabase());
                Assert.assertTrue(transport.unaryCall(null, null, null).join().isSuccess());
            }
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);
    }
}
