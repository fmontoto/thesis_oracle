package key;

import org.junit.Test;

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

}