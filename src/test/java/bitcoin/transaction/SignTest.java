package bitcoin.transaction;

import bitcoin.BitcoindClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 23-11-16.
 */
public class SignTest {

    private static final Logger LOGGER = Logger.getLogger(SignTest.class.getName());

    private BitcoindClient client;

    @BeforeClass
    static public void checkBitcoinD (){
        try{
            BitcoindClient.checkConnectivity(true);
            LOGGER.info("Connected to testnet bitcoind");
        }
        catch (Exception ex){
            LOGGER.severe("Not able to connect to bitcoind in the testned, tests will not be run.");
            throw ex;
        }
    }

    @Before
    public void setUp() {
        client = new BitcoindClient(true);
    }

    private String getAccountWithMoney() {
        List<String> testingAddresses = client.getAddresses("testingMoney");
        String addr = null;
        for(String s: testingAddresses) {
            System.out.println(client.getAddressBalance(s));
            if (client.getAddressBalance(s) > 0) {
                addr = s;
                break;
            }
        }
        assertNotNull("One testingMoney address must have a positive balance to perform a tx.",
                addr);
        return addr;
    }

    private String[] getAvailableOutput(String addr) {
        int i;
        ArrayList<Output>  outputs;
        List<Transaction> transactions = client.getTransactions(addr);
        for(Transaction transaction: transactions) {
            outputs = transaction.getOutputs();
            for(Output o: outputs) {
                if(o.isPayToKey()) {
                    if(o.getPayAddress().equals(addr)) {
//                        String ret = new String[2]{transaction.txid(), outputs.indexOf(o)}
                    }
                }
            }
        }
        throw new NotImplementedException();

    }

    @Test
    public void simpleSendToAddressSign() {
        int i;
        String srcAddr = getAccountWithMoney();
        String [] output = getAvailableOutput(srcAddr);

    }

}
