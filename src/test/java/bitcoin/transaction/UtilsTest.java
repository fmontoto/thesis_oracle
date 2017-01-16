package bitcoin.transaction;

import org.junit.Test;

import java.security.InvalidParameterException;

import static org.junit.Assert.*;
import static bitcoin.transaction.Utils.*;


/**
 * Created by fmontoto on 17-11-16.
 */
public class UtilsTest {
    @Test
    public void testReadUint64() {
        assertEquals(2, readUint64(new byte[]{0x02, 0x00, 0x00, 0x00,
                                              0x00, 0x00, 0x00, 0x00}));
        assertEquals(2, readUint64(new byte[]{0x00, 0x02, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00}, 1));
        assertEquals(1234, readUint64(new byte[]{(byte)0xd2, 0x04, 0x00, 0x00,
                                                       0x00, 0x00, 0x00, 0x00}, 0));
        assertEquals(Long.MIN_VALUE, readUint64(new byte[]{0x00, 0x00, 0x00, 0x00,
                                                           0x00, 0x00, 0x00, (byte)0x80}));
        assertEquals(Long.MAX_VALUE,
                     readUint64(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                                           (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F}));
    }

    @Test
    public void testSerializeUint64() {
        assertArrayEquals(new byte[]{0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                          serializeUint64(2));
        assertArrayEquals(new byte[]{(byte) 0xd2, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                          serializeUint64(1234));
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x80},
                          serializeUint64(Long.MIN_VALUE));
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                                     (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F},
                           serializeUint64(Long.MAX_VALUE));
        assertArrayEquals(new byte[]{(byte) 0xAE, (byte) 0x47, (byte) 0xE1, (byte) 0x7A,
                                     (byte) 0x14, (byte) 0xAE, (byte) 0x47, (byte) 0x01},
                serializeUint64(92233720368547758L));
    }


    @Test
    public void testReadUint32() {
        assertEquals(2, readUint32(new byte[]{0x02, 0x00, 0x00, 0x00}));
        assertEquals(2, readUint32(new byte[]{0x00, 0x02, 0x00, 0x00, 0x00}, 1));
        assertEquals(1234, readUint32(new byte[]{(byte)0xd2, 0x04, 0x00, 0x00}, 0));
    }

    @Test
    public void testSerializeUint32() {
        assertArrayEquals(new byte[]{(byte)0xd2, 0x04, 0x00, 0x00}, serializeUint32(1234));
        assertArrayEquals(new byte[]{(byte)0xff, (byte) 0xc9, (byte) 0x9a, 0x3b},
                serializeUint32(999999999));
    }

    @Test
    public void testSerializeReadUint32() {
        assertEquals(2,  readUint32(serializeUint32(2)));
        assertEquals(0,  readUint32(serializeUint32(0)));
        assertEquals(1234,  readUint32(serializeUint32(1234)));
        assertEquals(4321,  readUint32(serializeUint32(4321)));
        assertEquals(999999,  readUint32(serializeUint32(999999)));
    }

    @Test
    public void testUint16() {
        assertEquals(2, readUint16(serializeUint16(2)));
        assertEquals(8, readUint16(serializeUint16(8)));
        assertEquals(9999, readUint16(serializeUint16(9999)));
    }

    @Test(expected = InvalidParameterException.class)
    public void testTooBigUint16() {
        serializeUint16(99999);
    }

    @Test
    public void testReadVarInt() throws Exception {
        assertEquals(1L, readVarInt(new byte[]{0x00, 0x01}, 1));
        assertNotEquals(1L, readVarInt(new byte[]{0x00, 0x01}, 0));
        assertEquals(256L, readVarInt(new byte[]{0x00, (byte)0xfd, 0x00, 0x01}, 1));
        assertEquals((long)Math.pow(2, 17), readVarInt(new byte[]{(byte)0xfe, 0x00, 0x00, 0x02, 0x00}, 0));
        assertEquals((long)Math.pow(2, 33),
                     readVarInt(new byte[]{(byte)0xff, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00}, 0));

    }

    @Test
    public void testSerializeVarInt() throws Exception {
        assertArrayEquals(new byte[]{0x01}, serializeVarInt(1L));
        assertArrayEquals(new byte[]{(byte)0xfd, 0x00, 0x01}, serializeVarInt(256L));
        assertArrayEquals(new byte[]{(byte)0xfe, 0x00, 0x00, 0x02, 0x00},
                     serializeVarInt((long)Math.pow(2, 17)));
        assertArrayEquals(new byte[]{(byte)0xff, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00},
                     serializeVarInt((long)Math.pow(2, 33)));

    }

    @Test
    public void testArrayReverse() throws Exception {
        assertArrayEquals(new byte[]{0x01, 0x00},
                arrayReverse(new byte[]{0x00, 0x01}));
        assertArrayEquals(new byte[]{0x01},
                arrayReverse(new byte[]{0x01}));
        assertArrayEquals(new byte[]{0x01, 0x00, 0x02},
                arrayReverse(new byte[]{0x02, 0x00, 0x01}));
        assertArrayEquals(new byte[]{0x01, 0x00, 0x02, 0x03},
                arrayReverse(new byte[]{0x03, 0x02, 0x00, 0x01}));
    }

}