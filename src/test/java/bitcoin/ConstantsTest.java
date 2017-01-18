package bitcoin;

import org.junit.Test;

import static bitcoin.Constants.pushNumberOpcode;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class ConstantsTest {

    @Test
    public void testGetOpcode() throws Exception {
        assertEquals((byte)0xa6, Constants.getInstance().getOpcode("OP_RIPEMD160"));
        assertEquals((byte)0x61, Constants.getInstance().getOpcode("OP_NOP"));
        assertEquals((byte)0x63, Constants.getInstance().getOpcode("OP_IF"));
    }

    @Test
    public void getOpcodeName() throws Exception {
        assertEquals("OP_RIPEMD160", Constants.getInstance().getOpcodeName((byte)0xa6));
        assertEquals("OP_NOP", Constants.getInstance().getOpcodeName((byte)0x61));
        assertEquals("OP_IF", Constants.getInstance().getOpcodeName((byte)0x63));
    }

    @Test
    public void pushNumberTest() throws Exception {
        assertArrayEquals(new byte[]{(byte) 0x51}, pushNumberOpcode(1));
        assertArrayEquals(new byte[]{(byte) 0x52}, pushNumberOpcode(2));
        assertArrayEquals(new byte[]{(byte) 0x60}, pushNumberOpcode(16));
    }
}