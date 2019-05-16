package tech.ydb.table.utils;

import com.google.protobuf.ByteString;


/**
 * @author Sergey Polovko
 */
public class Hex {
    private Hex() {}

    private static final char[] HEX_DIGITS = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static void toHex(ByteString bytes, StringBuilder sb) {
        sb.ensureCapacity(bytes.size());
        for (int i = 0; i < bytes.size(); i++) {
            byte b = bytes.byteAt(i);
            sb.append(HEX_DIGITS[(b & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b & 0x0f]);
        }
    }
}
