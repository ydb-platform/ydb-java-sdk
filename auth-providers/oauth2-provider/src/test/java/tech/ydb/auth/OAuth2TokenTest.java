package tech.ydb.auth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OAuth2TokenTest {
    // Wednesday, June 1, 2022 00:00:00 UTC
    private static final Instant now = Instant.ofEpochSecond(1654041600);

    public static final String TEST_RSA_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC75/JS3rMcLJxv\n"
            + "FgpOzF5+2gH+Yig3RE2MTl9uwC0BZKAv6foYr7xywQyWIK+W1cBhz8R4LfFmZo2j\n"
            + "M0aCvdRmNBdW0EDSTnHLxCsFhoQWLVq+bI5f5jzkcoiioUtaEpADPqwgVULVtN/n\n"
            + "nPJiZ6/dU30C3jmR6+LUgEntUtWt3eq3xQIn5lG3zC1klBY/HxtfH5Hu8xBvwRQT\n"
            + "Jnh3UpPLj8XwSmriDgdrhR7o6umWyVuGrMKlLHmeivlfzjYtfzO1MOIMG8t2/zxG\n"
            + "R+xb4Vwks73sH1KruH/0/JMXU97npwpe+Um+uXhpldPygGErEia7abyZB2gMpXqr\n"
            + "WYKMo02NAgMBAAECggEAO0BpC5OYw/4XN/optu4/r91bupTGHKNHlsIR2rDzoBhU\n"
            + "YLd1evpTQJY6O07EP5pYZx9mUwUdtU4KRJeDGO/1/WJYp7HUdtxwirHpZP0lQn77\n"
            + "uccuX/QQaHLrPekBgz4ONk+5ZBqukAfQgM7fKYOLk41jgpeDbM2Ggb6QUSsJISEp\n"
            + "zrwpI/nNT/wn+Hvx4DxrzWU6wF+P8kl77UwPYlTA7GsT+T7eKGVH8xsxmK8pt6lg\n"
            + "svlBA5XosWBWUCGLgcBkAY5e4ZWbkdd183o+oMo78id6C+PQPE66PLDtHWfpRRmN\n"
            + "m6XC03x6NVhnfvfozoWnmS4+e4qj4F/emCHvn0GMywKBgQDLXlj7YPFVXxZpUvg/\n"
            + "rheVcCTGbNmQJ+4cZXx87huqwqKgkmtOyeWsRc7zYInYgraDrtCuDBCfP//ZzOh0\n"
            + "LxepYLTPk5eNn/GT+VVrqsy35Ccr60g7Lp/bzb1WxyhcLbo0KX7/6jl0lP+VKtdv\n"
            + "mto+4mbSBXSM1Y5BVVoVgJ3T/wKBgQDsiSvPRzVi5TTj13x67PFymTMx3HCe2WzH\n"
            + "JUyepCmVhTm482zW95pv6raDr5CTO6OYpHtc5sTTRhVYEZoEYFTM9Vw8faBtluWG\n"
            + "BjkRh4cIpoIARMn74YZKj0C/0vdX7SHdyBOU3bgRPHg08Hwu3xReqT1kEPSI/B2V\n"
            + "4pe5fVrucwKBgQCNFgUxUA3dJjyMES18MDDYUZaRug4tfiYouRdmLGIxUxozv6CG\n"
            + "ZnbZzwxFt+GpvPUV4f+P33rgoCvFU+yoPctyjE6j+0aW0DFucPmb2kBwCu5J/856\n"
            + "kFwCx3blbwFHAco+SdN7g2kcwgmV2MTg/lMOcU7XwUUcN0Obe7UlWbckzQKBgQDQ\n"
            + "nXaXHL24GGFaZe4y2JFmujmNy1dEsoye44W9ERpf9h1fwsoGmmCKPp90az5+rIXw\n"
            + "FXl8CUgk8lXW08db/r4r+ma8Lyx0GzcZyplAnaB5/6j+pazjSxfO4KOBy4Y89Tb+\n"
            + "TP0AOcCi6ws13bgY+sUTa/5qKA4UVw+c5zlb7nRpgwKBgGXAXhenFw1666482iiN\n"
            + "cHSgwc4ZHa1oL6aNJR1XWH+aboBSwR+feKHUPeT4jHgzRGo/aCNHD2FE5I8eBv33\n"
            + "of1kWYjAO0YdzeKrW0rTwfvt9gGg+CS397aWu4cy+mTI+MNfBgeDAIVBeJOJXLlX\n"
            + "hL8bFAuNNVrCOp79TNnNIsh7\n"
            + "-----END PRIVATE KEY-----\n";

    public static final String TEST_RSA_PRIVATE_KEY_JSON = TEST_RSA_PRIVATE_KEY.replace("\n", "\\n");

    public static final String TEST_RSA_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu+fyUt6zHCycbxYKTsxe\n"
            + "ftoB/mIoN0RNjE5fbsAtAWSgL+n6GK+8csEMliCvltXAYc/EeC3xZmaNozNGgr3U\n"
            + "ZjQXVtBA0k5xy8QrBYaEFi1avmyOX+Y85HKIoqFLWhKQAz6sIFVC1bTf55zyYmev\n"
            + "3VN9At45kevi1IBJ7VLVrd3qt8UCJ+ZRt8wtZJQWPx8bXx+R7vMQb8EUEyZ4d1KT\n"
            + "y4/F8Epq4g4Ha4Ue6OrplslbhqzCpSx5nor5X842LX8ztTDiDBvLdv88RkfsW+Fc\n"
            + "JLO97B9Sq7h/9PyTF1Pe56cKXvlJvrl4aZXT8oBhKxImu2m8mQdoDKV6q1mCjKNN\n"
            + "jQIDAQAB\n"
            + "-----END PUBLIC KEY-----\n";

    public static final String TEST_EC_PRIVATE_KEY = "-----BEGIN EC PRIVATE KEY-----\n"
                + "MHcCAQEEIB6fv25gf7P/7fkjW/2kcKICUhHeOygkFeUJ/ylyU3hloAoGCCqGSM49\n"
                + "AwEHoUQDQgAEvkKy92hpLiT0GEpzFkYBEWWnkAGTTA6141H0oInA9X30eS0RObAa\n"
                + "mVY8yD39NI7Nj03hBxEa4Z0tOhrq9cW8eg==\n"
                + "-----END EC PRIVATE KEY-----\n";

    public static final String TEST_EC_PRIVATE_KEY_JSON = TEST_EC_PRIVATE_KEY.replace("\n", "\\n");

    public static final String TEST_EC_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
                + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEvkKy92hpLiT0GEpzFkYBEWWnkAGT\n"
                + "TA6141H0oInA9X30eS0RObAamVY8yD39NI7Nj03hBxEa4Z0tOhrq9cW8eg==\n"
                + "-----END PUBLIC KEY-----\n";

    public static final String TEST_HMAC_SECRET_KEY_BASE64 = "VGhlIHdvcmxkIGhhcyBjaGFuZ2VkLgpJIHNlZSBpdCBpbiB0aGUgd2F0ZXIuCkkgZmVlbCBpdCBpbiB0aGUgRWFydGguCkkgc21lbGwgaXQgaW4gdGhlIGFpci4KTXVjaCB0aGF0IG9uY2Ugd2FzIGlzIGxvc3QsCkZvciBub25lIG5vdyBsaXZlIHdobyByZW1lbWJlciBpdC4K";

    public static Key getPrivateKeyFromPem(String pemContent) {
        try (PEMParser parser = new PEMParser(new StringReader(pemContent))) {
            Object parsed = parser.readObject();
            if (parsed == null) {
                throw new RuntimeException("Failed to parse PEM private key");
            }

            PrivateKeyInfo info = null;
            if (parsed instanceof PrivateKeyInfo) {
                info = (PrivateKeyInfo) parsed;
            } else if (parsed instanceof PEMKeyPair) {
                PEMKeyPair keyPair = (PEMKeyPair) parsed;
                info = keyPair.getPrivateKeyInfo();
            } else {
                throw new RuntimeException("Unknown private key PEM format");
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getPrivateKey(info);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read key: %s", e.getMessage()), e);
        }
    }

    public static Key getPublicKeyFromPem(String pemContent) {
        try (PEMParser parser = new PEMParser(new StringReader(pemContent))) {
            Object parsed = parser.readObject();
            if (parsed == null) {
                throw new RuntimeException("Failed to parse PEM key");
            }

            if (parsed instanceof SubjectPublicKeyInfo) {
                SubjectPublicKeyInfo info = (SubjectPublicKeyInfo) parsed;
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                return converter.getPublicKey(info);
            } else {
                throw new RuntimeException("Unknown public key PEM format");
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read punlic key: %s", e.getMessage()), e);
        }
    }

    public static Key getHmacKey(String alg, String base64Content) {
        byte[] bytes  = Base64.getDecoder().decode(base64Content);
        SignatureAlgorithm signatureAlgorithm = null;
        if (alg.equals("HS256")) {
            signatureAlgorithm = SignatureAlgorithm.HS256;
        }
        if (alg.equals("HS384")) {
            signatureAlgorithm = SignatureAlgorithm.HS384;
        }
        if (alg.equals("HS512")) {
            signatureAlgorithm = SignatureAlgorithm.HS512;
        }
        Assert.assertNotNull(signatureAlgorithm);
        return new SecretKeySpec(bytes, signatureAlgorithm.getJcaName());
    }

    @Test
    public void readFromFile() throws IOException {
        File file = File.createTempFile("test-oauth2", "token");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("token_from_file");
        }

        OAuth2TokenSource token = OAuth2TokenSource.fromFile(file);

        Assert.assertEquals("token_from_file", token.getToken());
        Assert.assertEquals(OAuth2TokenSource.JWT_TOKEN, token.getType());

        file.delete();
    }

    @Test
    public void readKeyFromFileTest() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        File file = File.createTempFile("test", "oauth2-key.pem");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(TEST_RSA_PRIVATE_KEY);
        }

        OAuth2TokenSource token = OAuth2TokenSource.withPrivateKeyPemFile(file).build();
        String jwt = token.getToken();

        Jwt<?, Claims> parsed = Jwts.parser()
                .setSigningKey(getPublicKeyFromPem(TEST_RSA_PUBLIC_KEY))
                .parseClaimsJws(jwt);

        Assert.assertEquals("JWT", parsed.getHeader().getType());
        Assert.assertEquals("RS256", parsed.getHeader().get("alg"));
        file.delete();
    }

    @Test
    public void jwsTokenTest() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Clock clock = Mockito.mock(Clock.class);
        Mockito.when(clock.instant()).thenReturn(now);

        OAuth2TokenSource token = OAuth2TokenSource.fromKey(getPrivateKeyFromPem(TEST_RSA_PRIVATE_KEY))
                .withAudience("testAudience")
                .withIssuer("junitIssuer")
                .withId("test")
                .withKeyId("keyID")
                .withSubject("subj")
                .withClaim("c1", "value1")
                .withClaim("c2", "value2")
                .withTtlSeconds(20)
                .withClock(clock)
                .build();

        String jwt = token.getToken();

        Jwt<?, Claims> parsed = Jwts.parser()
                .setClock(() -> Date.from(clock.instant()))
                .setSigningKey(getPublicKeyFromPem(TEST_RSA_PUBLIC_KEY))
                .parseClaimsJws(jwt);

        Assert.assertEquals("JWT", parsed.getHeader().getType());
        Assert.assertEquals("RS256", parsed.getHeader().get("alg"));
        Assert.assertEquals("keyID", parsed.getHeader().get("kid"));

        Assert.assertEquals("testAudience", parsed.getBody().getAudience());
        Assert.assertEquals("junitIssuer", parsed.getBody().getIssuer());
        Assert.assertEquals("test", parsed.getBody().getId());
        Assert.assertEquals("subj", parsed.getBody().getSubject());
        Assert.assertEquals("value1", parsed.getBody().get("c1", String.class));
        Assert.assertEquals("value2", parsed.getBody().get("c2", String.class));

        Assert.assertEquals(Date.from(now), parsed.getBody().getIssuedAt());
        Assert.assertEquals(Date.from(now.plusSeconds(20)), parsed.getBody().getExpiration());
    }

}
