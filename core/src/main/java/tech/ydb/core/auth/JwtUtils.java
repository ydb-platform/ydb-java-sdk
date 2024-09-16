package tech.ydb.core.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private static final char SEPARATOR_CHAR = '.';
    private static final Gson GSON = new Gson();

    private JwtUtils() { }

    private static class JwtClaims {
        @SerializedName("exp")
        private Long expiredAt;

        public Long getExpiredAt() {
            return this.expiredAt;
        }
    }

    public static Instant extractExpireAt(String jwt, Instant defaultValue) {
        if (jwt == null) {
            return defaultValue;
        }

        String[] parts = new String[3];

        int nextPart = 0;
        int startOfPart = 0;
        for (int idx = 0; idx < jwt.length() && nextPart < 3; idx += 1) {
            if (jwt.charAt(idx) == SEPARATOR_CHAR) {
                if (startOfPart < idx) {
                    parts[nextPart] = jwt.substring(startOfPart, idx);
                    nextPart += 1;
                }
                startOfPart = idx + 1;
            }
        }

        if (startOfPart < jwt.length() && nextPart < 3) {
            parts[nextPart] = jwt.substring(startOfPart);
            nextPart += 1;
        }

        if (nextPart < 2) {
            return defaultValue;
        }

        try {
            String payload = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JwtClaims claims = GSON.fromJson(payload, JwtClaims.class);
            if (claims != null && claims.getExpiredAt() != null) {
                return Instant.ofEpochSecond(claims.getExpiredAt());
            }
        } catch (IllegalArgumentException | JsonSyntaxException ex) {
            logger.error("can't get expire from jwt {}", jwt, ex);
        }

        return defaultValue;
    }
}
