package key;

import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static core.Utils.hexToByteArray;
import static key.Utils.bitcoinB58Decode;
import static key.Utils.bitcoinB58Encode;
import static key.Utils.mergeArrays;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 10-11-16.
 */
public class UtilsTest {

    @Test
    public void encodeDecodeTest() throws IOException, NoSuchAlgorithmException {
        byte[] expected = hexToByteArray("0123fc");
        assertArrayEquals(expected, bitcoinB58Decode(bitcoinB58Encode("01", "23fc")));
    }


    @Test
    public void mergeArraysTest() {
        byte[] a = {0x00, 0x0a};
        byte[] b = {0x0b};
        byte[] c = {0x0c};
        assertArrayEquals(new byte[]{0x0b}, mergeArrays(b));
        assertArrayEquals(new byte[]{0x00, 0x0a}, mergeArrays(a));
        assertArrayEquals(new byte[]{0x00, 0x0a, 0x0c, 0x0b}, mergeArrays(a, c, b));
    }
}