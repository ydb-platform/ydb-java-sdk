package tech.ydb.topic.impl;

import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DebugTools {
    private static final int ID_LENGTH = 6;
    private static final char[] ID_ALPHABET = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            .toCharArray();

    private DebugTools() { }

    public static String createDebugId(String id) {
        if (id != null) {
            return id;
        }

        return ThreadLocalRandom.current().ints(0, ID_ALPHABET.length)
                .limit(ID_LENGTH)
                .map(charId -> ID_ALPHABET[charId])
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
