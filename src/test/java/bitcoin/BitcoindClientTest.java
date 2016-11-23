package bitcoin;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class BitcoindClientTest {
    private static final Logger LOGGER = Logger.getLogger(BitcoindClient.class.getName());

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

    @Test
    public void getTransaction() throws Exception {
//        assertEquals

    }

}