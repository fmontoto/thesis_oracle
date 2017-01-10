package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static bitcoin.Constants.getHashType;
import static bitcoin.Constants.getOpcode;
import static bitcoin.Constants.pushDataOpcode;
import static bitcoin.key.Utils.bitcoinB58Encode;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.TransactionBuilder.payToPublicKeyHash;
import static bitcoin.transaction.TransactionBuilder.payToScriptHash;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
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
    public void simplePayToScriptHash() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        AbsoluteOutput srcOutput = null;
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
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

        BitcoinPrivateKey changePrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(changeAddr));
        byte[] changeAddrPublicKey = changePrivKey.getPublicKey().getKey();
        byte[] scriptRedeem = mergeArrays( new byte[]{getOpcode("OP_1")}
                                         , pushDataOpcode(changeAddrPublicKey.length)
                                         , changeAddrPublicKey
                                         , new byte[]{getOpcode("OP_1")}
//                                         , new byte[]{getOpcode("OP_CHECKMULTISIGVERIFY")}, new byte[] {getOpcode("OP_1")});
                                         , new byte[]{getOpcode("OP_CHECKMULTISIG")});


        byte[] scriptRedeemHash = r160SHA256Hash(scriptRedeem);

        byte[] addr = hexToByteArray(srcOutput.getPayAddress());
        String wifAddr = BitcoinPublicKey.txAddressToWIF(addr, true);
        BitcoinPrivateKey privKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifAddr));

        Transaction t0 = payToScriptHash(srcOutput, scriptRedeemHash, available);
        t0.sign(privKey);
        System.out.println("./bitcoin-cli -testnet signrawtransaction " + byteArrayToHex(t0.serialize()) + " \"[]\" \"[]\"");

        AbsoluteOutput scriptHashOutput = new AbsoluteOutput( t0.getOutputs().get(0).getValue()
                                                            , t0.getOutputs().get(0).getScript()
                                                            , 0
                                                            , t0.txid());

        Transaction t1 = payToPublicKeyHash(scriptHashOutput, changeAddr, available);
        // For a P2SH, the temporary scriptSig is the redeemScript itself.
        t1.setTempScriptSigForSigning(0, scriptRedeem);
        byte[] t1_signature = t1.getPayToScriptSignature(changePrivKey, getHashType("ALL"), 0);

        t1.getInputs().get(0).setScript(mergeArrays( new byte[]{getOpcode("OP_0")}
                                                   , t1_signature
                                                   , pushDataOpcode(scriptRedeem.length)
                                                   , scriptRedeem));

        System.out.println("./bitcoin-cli -testnet signrawtransaction " + t1.hexlify() +
                " '[{" + "\"txid\": \"" + t0.txid() + "\"" +
                   ", \"vout\": " + 0 +
                   ", \"amount\": " + available +
                   ", \"redeemScript\": \"" + byteArrayToHex(scriptRedeem) + "\"" +
                   ", \"scriptPubKey\": \"" + byteArrayToHex(t0.getOutputs().get(0).getScript()) + "\"" +
                   "}]' \"[]\"");
    }
}
