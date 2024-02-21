package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.proto.coordination.CreateNodeRequest;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ClientTest {
    private final Rpc rpc = Mockito.mock(Rpc.class);
    private final Client client = new Client(rpc);

    @Before
    public void setUp() {
        Mockito.when(rpc.createNode(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        Mockito.when(rpc.getDatabase()).thenReturn("/mocked");
    }

    @Test
    public void validatePathTest() {
        ArgumentCaptor<CreateNodeRequest> requestCapture = ArgumentCaptor.forClass(CreateNodeRequest.class);

        client.createNode("test");
        client.createNode("/test");

        Mockito.verify(rpc, Mockito.times(2)).createNode(requestCapture.capture(), Mockito.any());

        Assert.assertEquals("/mocked/test", requestCapture.getAllValues().get(0).getPath());
        Assert.assertEquals("/test", requestCapture.getAllValues().get(1).getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPathError() {
        client.createNode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPathError() {
        client.createNode("");
    }
}
