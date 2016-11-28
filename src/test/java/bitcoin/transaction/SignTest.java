package bitcoin.transaction;

import bitcoin.BitcoindClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
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

    private String getAddressWithMoney() {
        List<String> testingAddresses = client.getAddresses("testingMoney");
        String addr = null;
        for(String s: testingAddresses) {
            System.out.println(client.getAccountBalance(s));
            if (client.getAccountBalance(s) > 0) {
                addr = s;
                break;
            }
        }
        assertNotNull("One testingMoney address must have a positive balance to perform a tx.",
                addr);
        return addr;
    }

    private List<String> getAvailableOutputs(String addr) throws NoSuchAlgorithmException {
        int i;
        ArrayList<Output>  outputs;
        List<Transaction> transactions = client.getTransactions(addr);
        List<String> ret = new LinkedList<String>();
        for(Transaction transaction: transactions) {
            outputs = transaction.getOutputs();
            for(Output o: outputs) {
                if(o.isPayToKey()) {
                    if(o.getPayAddress().equals(addr)) {
                        ret.add(new String(transaction.txid() + ":" + outputs.indexOf(o)));
                    }
                }
            }
        }
        return ret;

    }

    @Test
    public void simpleSendToAddressSign() throws NoSuchAlgorithmException {
        int i;
        String srcAddr = getAddressWithMoney();
        List<String> availableOutputs = getAvailableOutputs(srcAddr);
        String outputTxId = availableOutputs.get(0).split(":")[0];
        int outputIdx = Integer.parseInt(availableOutputs.get(0).split(":")[1]);
//        client.getPrivateKey();


    }

}
