package tech.ydb.test.integration;

import com.github.dockerjava.api.DockerClient;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.ResourceReaper;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DockerMock implements AutoCloseable {
    private final MockedStatic<TestcontainersConfiguration> staticConfiguration =
            Mockito.mockStatic(TestcontainersConfiguration.class);
    private final MockedStatic<DockerClientFactory> staticDockerFactory =
            Mockito.mockStatic(DockerClientFactory.class);
    private final MockedStatic<ResourceReaper> staticResourceReaper =
            Mockito.mockStatic(ResourceReaper.class);

    private final TestcontainersConfiguration configration = Mockito.mock(TestcontainersConfiguration.class);
    private final DockerClientFactory dockerClientFactory = Mockito.mock(DockerClientFactory.class);
    private final ResourceReaper resourceReaper = Mockito.mock(ResourceReaper.class);
    private final DockerClient dockerClient = Mockito.mock(DockerClient.class);

    public void setup(Boolean enabled) {
        setup(enabled, Boolean.FALSE);
    }

    public void setup(Boolean enabled, Boolean reuseSupport) {
        staticDockerFactory.when(DockerClientFactory::instance).thenReturn(dockerClientFactory);
        staticDockerFactory.when(DockerClientFactory::lazyClient).thenReturn(dockerClient);

        staticResourceReaper.when(ResourceReaper::instance).thenReturn(resourceReaper);
        staticConfiguration.when(TestcontainersConfiguration::getInstance).thenReturn(configration);

        Mockito.when(dockerClientFactory.client()).thenReturn(dockerClient);
        Mockito.when(dockerClientFactory.isDockerAvailable()).thenReturn(enabled);

        Mockito.doNothing().when(resourceReaper).registerLabelsFilterForCleanup(Mockito.anyMap());

        Mockito.when(configration.environmentSupportsReuse()).thenReturn(reuseSupport);
    }

    @Override
    public void close() {
        staticResourceReaper.close();
        staticDockerFactory.close();
        staticConfiguration.close();
    }
}
