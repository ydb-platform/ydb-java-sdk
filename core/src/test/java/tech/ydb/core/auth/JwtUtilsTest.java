package tech.ydb.core.auth;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class JwtUtilsTest {

    @Test
    public void parseTest() {
        // { "alg": "HS256", "typ": "JWT" }.{ "sub": "1234567890", "iat": 1516239022, "exp": 1726544488 }
        String jwt1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE3MjY1NDQ0ODh9";
        // { "alg": "HS256", "typ": "JWT" }.{ "aud":  "Base", "sub": "1234567890", "iat": 1516239022, "exp": 1726544488 }
        String jwt2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJCYXNlIiwic3ViIjoiMTIzNDU2Nzg5MCIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNzI2NTQ0NDg4fQ";
        // { "alg": "HS256", "typ": "JWT" }.{ "aud": [ "Base" ], "sub": "1234567890", "iat": 1516239022, "exp": 1726544488 }
        String jwt3 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiQmFzZSJdLCJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE3MjY1NDQ0ODh9";

        Assert.assertEquals(Instant.ofEpochSecond(1726544488), JwtUtils.extractExpireAt(jwt1, Instant.EPOCH));
        Assert.assertEquals(Instant.ofEpochSecond(1726544488), JwtUtils.extractExpireAt(jwt2, Instant.EPOCH));
        Assert.assertEquals(Instant.ofEpochSecond(1726544488), JwtUtils.extractExpireAt(jwt3, Instant.EPOCH));
    }

    @Test
    public void parseBase64UrlPayloadTest() {
        // JWT payloads use the base64url alphabet, so they may contain '-' and '_' instead of '+' and '/'
        // { "alg": "HS256", "typ": "JWT" }.{ "sub": "a?b>c", "aud": "x?y?z", "exp": 1726544488 }
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                + ".eyJzdWIiOiJhP2I-YyIsImF1ZCI6Ing_eT96IiwiZXhwIjoxNzI2NTQ0NDg4fQ";

        Assert.assertEquals(Instant.ofEpochSecond(1726544488), JwtUtils.extractExpireAt(jwt, Instant.EPOCH));
    }

    @Test
    public void unparsablePayloadReturnsDefaultTest() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.!not-a-base64-payload!";

        Assert.assertEquals(Instant.EPOCH, JwtUtils.extractExpireAt(jwt, Instant.EPOCH));
    }
}
