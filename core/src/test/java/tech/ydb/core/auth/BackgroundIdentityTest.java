package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class BackgroundIdentityTest {
    private final Instant now = Instant.EPOCH;
    private final Clock clock = Clock.fixed(now, ZoneId.of("UTC"));

    private static class MockedRpc implements BackgroundIdentity.Rpc {
        private final CompletableFuture<Token> tokenFuture = new CompletableFuture<>();

        @Override
        public CompletableFuture<Token> getTokenAsync() {
            return tokenFuture;
        }

        @Override
        public int getTimeoutSeconds() {
            return 60;
        }
    }

    @Test(timeout = 30_000)
    public void interruptedGetTokenDoesNotBreakIdentity() throws InterruptedException {
        MockedRpc rpc = new MockedRpc();
        BackgroundIdentity identity = new BackgroundIdentity(clock, rpc);

        AtomicReference<Throwable> caught = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                identity.getToken();
            } catch (Throwable th) {
                caught.set(th);
            }
        });

        // the login never answers, so the reader blocks in the sync state and gets interrupted there
        reader.start();
        // the reader may not have reached the await yet, interrupting early is handled the same way
        reader.interrupt();
        reader.join();

        Assert.assertNotNull("interrupted getToken must report a failure", caught.get());
        Assert.assertFalse(
                "interrupt must not surface as a NullPointerException",
                caught.get() instanceof NullPointerException
        );

        // the identity must recover once the login completes
        rpc.getTokenAsync().complete(
                new BackgroundIdentity.Rpc.Token(
                        "token-value",
                        now.plus(Duration.ofHours(2)),
                        now.plus(Duration.ofHours(1))
                )
        );

        Assert.assertEquals("token-value", identity.getToken());
    }
}
