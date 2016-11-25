package bitcoin.transaction;

import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static core.Utils.byteArrayToHex;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class TransactionTest {
    private String rawTransaction;

    @Before
    public void setUp() {
        rawTransaction =
                "010000000167ACA719146B3E5F0A5C4D37C2E487C1635C36272BE6C3E614AA2C9A0BADC2520000000"
                + "06A473044022053002185456A6C14112DDF0576D6679BED9CC17E1F0555C717C8DB1E512041CE02"
                + "206FA9CFBC517BEBEC996E4BBE9F43DB546D4606D71EE137BB22E77733E5971536012102F262D3D"
                + "68F940FE1F751194092438B541BA809A4E81D145870C9A121D1419235FFFFFFFF02000000000000"
                + "00002A6A28444F4350524F4F467F1213F5F0D7DE70C071A65BEC6A7D1D061C225323AD2B9CF495C"
                + "E4518F66E4B107A0700000000001976A9140002104E305CA7CF99DD6D2A688561F23197DB8388AC"
                + "00000000";
    }

    @Test
    public void simpleTest() {
        Transaction tx = new Transaction(rawTransaction);
    }

    @Test
    public void serializeTest() {
        Transaction tx = new Transaction(rawTransaction);
        assertEquals(rawTransaction, byteArrayToHex(tx.serialize()));
    }

    @Test
    public void txIdTest() throws NoSuchAlgorithmException {
        Transaction tx = new Transaction(rawTransaction);
        assertEquals("5F68C1E5F92FA217A7EBF466DDBC87F1C9C6F2EBD758BED3C60198FF5661C598", tx.txid());
        assertEquals("98C56156FF9801C6D3BE58D7EBF2C6C9F187BCDD66F4EBA717A22FF9E5C1685F", tx.txid(false));
    }

}