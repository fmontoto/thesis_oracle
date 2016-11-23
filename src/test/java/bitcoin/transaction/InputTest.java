package bitcoin.transaction;

import org.junit.Before;
import org.junit.Test;

import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class InputTest {
    private String rawTransaction;
    @Before
    public void setUp() {
        rawTransaction =
            "484D40D45B9EA0D652FCA8258AB7CAA42541EB52975857F96FB50CD732C8B481000000008A47304402202"
            + "CB265BF10707BF49346C3515DD3D16FC454618C58EC0A0FF448A676C54FF71302206C6624D762A1FCEF"
            + "4618284EAD8F08678AC05B13C84235F1654E6AD168233E8201410414E301B2328F17442C0B8310D787B"
            + "F3D8A404CFBD0704F135B6AD4B2D3EE751310F981926E53A6E8C39BD7D3FEFD576C543CCE493CBAC063"
            + "88F2651D1AACBFCDFFFFFFFF";
    }

    @Test
    public void simpleTest() {
        Input i = new Input(rawTransaction);
        assertEquals(179, i.getByteSize());
        assertEquals("81B4C832D70CB56FF957589752EB4125A4CAB78A25A8FC52D6A09E5BD4404D48",
                     byteArrayToHex(i.getPrevTxHash()));
        assertEquals(0, i.getPrevIdx());
    }

    @Test
    public void testSerialization() {
        Input i = new Input(rawTransaction);
        assertEquals(rawTransaction, byteArrayToHex(i.serialize()));
    }
}