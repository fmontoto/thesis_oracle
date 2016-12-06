package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static bitcoin.key.Utils.bitcoinB58Encode;
import static bitcoin.transaction.TransactionBuilder.payToPublicKeyHash;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
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
    public void simpleSendToAddressSign() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        int i;
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        AbsoluteOutput srcOutput = null;
        String changeAddr = null;
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        for(String addr: client.listReceivedByAddr())
            changeAddr = addr;
        if(changeAddr == null)
            changeAddr = srcOutput.getPayAddress();
        long available = srcOutput.getValue();

        byte[] addr = hexToByteArray(srcOutput.getPayAddress());
        String wifAddr = BitcoinPublicKey.txAddressToWIF(addr, true);

        char[] privKey = client.getPrivateKey(wifAddr);
        BitcoinPrivateKey pKey = BitcoinPrivateKey.fromWIF(privKey);
        for(char b: privKey)
            b = '\0';

        Transaction t = payToPublicKeyHash(srcOutput, changeAddr, available);
        t.sign(pKey);
        // The java client does not provide an interface to check the transaction, you need to do ir manually:
        // bitcoin-cli -testnet signrawtransaction <HEX TRANSACTION> "[]" "[]"
        System.out.println("./bitcoin-cli -testnet signrawtransaction " + byteArrayToHex(t.serialize()) + " \"[]\" \"[]\"");
    }

    @Test
    public void multipleInputsSignTest() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        List<AbsoluteOutput> outputs = new ArrayList<>();
        List<BitcoinPrivateKey> privKeys = new ArrayList<>();
        String changeAddr = null;

        for(AbsoluteOutput ao: unspentOutputs) {
            if(ao.isPayToKey())
                outputs.add(ao);
        }

        assertTrue("Couldn't find two unspent outputs.", 2 <= outputs.size());

        for(String addr: client.listReceivedByAddr())
            changeAddr = addr;
        if(changeAddr == null)
            changeAddr = outputs.get(0).getPayAddress();

        long available = 0;

        for(AbsoluteOutput ao: outputs) {
            available += ao.getValue();
            String wifAddr = BitcoinPublicKey.txAddressToWIF(hexToByteArray(ao.getPayAddress()), true);
            privKeys.add(BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifAddr)));
        }

        Transaction t = payToPublicKeyHash(outputs, changeAddr, available);
        for(int i = 0; i < privKeys.size(); i++) {
            t.sign(privKeys.get(i), i);
        }

        System.out.println("./bitcoin-cli -testnet signrawtransaction " + byteArrayToHex(t.serialize()) + " \"[]\" \"[]\"");
    }

    @Test
    public void simplePayToScriptHash() throws IOException, NoSuchAlgorithmException {
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        AbsoluteOutput srcOutput = null;
        String changeAddr = null;
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        for(String addr: client.listReceivedByAddr())
            changeAddr = addr;
        if(changeAddr == null)
            changeAddr = srcOutput.getPayAddress();
        long availble = srcOutput.getValue();

        byte[] addr = hexToByteArray(srcOutput.getPayAddress());
        String wifAddr = BitcoinPublicKey.txAddressToWIF(addr, true);




    }

}
