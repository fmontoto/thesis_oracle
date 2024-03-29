package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static bitcoin.Constants.*;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.*;
import static bitcoin.transaction.builder.OutputBuilder.*;
import static bitcoin.transaction.builder.TransactionBuilder.*;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;
import static org.junit.Assert.*;

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

    public String getAddressWithMoney() {
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

    public String getChangeAddress(Set<String> forbiddenAddresses) {
        return getChangeAddress(client, forbiddenAddresses);
    }

    static public String getChangeAddress(BitcoindClient client, Set<String> forbiddenAddresses, String account) {
        List<String> addresses = client.getAddresses(account);
        for(String address: addresses)
            if(forbiddenAddresses == null || !forbiddenAddresses.contains(address))
                return address;
        // GetRawChangeAddress must be used here
        throw new NotImplementedException();
    }

    static public String getChangeAddress(BitcoindClient client, Set<String> forbiddenAddresses) {
        return getChangeAddress(client, forbiddenAddresses, "testingNoMoney");
    }

    public String getChangeAddress(String... forbiddenAddresses) {
        return getChangeAddress(client, forbiddenAddresses);
    }
    static public String getChangeAddress(BitcoindClient client, String... forbiddenAddresses) {
        Set<String> fA = new HashSet<>();
        for(String forbiddenAdress : forbiddenAddresses)
            fA.add(forbiddenAdress);
        return getChangeAddress(client, fA);
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
    public void simpleSendToAddressSign() throws Exception {
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

        client.verifyTransaction(t);
    }

    @Test
    public void multipleInputsSignTest() throws Exception {
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
        client.verifyTransaction(t);
    }

    @Test
    public void treeRequiredSignaturesOneOnTimeout() throws Exception {
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        AbsoluteOutput srcOutput = null;
        for (AbsoluteOutput ao : unspentOutputs)
            if (ao.isPayToKey())
                srcOutput = ao;
        List<BitcoinPublicKey> optionalPublicKeys = new LinkedList<>();
        List<BitcoinPrivateKey> optionalPrivateKeys = new LinkedList<>();
        List<BitcoinPublicKey> neededPublicKeys = new LinkedList<>();
        List<BitcoinPrivateKey> neededPrivateKeys= new LinkedList<>();
        Set<String> alreadyUsedWIFAddresses = new HashSet<>();
        List<byte[]> requiredSignatures = new LinkedList<>();
        List<byte[]> optionalSignatures = new LinkedList<>();

        assertTrue("Couldn't find an unspent output", srcOutput != null);
        String wifSrcAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcOutput.getPayAddress()), true);
        alreadyUsedWIFAddresses.add(wifSrcAddress);
        BitcoinPrivateKey srcPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifSrcAddress));

        optionalPrivateKeys.add(BitcoinPrivateKey.fromWIF(
                client.getPrivateKey(getChangeAddress(alreadyUsedWIFAddresses))));
        optionalPublicKeys.add(optionalPrivateKeys.get(optionalPrivateKeys.size() - 1).getPublicKey());
        alreadyUsedWIFAddresses.add(optionalPublicKeys.get(optionalPublicKeys.size() - 1).toWIF());
        optionalPrivateKeys.add(BitcoinPrivateKey.fromWIF(
                client.getPrivateKey(getChangeAddress(alreadyUsedWIFAddresses))));
        optionalPublicKeys.add(optionalPrivateKeys.get(optionalPrivateKeys.size() - 1).getPublicKey());
        alreadyUsedWIFAddresses.add(optionalPublicKeys.get(optionalPublicKeys.size() - 1).toWIF());

        neededPrivateKeys.add(
                BitcoinPrivateKey.fromWIF(client.getPrivateKey(getChangeAddress(alreadyUsedWIFAddresses))));
        neededPublicKeys.add(neededPrivateKeys.get(neededPrivateKeys.size() - 1).getPublicKey());
        alreadyUsedWIFAddresses.add(neededPrivateKeys.get(neededPrivateKeys.size() - 1).getPublicKey().toWIF());

        byte[] redeemScript = multisigOrSomeSignaturesTimeoutOutput(
                TimeUnit.MINUTES, 20, neededPublicKeys, optionalPublicKeys);

        Transaction t0 = payToScriptHash(srcOutput, redeemScript, srcOutput.getValue());
        t0.sign(srcPrivKey);

        AbsoluteOutput scriptHashOutput = new AbsoluteOutput(t0, 0);
        Transaction t1 = payToPublicKeyHash(scriptHashOutput, wifSrcAddress, srcOutput.getValue());
        t1.setTempScriptSigForSigning(0, redeemScript);

        for(BitcoinPrivateKey bitcoinPrivateKey : neededPrivateKeys)
            requiredSignatures.add(t1.getPayToScriptSignature(bitcoinPrivateKey, getHashType("ALL"), 0));
        for(BitcoinPrivateKey bitcoinPrivateKey : optionalPrivateKeys)
            optionalSignatures.add(t1.getPayToScriptSignature(bitcoinPrivateKey, getHashType("ALL"), 0));

        t1.getInputs().get(0).setScript(redeemMultisigOrSomeSignaturesTimeoutOutput(
                redeemScript, requiredSignatures, optionalSignatures));

        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));
    }

    @Test
    public void multisigTimeoutFallback() throws Exception {
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        AbsoluteOutput srcOutput = null;
        for (AbsoluteOutput ao : unspentOutputs)
            if (ao.isPayToKey())
                srcOutput = ao;
        assertTrue("Couldn't find an unspent output", srcOutput != null);
        String wifSrcAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcOutput.getPayAddress()), true);
        BitcoinPrivateKey srcPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifSrcAddress));
        String wifOptionalAddress = getChangeAddress(wifSrcAddress);
        BitcoinPrivateKey optionalPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifOptionalAddress));
        byte[] optionalPublicKey = optionalPrivKey.getPublicKey().getKey();
        String wifNeededAddress = getChangeAddress(wifSrcAddress, wifOptionalAddress);
        BitcoinPrivateKey neededPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifNeededAddress));
        byte[] neededPublicKey = neededPrivKey.getPublicKey().getKey();
        String wifChangeAddr = wifOptionalAddress;

        byte[] redeemScript = multisigOrOneSignatureTimeoutOutput(TimeUnit.MINUTES, 20,
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

        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));

        int txVersion = 2;
        int locktime = 0;
        // TODO this 10 should make the transaction fail as the expected is 20 (at the redeem script).
        int sequenceNo = (int) readScriptNum(createSequenceNumber(TimeUnit.MINUTES, 10));

        Transaction t2 = payToPublicKeyHash(scriptHashOutput, wifSrcAddress, wifChangeAddr, srcOutput.getValue(),
                                       0, txVersion, locktime, sequenceNo);
        t2.setTempScriptSigForSigning(0, redeemScript);
        byte[] t2NeededSignature = t2.getPayToScriptSignature(neededPrivKey, getHashType("ALL"), 0);
        t2.getInputs().get(0).setScript(redeemMultisigOrOneSignatureTimeoutOutput(redeemScript, t2NeededSignature));

        try {
            client.verifyTransaction(t2, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));
            fail("Transaction shouldn't verify.");
        }catch (BitcoindClient.BitcoindClientException e ) {

        }
    }

    @Test
    public void simplePayToScriptHash() throws Exception {
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

        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, scriptRedeem));
    }

    @Test
    public void betPromisePayToScript() throws Exception {
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

        BitcoinPrivateKey[] keys = new BitcoinPrivateKey[2];
        keys[0] = new BitcoinPrivateKey(true, client.isTestnet());
        keys[1] = new BitcoinPrivateKey(true, client.isTestnet());

        byte[] addr = hexToByteArray(srcOutput.getPayAddress());
        String wifAddr = BitcoinPublicKey.txAddressToWIF(addr, true);
        BitcoinPrivateKey privKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifAddr));
        Input t0Input = new Input(srcOutput, new byte[]{});
        Output t0Output = oneSignatureOnTimeoutOrMultiSig(keys[0].getPublicKey().getKey(),
                keys[1].getPublicKey().getKey(), available, TimeUnit.SECONDS, 100);
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();
        inputs.add(t0Input);
        outputs.add(t0Output);

        Transaction t0 = buildTx(inputs, outputs);
        t0.sign(privKey);
        byte[] redeemScript = multisigOrOneSignatureTimeoutOutput(TimeUnit.SECONDS, 100,
                keys[0].getPublicKey().getKey(), keys[1].getPublicKey().getKey());

        AbsoluteOutput scriptHashOutput = new AbsoluteOutput(t0, 0);

        Transaction t1 = payToPublicKeyHash(scriptHashOutput, changeAddr, available);
        // For a P2SH, the temporary scriptSig is the redeemScript itself.
        t1.setTempScriptSigForSigning(0, redeemScript);
        byte[] k0_signature = t1.getPayToScriptSignature(keys[0], getHashType("ALL"), 0);
        byte[] k1_signature = t1.getPayToScriptSignature(keys[1], getHashType("ALL"), 0);

        t1.getInputs().get(0).setScript(redeemMultisigOrOneSignatureTimeoutOutput(redeemScript, k0_signature, k1_signature));
        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));
    }

    @Test
    public void betOracleInscriptionRedeem() throws Exception {
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

        BitcoinPrivateKey[] keys = new BitcoinPrivateKey[3];
        keys[0] = new BitcoinPrivateKey(true, client.isTestnet());
        keys[1] = new BitcoinPrivateKey(true, client.isTestnet());
        keys[2] = new BitcoinPrivateKey(true, client.isTestnet());
        List<BitcoinPublicKey> optionalKeys = new LinkedList<>();
        optionalKeys.add(keys[1].getPublicKey());
        optionalKeys.add(keys[2].getPublicKey());


        byte[] addr = hexToByteArray(srcOutput.getPayAddress());
        String wifAddr = BitcoinPublicKey.txAddressToWIF(addr, true);
        BitcoinPrivateKey privKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifAddr));
        Input t0Input = new Input(srcOutput, new byte[]{});

        byte[] redeemScript = multisigOrSomeSignaturesTimeoutOutput(
                TimeUnit.SECONDS, 100, keys[0].getPublicKey(), optionalKeys);

        Output t0Output = createPayToScriptHashOutputFromScript(available, redeemScript);
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();
        inputs.add(t0Input);
        outputs.add(t0Output);

        Transaction t0 = buildTx(inputs, outputs);
        t0.sign(privKey);

        AbsoluteOutput scriptHashOutput = new AbsoluteOutput(t0, 0);

        Transaction t1 = payToPublicKeyHash(scriptHashOutput, changeAddr, available);
        // For a P2SH, the temporary scriptSig is the redeemScript itself.
        t1.setTempScriptSigForSigning(0, redeemScript);
        byte[] k0_signature = t1.getPayToScriptSignature(keys[0], getHashType("ALL"), 0);
        byte[] k1_signature = t1.getPayToScriptSignature(keys[1], getHashType("ALL"), 0);
        byte[] k2_signature = t1.getPayToScriptSignature(keys[2], getHashType("ALL"), 0);
        List<byte[]> requiredSignatures = new LinkedList<>();
        List<byte[]> optionalSignatures = new LinkedList<>();
        requiredSignatures.add(k0_signature);
        optionalSignatures.add(k1_signature);
        optionalSignatures.add(k2_signature);

        t1.getInputs().get(0).setScript(redeemMultisigOrSomeSignaturesTimeoutOutput(redeemScript, requiredSignatures, optionalSignatures));
        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));
    }


    @Test
    public void simpleMultiSigPayToScriptHash() throws Exception {
        AbsoluteOutput srcOutput = null;
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        String srcAddr = srcOutput.getPayAddress();
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

        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));
    }

    @Test
    public void multiPreImageCheck() throws Exception {
        AbsoluteOutput srcOutput = null;
        List<AbsoluteOutput> unspentOutputs = client.getUnspent();
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        String srcAddr = srcOutput.getPayAddress();
        long available = srcOutput.getValue();

        String wifSrcAddr = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcAddr), client.isTestnet());
        BitcoinPrivateKey srcPrivKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(wifSrcAddr));

        final int totalHashes = 6;
        final int requiredHashes = (totalHashes / 2) + 1;
        final int timeout = 600;

        BitcoinPrivateKey player1 = new BitcoinPrivateKey(true, client.isTestnet());
        BitcoinPrivateKey player2 = new BitcoinPrivateKey(true, client.isTestnet());
        BitcoinPublicKey[] playersPubKeys = new BitcoinPublicKey[]{
                player1.getPublicKey(), player2.getPublicKey()};

        List<byte[]> aPreimages = new LinkedList<>();
        List<byte[]> bPreimages = new LinkedList<>();
        List<byte[]> aHashes = new LinkedList<>();
        List<byte[]> bHashes = new LinkedList<>();
        for(int i = 0; i < totalHashes; i++) {
            byte[] random = new byte[15];
            new Random().nextBytes(random);
            aPreimages.add(random);
            aHashes.add(r160SHA256Hash(random));
            random = new byte[15];
            new Random().nextBytes(random);
            bPreimages.add(random);
            bHashes.add(r160SHA256Hash(random));
        }


        byte[] redeemScript = betPrizeResolutionRedeemScript(aHashes, bHashes,
                Arrays.asList(playersPubKeys), requiredHashes, timeout, player1.getPublicKey());

        Transaction t0 = payToScriptHash(srcOutput, redeemScript, available);
        t0.sign(srcPrivKey);

        Transaction t1 = payToPublicKeyHash(new AbsoluteOutput(t0, 0), wifSrcAddr, available);

        t1.setTempScriptSigForSigning(0, redeemScript);
        byte[] signature = t1.getPayToScriptSignature(player1, getHashType("ALL"), 0);
        while(aPreimages.size() > requiredHashes)
            aPreimages.remove(0);
        List<byte[]> formattedPreImages = bitcoin.transaction.redeem.Utils.formatPreimages(
                aHashes, bHashes, aPreimages);
        t1.getInputs().get(0).setScript(
                redeemPlayerPrize(redeemScript, signature, player1.getPublicKey(), 0, 0, formattedPreImages));
        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));

        t1.setTempScriptSigForSigning(0, redeemScript);
        signature = t1.getPayToScriptSignature(player2, getHashType("ALL"), 0);
        while(bPreimages.size() > requiredHashes)
            bPreimages.remove(0);
        formattedPreImages = bitcoin.transaction.redeem.Utils.formatPreimages(
                aHashes, bHashes, bPreimages);
        t1.getInput(0).setScript(
                redeemPlayerPrize(redeemScript, signature, player2.getPublicKey(), 1,
                        0, formattedPreImages));
        client.verifyTransaction(t1, new PayToScriptAbsoluteOutput(t0, 0, redeemScript));

    }
}