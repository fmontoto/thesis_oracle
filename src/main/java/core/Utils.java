package core;

import java.math.BigInteger;
import java.security.InvalidParameterException;

/**
 * Created by fmontoto on 09-11-16.
 */
public class Utils {

    static private final String alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private static int digit(char c, int radix) throws InvalidParameterException {
        int ret = Character.digit(c, radix);
        if (ret == -1)
            throw new InvalidParameterException("Not an hex digit:" + c);
        return ret;
    }

    public static byte[] hexToByteArray(String s) throws InvalidParameterException {
        int len = s.length();
        if (len % 2 != 0)
            throw new InvalidParameterException("String must be an even number of characters");
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((digit(s.charAt(i), 16) << 4)
                    + digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static String encodeB58(BigInteger value) {
        BigInteger divisor = BigInteger.valueOf(58);
        StringBuilder sb = new StringBuilder();
        while(value.compareTo(BigInteger.ZERO) > 0) {
            // This is expensive, use a list or something else.
            sb.insert(0, alphabet.charAt(value.mod(divisor).intValue()));
            value = value.divide(divisor);
        }
        return sb.toString();
    }

    public static BigInteger decodeB58(String val) {
        BigInteger ret = BigInteger.ZERO;
        BigInteger multiplier = BigInteger.valueOf(58);
        for(int i = 0; i < val.length(); i++) {
            ret = ret.multiply(multiplier).add(BigInteger.valueOf(alphabet.indexOf(val.charAt(i))));
        }
        return ret;
    }
}
