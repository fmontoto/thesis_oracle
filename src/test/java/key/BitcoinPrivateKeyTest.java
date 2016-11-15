package key;

import org.junit.Test;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import static core.Utils.byteArrayToHex;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 10-11-16.
 */
public class BitcoinPrivateKeyTest {
    @Test
    public void fromWIFTest() throws Exception {
        BitcoinPrivateKey bitcoinPrivateKey =
                BitcoinPrivateKey.fromWIF("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
        assertEquals("E9873D79C6D87DC0FB6A5778633389F4453213303DA61F20BD67FC233AA33262",
                     byteArrayToHex(bitcoinPrivateKey.getS().toByteArray()));

        BitcoinPrivateKey bitcoinPrivateKey2 =
                BitcoinPrivateKey.fromWIF("5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ");
        assertEquals("0C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D",
                byteArrayToHex(bitcoinPrivateKey2.getS().toByteArray()));
    }

    @Test
    public void getPublicKeyTest() throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        BitcoinPrivateKey bitcoinPrivateKey = new BitcoinPrivateKey(
                "18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725", false, false);
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM",
                     bitcoinPrivateKey.getPublicKey().getAddress());
    }

    @Test
    public void signingTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        BitcoinPrivateKey bitcoinPrivateKey = new BitcoinPrivateKey(true, true);
        byte[] data = new byte[]{0x02, (byte)0xec, 0x0d, 0x0e, (byte)0xdf};
        Signature dsa = Signature.getInstance("SHA256withECDSA");
        dsa.initSign(bitcoinPrivateKey);
        dsa.update(data);
        byte[] signature = dsa.sign();

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(bitcoinPrivateKey.getPublicKey());
        verifier.update(new byte[]{0x03, 0x04});
        assertFalse(verifier.verify(signature));

        verifier.initVerify(bitcoinPrivateKey.getPublicKey());
        verifier.update(data);
        assertTrue(verifier.verify(signature));
    }
}