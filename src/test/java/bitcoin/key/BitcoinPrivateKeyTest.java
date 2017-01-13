package bitcoin.key;

import org.junit.Test;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.List;

import static core.Utils.byteArrayToHex;
import static bitcoin.key.Utils.get32ByteRepresentation;
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
                     byteArrayToHex(get32ByteRepresentation(bitcoinPrivateKey.getS())));

        bitcoinPrivateKey =
                BitcoinPrivateKey.fromWIF("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF".toCharArray());
        assertEquals("E9873D79C6D87DC0FB6A5778633389F4453213303DA61F20BD67FC233AA33262",
                byteArrayToHex(get32ByteRepresentation(bitcoinPrivateKey.getS())));

        BitcoinPrivateKey bitcoinPrivateKey2 =
                BitcoinPrivateKey.fromWIF("5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ");
        assertEquals("0C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D",
                byteArrayToHex(bitcoinPrivateKey2.getS().toByteArray()));

        BitcoinPrivateKey bitcoinPrivateKey3 =
                BitcoinPrivateKey.fromWIF("cMkY7CxQFR1GKnVnauBePfUsumiDiQA3gJV2Lbu7njVbbRsHbM7j");
        assertEquals("n4Ke2X6TSdLP9Q4VpVzW77D43Tc18sfpk1",
                     bitcoinPrivateKey3.getPublicKey().toWIF());

        BitcoinPrivateKey bitcoinPrivateKey4 =
                BitcoinPrivateKey.fromWIF("cW4D2bQbjL76BYfU8V3jYVxq3qXccuNzbNwQCRxKgZHe5qCWZQm1");
        assertEquals("mppjAUikJwPGFk2MR9y4cgjCdSGyFMc8ev",
                     bitcoinPrivateKey4.getPublicKey().toWIF());
    }

    @Test
    public void getPublicKeyTest() throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        BitcoinPrivateKey bitcoinPrivateKey = new BitcoinPrivateKey(
                "18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725", false, false);
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM",
                     bitcoinPrivateKey.getPublicKey().toWIF());
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


        bitcoinPrivateKey =
                BitcoinPrivateKey.fromWIF("cW4D2bQbjL76BYfU8V3jYVxq3qXccuNzbNwQCRxKgZHe5qCWZQm1");
        Signature dsa2 = Signature.getInstance("SHA256withECDSA");
        dsa2.initSign(bitcoinPrivateKey);
        dsa2.update(data);
        byte[] signature2 = dsa2.sign();

        Signature verifier2 = Signature.getInstance("SHA256withECDSA");
        verifier2.initVerify(bitcoinPrivateKey.getPublicKey());
        verifier2.update(data);
        assertTrue(verifier2.verify(signature2));
    }

    @Test
    public void signTest() throws SignatureException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        BitcoinPrivateKey pKey = new BitcoinPrivateKey(true, true);

        MessageDigest dig = MessageDigest.getInstance("SHA-256");
        byte[] data = new byte[]{0x02, (byte)0xec, 0x0d, 0x0e, (byte)0xdf};
        byte[] hashed_data = dig.digest(data);
        byte[] signature = pKey.sign(data);

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(pKey.getPublicKey());
        verifier.update(hashed_data);
        assertTrue(verifier.verify(signature));
    }

//                BitcoinPrivateKey.fromWIF("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
//    assertEquals("E9873D79C6D87DC0FB6A5778633389F4453213303DA61F20BD67FC233AA33262",
    @Test
    public void toWIF() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {
        List<String> WIFs = new LinkedList<String>();
        WIFs.add("5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ");
        WIFs.add("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
        WIFs.add("cMkY7CxQFR1GKnVnauBePfUsumiDiQA3gJV2Lbu7njVbbRsHbM7j");
        for(String wif : WIFs){
            assertEquals(wif, BitcoinPrivateKey.fromWIF(wif).toWIF());
        }
    }
}