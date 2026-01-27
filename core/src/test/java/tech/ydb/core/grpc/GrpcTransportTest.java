package tech.ydb.core.grpc;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mikhail Firsov
 */
public class GrpcTransportTest {

    @Test
    public void failFastOnMissingPort() {
        String endpoint = "127.1.2.3";
        try {
            GrpcTransport.forEndpoint(endpoint, "test").build().close();
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Can't create discovery rpc, port is not specified for endpoint 127.1.2.3", e.getMessage());
        }
    }

    @Test
    public void doNotFailIfEndpointHasPort() {
        String endpoint = "127.1.2.3:12345";
        GrpcTransport
                .forEndpoint(endpoint, "test")
                .withInitMode(GrpcTransportBuilder.InitMode.ASYNC)
                .build()
                .close();
    }
}
