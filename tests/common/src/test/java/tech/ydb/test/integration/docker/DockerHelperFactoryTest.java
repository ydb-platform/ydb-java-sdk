package tech.ydb.test.integration.docker;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.utility.ThrowingFunction;

import tech.ydb.test.integration.DockerMock;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbEnvironmentMock;
import tech.ydb.test.integration.YdbHelper;

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
        public String getHost() {
            return "mocked";
        }

        @Override
        public <T> T copyFileFromContainer(String containerPath, ThrowingFunction<InputStream, T> function) {
            Assert.assertEquals("check pem path", pemPath, containerPath);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(pemCert)) {
                return function.apply(bais);
            } catch (Exception ex) {
                throw new AssertionError("mock error", ex);
            }
        }
    }

    private final DockerMock dockerMock = new DockerMock();

    @After
    public void cleanup() {
        dockerMock.close();
    }

    @Test
    public void defaultDockerContainerTests() {
        dockerMock.setup(Boolean.TRUE);

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);
        Mockito.when(ports.findAvailablePort()).thenReturn(/* Secure */ 10, /* Insecure */ 11);

        YdbEnvironmentMock env = YdbEnvironmentMock.create();
        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            Assert.assertFalse("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:11", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 1, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 1, container.stops);

            Assert.assertFalse("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:11", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 2, container.stops);
    }

    @Test
    public void tlsDockerContainerTests() {
        dockerMock.setup(Boolean.TRUE);

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);
        Mockito.when(ports.findAvailablePort()).thenReturn(/* Secure */ 22, /* Insecure */ 33);

        YdbEnvironmentMock env = YdbEnvironmentMock.create()
                .with("YDB_USE_TLS", "True")
                .with("YDB_TOKEN", "SIMPLE_TOKEN");

        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:22", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 1, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 1, container.stops);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:22", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 2, container.stops);
    }

    @Test
    public void reuseDockerContainerTests() {
        dockerMock.setup(Boolean.TRUE, Boolean.TRUE);

        PortsGenerator ports = Mockito.mock(PortsGenerator.class);
        Mockito.when(ports.findAvailablePort()).thenReturn(/* Secure */ 41, /* Insecure */ 44);

        YdbEnvironmentMock env = YdbEnvironmentMock.create()
                .with("YDB_USE_TLS", "True")
                .with("YDB_DOCKER_REUSE", "true");

        YdbMockContainer container = new YdbMockContainer(env, ports);
        DockerHelperFactory factory = new DockerHelperFactory(env, container);

        Assert.assertEquals("check container is started", 0, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 1, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:41", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());
        }

        Assert.assertEquals("check container is started", 1, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);

        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertEquals("check container is started", 2, container.starts);
            Assert.assertEquals("check container is stopped", 0, container.stops);

            Assert.assertTrue("check helper use tls", helper.useTls());
            Assert.assertEquals("check helper endpoint", "mocked:41", helper.endpoint());
            Assert.assertEquals("check helper database", "/local", helper.database());
            Assert.assertNull("check helper auth token", helper.authToken());
            Assert.assertArrayEquals("check helper database", container.pemCert, helper.pemCert());
        }

        Assert.assertEquals("check container is started", 2, container.starts);
        Assert.assertEquals("check container is stopped", 0, container.stops);
    }
}
