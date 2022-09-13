package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    
    // Wednesday, June 1, 2022 00:00:00 UTC
    private final Instant now = Instant.ofEpochSecond(1654041600);

    @Before
    public void setup() {
        Mockito.when(rpc.getEndpoint()).thenReturn("Mocked endpoint");
        Mockito.when(rpc.getDatabase()).thenReturn("Mocked database name");
        Mockito.when(rpc.createTransport()).thenReturn(transport);
    }
    
    @Test
    public void testOK() {
        String token = JwtBuilder.create(now.plus(Duration.ofHours(2)), now);

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()),
                Mockito.any(),
                Mockito.argThat(req -> req.getUser().equals("user") && req.getPassword().equals("pass1"))
        )).thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token))));
        Mockito.when(clock.instant()).thenReturn(now);

        AuthProvider provider = new StaticCredentials(clock, "user", "pass1");
        AuthIdentity identity = provider.createAuthIdentity(rpc);
        
        Assert.assertEquals("Invalid token", token, identity.getToken());
        
        identity.close();
    }
    
    public static YdbAuth.LoginResponse responseOk(String token) {
        YdbAuth.LoginResult result = YdbAuth.LoginResult.newBuilder()
                .setToken(token)
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
