package tech.ydb.test.integration;

import java.util.UUID;

import com.github.dockerjava.api.DockerClient;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testcontainers.utility.ResourceReaper;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DockerMock implements AutoCloseable {
    public final static String UUID_MOCKED = "20354d7a-e4fe-47af-8ff6-187bca92f3f9";

    private final MockedStatic<UUID> staticUUID;
    private final MockedStatic<TestcontainersConfiguration> staticConfiguration;
    private final MockedStatic<DockerClientFactory> staticDockerFactory;
    private final MockedStatic<ResourceReaper> staticResourceReaper;
    private final MockedStatic<ImageNameSubstitutor> staticImageNameSubstitutor;

    private final TestcontainersConfiguration configration = Mockito.mock(TestcontainersConfiguration.class);
    private final DockerClientFactory dockerClientFactory = Mockito.mock(DockerClientFactory.class);
    private final ResourceReaper resourceReaper = Mockito.mock(ResourceReaper.class);
    private final DockerClient dockerClient = Mockito.mock(DockerClient.class);
    private final ImageNameSubstitutor nameSubtitutor = Mockito.mock(ImageNameSubstitutor.class, Answers.CALLS_REAL_METHODS);

    public DockerMock() {
        UUID mocked = UUID.fromString(UUID_MOCKED);
        staticUUID = Mockito.mockStatic(UUID.class, Answers.CALLS_REAL_METHODS);
        staticUUID.when(UUID::randomUUID).thenReturn(mocked);

        staticConfiguration = Mockito.mockStatic(TestcontainersConfiguration.class);
        staticDockerFactory = Mockito.mockStatic(DockerClientFactory.class);
        staticResourceReaper = Mockito.mockStatic(ResourceReaper.class);
        staticImageNameSubstitutor = Mockito.mockStatic(ImageNameSubstitutor.class);
    }

    public void setup(Boolean enabled) {
        setup(enabled, Boolean.FALSE);
    }

    public void setup(Boolean enabled, Boolean reuseSupport) {
        staticDockerFactory.when(DockerClientFactory::instance).thenReturn(dockerClientFactory);
        staticDockerFactory.when(DockerClientFactory::lazyClient).thenReturn(dockerClient);

        staticResourceReaper.when(ResourceReaper::instance).thenReturn(resourceReaper);
        staticConfiguration.when(TestcontainersConfiguration::getInstance).thenReturn(configration);
        staticImageNameSubstitutor.when(ImageNameSubstitutor::instance).thenReturn(nameSubtitutor);

        Mockito.when(dockerClientFactory.client()).thenReturn(dockerClient);
        Mockito.when(dockerClientFactory.isDockerAvailable()).thenReturn(enabled);

        Mockito.doNothing().when(resourceReaper).registerLabelsFilterForCleanup(Mockito.anyMap());

        Mockito.when(configration.environmentSupportsReuse()).thenReturn(reuseSupport);
        Mockito.when(configration.getImagePullTimeout()).thenReturn(120);
    }

    @Override
    public void close() {
        staticImageNameSubstitutor.close();
        staticResourceReaper.close();
        staticDockerFactory.close();
        staticConfiguration.close();
        staticUUID.close();
    }
}
