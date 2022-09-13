package tech.ydb.core.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class JwtBuilder {
    // {"typ":"JWT","alg":"none"}
    private final static String PREFIX = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.";
    
    public static String create(Instant expiredAt, Instant issuedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"exp\":").append(expiredAt.getEpochSecond()).append(",");
        sb.append("\"iat\":").append(issuedAt.getEpochSecond()).append(",");
        sb.append("\"iss\":\"").append("MOCK").append("\"");
        sb.append("}");
        
        String base64 = Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
        
        System.out.println(sb.toString());
        return PREFIX + base64;
    }
}
