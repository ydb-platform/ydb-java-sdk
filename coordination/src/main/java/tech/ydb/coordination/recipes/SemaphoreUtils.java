package tech.ydb.coordination.recipes;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SemaphoreUtils {
    private SemaphoreUtils() { }

    public static void createIfNotExists(CoordinationClient client, String path, String name, long limit, byte[] data) {
        final String fullPath = client.getDatabase() + path;
        try (CoordinationSession session = client.createSession(fullPath)) {
            session.connect().join().expectSuccess();
            Status createStatus = session.createSemaphore(name, limit, data).join();
        }
    }
}
