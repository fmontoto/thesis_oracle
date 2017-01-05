package bitcoin;

import bitcoin.transaction.Transaction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

import java.util.logging.Logger;

import static core.Utils.byteArrayToHex;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class BitcoindClientTest {
    private static final Logger LOGGER = Logger.getLogger(BitcoindClient.class.getName());

    private BitcoindClient client;

    @BeforeClass
    static public void checkBitcoinD (){
        try{
            BitcoindClient.checkConnectivity(false);
            LOGGER.info("Connected to mainnet bitcoind");
        }
        catch (Exception ex){
            LOGGER.severe("Not able to connect to bitcoind in the mainnet, tests will not be run.");
            throw ex;
        }
    }

    @Before
    public void setUp() {
        client = new BitcoindClient(false);
    }

    @Test(expected=wf.bitcoin.javabitcoindrpcclient.BitcoinRpcException.class)
    public void getTransaction() throws Exception {
        Transaction tx = client.getTransaction("57cc385b9918ec124155b56321ddcbe927a100c9f4c6d18e5b04efa8e9b602c");
    }

    @Test
    public void test() throws Exception {
        assertEquals(255, (int)(((byte)255) & 0xFF));
    }
}