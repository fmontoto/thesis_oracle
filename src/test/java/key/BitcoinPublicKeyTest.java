package key;

import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static core.Utils.hexToByteArray;
import static key.Utils.bytesToBigInteger;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 10-11-16.
 */
public class BitcoinPublicKeyTest {

    @Test
    public void testNotCompressedNotTestnetConstructors() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        String hexKey = "0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6";
        // From hex
        BitcoinPublicKey bitcoinPublicKey = new BitcoinPublicKey(hexKey, false, false);
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM", bitcoinPublicKey.getAddress());
        // From bytes
        bitcoinPublicKey = new BitcoinPublicKey(hexToByteArray(hexKey), false, false);
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM", bitcoinPublicKey.getAddress());
        // From two BigIntegers
        bitcoinPublicKey = new BitcoinPublicKey(
                bytesToBigInteger(hexToByteArray(hexKey.substring(2, 66))),
                bytesToBigInteger(hexToByteArray(hexKey.substring(66, 130))),
                false, false);
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM", bitcoinPublicKey.getAddress());

    }
    @Test
    public void toWIFSimpleTest() throws Exception {
        BitcoinPublicKey bitcoinPublicKey = new BitcoinPublicKey(
              "030881eb43770203716888f131eaba4d9b35446d60cebafebcb9908ffdb050b006", true, true);
        assertEquals("mrbFF4kC3Mci2KqHLzPTNhj7QXoi2vyxfk", bitcoinPublicKey.getAddress());
    }

}