package bitcoin;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static bitcoin.Utils.getOracleList;
import static bitcoin.Utils.compileOracleList;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 11-01-17.
 */
public class UtilsTest {

    int currentHeight;
    BitcoindClient client;
    boolean testnet;

    @Before
    public void setUp() {
        testnet = false;
        client = new BitcoindClient(testnet);
        currentHeight = client.getBlockCount();

    }

    @Test
    public void getOracleListTest() {
        List<String> oracles = getOracleList(testnet, "34", "99");
        assertTrue(oracles.isEmpty());
    }

    @Test(expected= Exception.class)
    public void getOracleListWrongHeight() {
        getOracleList(testnet, "" + currentHeight, "" + (currentHeight + 4));
    }

    @Test
    public void removeThisTest() {
        getOracleList(true, "1063302", "1064302");
    }
}
