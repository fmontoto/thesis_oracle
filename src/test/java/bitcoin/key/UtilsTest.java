package bitcoin.key;

import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static core.Utils.hexToByteArray;
import static bitcoin.key.Utils.bitcoinB58Decode;
import static bitcoin.key.Utils.bitcoinB58Encode;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 10-11-16.
 */
public class UtilsTest {

    @Test
    public void encodeDecodeTest() throws IOException, NoSuchAlgorithmException {
        byte[] expected = hexToByteArray("0123fc");
        byte[] expected2 = hexToByteArray("110123fc");
        assertArrayEquals(expected, bitcoinB58Decode(bitcoinB58Encode("01", "23fc")));
        assertArrayEquals(expected2, bitcoinB58Decode(bitcoinB58Encode("11", "0123fc")));

    }


}