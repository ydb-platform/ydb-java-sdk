package tech.ydb.core.auth;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;

import tech.ydb.OperationProtos;
import tech.ydb.StatusCodesProtos;
import tech.ydb.auth.YdbAuth;
import tech.ydb.auth.v1.AuthServiceGrpc;
import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;

import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CredentialsAuthProviderTest {
    private final Clock clock = Mockito.mock(Clock.class);
    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final AuthRpc rpc = Mockito.mock(AuthRpc.class);

    @Before
    public void setup() {
        Mockito.when(rpc.getEndpoint()).thenReturn("Mocked endpoint");
        Mockito.when(rpc.getDatabase()).thenReturn("Mocked database name");
        Mockito.when(rpc.createTransport()).thenReturn(transport);
    }
    
    @Test
    public void testOK() {
        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()),
                Mockito.any(),
                Mockito.argThat(req -> req.getUser().equals("user") && req.getPassword().equals("pass1"))
        )).thenReturn(CompletableFuture.completedFuture(Result.success(goodAnswer())));

        AuthProvider provider = new CredentialsAuthProvider(clock, "user", "pass1");
        AuthIdentity identity = provider.createAuthIdentity(rpc);
        
        Assert.assertEquals("Invalid token", "TOKEN", identity.getToken());
        
        identity.close();
    }
    
    public static YdbAuth.LoginResponse goodAnswer() {
        YdbAuth.LoginResult result = YdbAuth.LoginResult.newBuilder()
                .setToken("TOKEN")
                .build();

        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setId("good_id")
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setReady(true)
                .setResult(Any.pack(result))
                .build();

        return YdbAuth.LoginResponse.newBuilder()
                .setOperation(operation)
                .build();
    }
}
