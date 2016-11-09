package core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;
import java.security.InvalidParameterException;

import static core.Utils.decodeB58;
import static core.Utils.encodeB58;
import static core.Utils.hexToByteArray;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 09-11-16.
 */
public class UtilsTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void hexToByteArrayLiteralTesting() throws Exception {
        assertArrayEquals(hexToByteArray("ae32"), new byte[] { (byte) 0xae, (byte)0x32});
        assertArrayEquals(hexToByteArray("AE32"), new byte[] { (byte) 0xae, (byte)0x32});
        assertArrayEquals(hexToByteArray("013E"), new byte[] { (byte) 0x01, (byte)0x3e});
    }

    @Test
    public void hexToByteArrayBorderCases() throws Exception {
        assertArrayEquals(hexToByteArray(""), new byte[0]);
        exception.expect(InvalidParameterException.class);
        hexToByteArray("2");
    }

    @Test
    public void byteArrayToHex() throws Exception {
        assertArrayEquals(new byte[] { (byte) 0xae, (byte)0x32}, hexToByteArray("ae32"));
        assertArrayEquals(new byte[] { (byte) 0xae, (byte)0x32}, hexToByteArray("AE32"));
        assertArrayEquals(new byte[] { (byte) 0x01, (byte)0x3e}, hexToByteArray("013E"));
    }

    @Test
    public void encodeB58Test() throws Exception {
        assertEquals("3", encodeB58(BigInteger.valueOf(2)));
        assertEquals("21", encodeB58(BigInteger.valueOf(58)));
        assertEquals("5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ",
                     encodeB58(new BigInteger(
                             "800C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D507A5B8D", 16)));
    }

    @Test
    public void decodeB58Test() throws Exception {
        assertEquals(BigInteger.valueOf(2), decodeB58("3"));
        assertEquals(BigInteger.valueOf(58), decodeB58("21"));
        assertEquals(new BigInteger("800C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D507A5B8D", 16),
                     decodeB58("5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ"));
    }

    @Test
    public void encodeDecodeB58Test() throws Exception {
        assertEquals("", encodeB58(decodeB58("11")));
        assertEquals("", encodeB58(decodeB58("111111111")));
        assertEquals("211", encodeB58(decodeB58("211")));
        assertEquals("24", encodeB58(decodeB58("24")));
        assertEquals("ae32", encodeB58(decodeB58("ae32")));
        assertEquals("fa", encodeB58(decodeB58("fa")));
        assertEquals("fakp243NM", encodeB58(decodeB58("fakp243NM")));
    }
}