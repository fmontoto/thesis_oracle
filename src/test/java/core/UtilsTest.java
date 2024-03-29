package core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;
import java.security.InvalidParameterException;

import static core.Utils.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        assertEquals(BigInteger.valueOf(2), decodeB58(new char[]{'3'}));
        assertEquals(BigInteger.valueOf(2), decodeB58("3"));
        assertEquals(BigInteger.valueOf(58), decodeB58("21"));
        assertEquals(BigInteger.valueOf(58), decodeB58(new char[] {'2', '1'}));
        assertEquals(new BigInteger("800C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D507A5B8D", 16),
                decodeB58("5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ"));
        assertEquals(new BigInteger("800C28FCA386C7A227600B2FE50B7CAE11EC86D3BF1FBE471BE89827E19D72AA1D507A5B8D", 16),
                     decodeB58(new char[]{'5', 'H', 'u', 'e', 'C', 'G', 'U', '8', 'r', 'M', 'j', 'x', 'E', 'X', 'x',
                                          'i', 'P', 'u', 'D', '5', 'B', 'D', 'k', 'u', '4', 'M', 'k', 'F', 'q', 'e',
                                          'Z', 'y', 'd', '4', 'd', 'Z', '1', 'j', 'v', 'h', 'T', 'V', 'q', 'v', 'b',
                                          'T', 'L', 'v', 'y', 'T', 'J'}));
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