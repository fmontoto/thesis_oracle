package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.builder.OutputBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static bitcoin.Constants.*;
import static bitcoin.key.Utils.bitcoinB58Encode;
import static bitcoin.transaction.builder.InputBuilder.redeemMultisigOrOneSignatureTimeoutOutput;
import static bitcoin.transaction.builder.InputBuilder.redeemMultisigOutput;
import static bitcoin.transaction.builder.OutputBuilder.multisigScript;
import static bitcoin.transaction.builder.TransactionBuilder.*;
import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    private String getChangeAddress(Set<String> forbiddenAddresses) {
        List<String> addresses = client.getAddresses("testingNoMoney");
        for(String address: addresses)
            if(!forbiddenAddresses.contains(address))
                return address;
        // GetRawChangeAddress must be used here
        throw new NotImplementedException();
    }

    private String getChangeAddress(String... forbiddenAddresses) {
        Set<String> fA = new HashSet<>();
        for(String forbiddenAdress : forbiddenAddresses)
            fA.add(forbiddenAdress);
        return getChangeAddress(fA);
    }

    private List<String> getAvailableOutputs(String addr) throws NoSuchAlgorithmException {
        int i;
        ArrayList<Output>  outputs;
        List<Transaction> transactions = client.getTransactionsBestEffort(addr);
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
    public void simpleSendToAddressSign() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException, ParseTransactionException {
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
        for(i = 0; i < privKey.length; i++)
            privKey[i] = '\0';

        Transaction t = payToPublicKeyHash(srcOutput, changeAddr, available);
        t.sign(pKey);
        // The java client does not provide an interface to check the transaction, you need to do ir manually:
        // bitcoin-cli -testnet signrawtransaction <HEX TRANSACTION> "[]" "[]"
        System.out.println("./bitcoin-cli -testnet signrawtransaction " + byteArrayToHex(t.serialize()) + " \"[]\" \"[]\"");
    }

    @Test
    public void multipleInputsSignTest() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException, ParseTransactionException {
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
    public void multisigTimeoutFallback() throws ParseTransactionException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        AbsoluteOutput srcOutput = null;
        for (AbsoluteOutput ao : unspentOutputs)
            if (ao.isPayToKey())
                srcOutput = ao;
        assertTrue("Couldn't find an unspent output", srcOutput != null);
        String srcAddress = srcOutput.getPayAddress();
        String wifSrcAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcAddress), true);
        BitcoinPrivateKey srcPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifSrcAddress));
        String wifOptionalAddress = getChangeAddress(srcAddress);
        BitcoinPrivateKey optionalPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifOptionalAddress));
        byte[] optionalPublicKey = optionalPrivKey.getPublicKey().getKey();
        String wifNeededAddress = getChangeAddress(wifSrcAddress, wifOptionalAddress);
        BitcoinPrivateKey neededPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifNeededAddress));
        byte[] neededPublicKey = neededPrivKey.getPublicKey().getKey();
        String wifChangeAddr = wifOptionalAddress;

        byte[] redeemScript = OutputBuilder.multisigOrOneSignatureTimeoutOutput(TimeUnit.MINUTES, 20,
                                                                                neededPublicKey, optionalPublicKey);

        Transaction t0 = payToScriptHash(srcOutput, redeemScript, srcOutput.getValue());
        t0.sign(srcPrivKey);

        AbsoluteOutput scriptHashOutput = new AbsoluteOutput(t0, 0);


        Transaction t1 = payToPublicKeyHash(scriptHashOutput, wifSrcAddress, srcOutput.getValue());
//         For a P2SH, the temporary scriptSig is the redeemScript itself.
        t1.setTempScriptSigForSigning(0, redeemScript);
        byte[] t1OptionalSignature = t1.getPayToScriptSignature(optionalPrivKey, getHashType("ALL"), 0);
        byte[] t1NeededSignature = t1.getPayToScriptSignature(neededPrivKey, getHashType("ALL"), 0);
//

        t1.getInputs().get(0).setScript(redeemMultisigOrOneSignatureTimeoutOutput(redeemScript, t1NeededSignature,
                                        t1OptionalSignature));

        System.out.println("./bitcoin-cli -testnet signrawtransaction " + t1.hexlify() +
                " '[{" + "\"txid\": \"" + t0.txid() + "\"" +
                ", \"vout\": " + 0 +
                ", \"amount\": " + t1.getOutputs().get(0).getValue() +
                ", \"redeemScript\": \"" + byteArrayToHex(redeemScript) + "\"" +
                ", \"scriptPubKey\": \"" + byteArrayToHex(t0.getOutputs().get(0).getScript()) + "\"" +
                "}]' \"[]\"");

        int txVersion = 2;
        int locktime = 0;
        // TODO this 10 should make the transaction fail as the expected is 20 (at the redeem script).
        int sequenceNo = (int) readScriptNum(createSequenceNumber(TimeUnit.MINUTES, 10));

        Transaction t2 = payToPublicKeyHash(scriptHashOutput, wifSrcAddress, wifChangeAddr, srcOutput.getValue(),
                                       0, txVersion, locktime, sequenceNo);
        t2.setTempScriptSigForSigning(0, redeemScript);
        byte[] t2NeededSignature = t2.getPayToScriptSignature(neededPrivKey, getHashType("ALL"), 0);
        t2.getInputs().get(0).setScript(redeemMultisigOrOneSignatureTimeoutOutput(redeemScript, t2NeededSignature));

        System.out.println("./bitcoin-cli -testnet signrawtransaction " + t2.hexlify() +
                " '[{" + "\"txid\": \"" + t0.txid() + "\"" +
                ", \"vout\": " + 0 +
                ", \"amount\": " + t2.getOutputs().get(0).getValue() +
                ", \"redeemScript\": \"" + byteArrayToHex(redeemScript) + "\"" +
                ", \"scriptPubKey\": \"" + byteArrayToHex(t0.getOutputs().get(0).getScript()) + "\"" +
                "}]' \"[]\"");
    }

    @Test
    public void simplePayToScriptHash() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException, ParseTransactionException {
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


        byte[] addr = hexToByteArray(srcOutput.getPayAddress());
        String wifAddr = BitcoinPublicKey.txAddressToWIF(addr, true);
        BitcoinPrivateKey privKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifAddr));

        Transaction t0 = payToScriptHash(srcOutput, scriptRedeem, available);
        t0.sign(privKey);

        AbsoluteOutput scriptHashOutput = new AbsoluteOutput(t0, 0);

        Transaction t1 = payToPublicKeyHash(scriptHashOutput, changeAddr, available);
        // For a P2SH, the temporary scriptSig is the redeemScript itself.
        t1.setTempScriptSigForSigning(0, scriptRedeem);
        byte[] t1_signature = t1.getPayToScriptSignature(changePrivKey, getHashType("ALL"), 0);

        t1.getInputs().get(0).setScript(mergeArrays( new byte[]{getOpcode("OP_0")}
                                                   , pushDataOpcode(t1_signature.length)
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

    @Test
    public void simpleMultiSigPayToScriptHash() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException, ParseTransactionException {
        AbsoluteOutput srcOutput = null;
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        String srcAddr = null;
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        srcAddr = srcOutput.getPayAddress();
        long available = srcOutput.getValue();

        String wifSrcAddr = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcAddr), client.isTestnet());
        BitcoinPrivateKey srcPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifSrcAddr));

        final int numKeys = 3;
        final int requiredSignatures = 2;
        Set<String> alreadyUsedAddress = new HashSet<>();
        BitcoinPrivateKey[] privateKeys = new BitcoinPrivateKey[numKeys];
        String[] wifAddresses = new String[numKeys];
        BitcoinPublicKey[] publicKeys = new BitcoinPublicKey[numKeys];

        for(int i = 0; i < numKeys; i++) {
            wifAddresses[i] = getChangeAddress(alreadyUsedAddress);
            privateKeys[i] = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifAddresses[i]));
            publicKeys[i] = privateKeys[i].getPublicKey();
        }

        byte[] redeemScript = multisigScript(publicKeys, requiredSignatures);

        Transaction t0 = payToScriptHash(srcOutput, redeemScript, available);
        t0.sign(srcPrivKey);

        Transaction t1 = payToPublicKeyHash(new AbsoluteOutput(t0, 0), wifSrcAddr, available);

        t1.setTempScriptSigForSigning(0, redeemScript);
        List<byte[]> signatures = new ArrayList<>();
        for(int i = 0; i < requiredSignatures; i++)
            signatures.add(t1.getPayToScriptSignature(privateKeys[i], getHashType("ALL"), 0));

        t1.getInputs().get(0).setScript(redeemMultisigOutput(redeemScript, signatures));

        System.out.println("./bitcoin-cli -testnet signrawtransaction " + t1.hexlify() +
                " '[{" + "\"txid\": \"" + t0.txid() + "\"" +
                ", \"vout\": " + 0 +
                ", \"amount\": " + available +
                ", \"redeemScript\": \"" + byteArrayToHex(redeemScript) + "\"" +
                ", \"scriptPubKey\": \"" + byteArrayToHex(t0.getOutputs().get(0).getScript()) + "\"" +
                "}]' \"[]\"");
    }
}