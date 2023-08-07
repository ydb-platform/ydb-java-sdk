package tech.ydb.table.utils;

import java.util.Random;

public class ExtendedRandom  extends Random {
    public String nextString(char lowerChar, char upperChar, int length) {
        return this.ints(lowerChar, upperChar + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
