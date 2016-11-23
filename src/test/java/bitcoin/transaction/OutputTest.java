package bitcoin.transaction;

import org.junit.Before;
import org.junit.Test;

import static core.Utils.byteArrayToHex;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class OutputTest {
    private String rawOutput;

    @Before
    public void setUp() {
        rawOutput = "62640100000000001976A914C8E90996C7C6080EE06284600C684ED904D14C5C88AC";
    }

    @Test
    public void simpleTest() {
        Output o = new Output(rawOutput);
        assertEquals("76A914C8E90996C7C6080EE06284600C684ED904D14C5C88AC",
                     byteArrayToHex(o.getScript()));
        assertEquals(91234, o.getValue());
    }

    @Test
    public void testSerialization() {
        Output o = new Output(rawOutput);
        assertEquals(rawOutput, byteArrayToHex(o.serialize()));
    }

}