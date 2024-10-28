package tech.ydb.core.auth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

import tech.ydb.auth.AuthIdentity;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.impl.auth.GrpcAuthRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class EnvironAuthProvider implements AuthRpcProvider<GrpcAuthRpc> {
    private static final String IAM_CLASS_NAME = "tech.ydb.auth.iam.CloudAuthIdentity";
    private static final String IAM_CLASS_ERROR = "Cannot find CloudAuthIdentity class, "
            + "you have to add tech.ydb.auth:yc-auth-provider artifact to classpath";

    private static final String OAUTH2_CLASS_NAME = "tech.ydb.auth.OAuth2AuthHelper";
    private static final String OAUTH2_CLASS_ERROR = "Cannot find OAuth2AuthHelper class, "
            + "you have to add tech.ydb.auth:ydb-oauth2-provider artifact to classpath";

    @Override
    public AuthIdentity createAuthIdentity(GrpcAuthRpc rpc) {
        String anonCredentials = System.getenv("YDB_ANONYMOUS_CREDENTIALS");
        if (anonCredentials != null && anonCredentials.equals("1")) {
            return null;
        }

        String saKeyFile = System.getenv("YDB_SERVICE_ACCOUNT_KEY_FILE_CREDENTIALS");
        if (saKeyFile != null) {
            return loadServiceAccountIdentity(Paths.get(saKeyFile));
        }

        String metadataCredentials = System.getenv("YDB_METADATA_CREDENTIALS");
        if (metadataCredentials != null && metadataCredentials.equals("1")) {
            return loadMetadataIdentity();
        }

        String accessToken = System.getenv("YDB_ACCESS_TOKEN_CREDENTIALS");
        if (accessToken != null) {
            return () -> accessToken;
        }

        String oauth2KeyFile = System.getenv("YDB_OAUTH2_KEY_FILE");
        if (oauth2KeyFile != null) {
            return loadOAuth2KeyProvider(rpc, Paths.get(oauth2KeyFile));
        }

        return loadMetadataIdentity();
    }

    private static AuthIdentity loadServiceAccountIdentity(Path saKeyFile) {
        try {
            Class<?> clazz = Class.forName(IAM_CLASS_NAME);
            Method method = clazz.getMethod("serviceAccountIdentity", Path.class, String.class);
            return (AuthIdentity) method.invoke(null, saKeyFile, null);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(IAM_CLASS_ERROR, ex);
        }
    }

    private static AuthIdentity loadMetadataIdentity() {
        try {
            Class<?> clazz = Class.forName(IAM_CLASS_NAME);
            Method method = clazz.getMethod("metadataIdentity", String.class);
            return (AuthIdentity) method.invoke(null, (Object) null);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(IAM_CLASS_ERROR, ex);
        }
    }

    private static AuthIdentity loadOAuth2KeyProvider(GrpcAuthRpc rpc, Path configFile) {
        try {
            Class<?> clazz = Class.forName(OAUTH2_CLASS_NAME);
            Method method = clazz.getMethod("configFileIdentity", Path.class, GrpcAuthRpc.class);
            return (AuthIdentity) method.invoke(null, configFile, rpc);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(OAUTH2_CLASS_ERROR, ex);
        }
    }
}
