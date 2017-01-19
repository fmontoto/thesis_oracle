package bitcoin.transaction;

import bitcoin.Constants;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import org.bitcoinj.core.ECKey;
import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;

import static bitcoin.Utils.doubleSHA256;
import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Transaction {
    private static final Logger LOGGER = Logger.getLogger(Transaction.class.getName());

    private long version;
    private ArrayList<Input> inputs;
    private ArrayList<Output> outputs;
    private long lockTime;
    private byte[] witnessScript;
    private Byte marker;
    private Byte flag;

    private boolean isSigned;

    public Transaction() {
        isSigned = false;
        version = 1;
        lockTime = 0xFFFFFFFFL;
        inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
        witnessScript = null;
        marker = null;
        flag = null;
    }

    public Transaction(int version, int lockTime){
        this();
        this.version = version;
        this.lockTime = lockTime;
        if(version == 2) {
            marker = 0x00;
            flag = 0x01;
        }
    }

    public Transaction(byte[] rawTransaction) throws ParseTransactionException {
        this();
        version = readUint32(rawTransaction, 0);
        try {
            if (version == 2) {
                if (rawTransaction[4] == 0x00)
                    v2TxParser(rawTransaction);
                else
                    v1TxParser(rawTransaction);
            } else if (version == 1) {
                v1TxParser(rawTransaction);
            } else {
                LOGGER.warning("unexpected Tx version:" + version);
                v1TxParser(rawTransaction);
            }
        }catch (IndexOutOfBoundsException e) {
            LOGGER.throwing("Transaction", "constructor", e);
            try {
                throw new ParseTransactionException(e.getMessage(), txid(rawTransaction));
            } catch (NoSuchAlgorithmException e1) {
                throw new ParseTransactionException(e.getMessage(), "");
            }
        }
    }

    public Transaction(String rawTransactionHex) throws ParseTransactionException {
        this(hexToByteArray(rawTransactionHex));
    }

    private void v1TxParser (byte[] rawTransaction) {
        long inputs_num, outputs_num;
        int offset = 4;
        inputs_num = readVarInt(rawTransaction, offset);
        offset += varIntByteSize(inputs_num);
        for(int i = 0; i < inputs_num; i++) {
            inputs.add(new Input(rawTransaction, offset));
            offset += inputs.get(i).getByteSize();
        }
        outputs_num = readVarInt(rawTransaction, offset);
        offset += varIntByteSize(outputs_num);
        for(int i = 0; i < outputs_num; i++) {
            outputs.add(new Output(rawTransaction, offset));
            offset += outputs.get(i).getByteSize();
        }
        lockTime = readUint32(rawTransaction, offset);
    }
    private void v2TxParser (byte[] rawTransaction) {
        long inputs_num, outputs_num;
        int offset = 4;
        marker = rawTransaction[offset++];
        flag = rawTransaction[offset++];
        inputs_num = readVarInt(rawTransaction, offset);
        offset += varIntByteSize(inputs_num);
        for(int i = 0; i < inputs_num; i++) {
            inputs.add(new Input(rawTransaction, offset));
            offset += inputs.get(i).getByteSize();
        }
        outputs_num = readVarInt(rawTransaction, offset);
        offset += varIntByteSize(outputs_num);
        for(int i = 0; i < outputs_num; i++) {
            outputs.add(new Output(rawTransaction, offset));
            offset += outputs.get(i).getByteSize();
        }
        witnessScript = Arrays.copyOfRange(rawTransaction, offset, rawTransaction.length - 4);
        offset += witnessScript.length;
        lockTime = readUint32(rawTransaction, offset);
}

    private Queue<byte []> removeOtherInputsScript(int leftInput) {
        Queue<byte[]> removedScripts = new LinkedList<>();
        for (int i = 0; i < inputs.size(); i++) {
            if(i != leftInput) {
                removedScripts.add(inputs.get(i).getScript());
                inputs.get(i).setScript(new byte[] {});
            }
        }
        return removedScripts;
    }

    private void retrieveRemovedInputs(int leftInput, Queue<byte[]> removedScripts) {
        for(int i = 0; i < inputs.size(); i++) {
            if(i != leftInput)
                inputs.get(i).setScript(removedScripts.remove());
        }
    }

    //For a P2PKH, this temporary scriptSig is the scriptPubKey of the input transaction.
    // For a P2SH, the temporary scriptSig is the redeemScript itself.
    // Thanks a lot to this log. There is no much documentation about this.
    // http://www.soroushjp.com/2014/12/20/bitcoin-multisig-the-hard-way-understanding-raw-multisignature-bitcoin-transactions/
    public void setTempScriptSigForSigning(int inputNo, byte[] script) {
        getInputs().get(inputNo).setScript(script);
    }

    private byte[] signTransaction(BitcoinPrivateKey privateKey, byte[] hashTypeCode) throws NoSuchAlgorithmException,
                                                                                             InvalidKeyException,
                                                                                             SignatureException {
        byte[] toSign = mergeArrays(serialize(), hashTypeCode);
        byte[] nonCanonicalisedSig = privateKey.sign(toSign);
        // Nodes will not accept non canonical signatures (since BIP66).
        return ECKey.ECDSASignature.decodeFromDER(nonCanonicalisedSig).toCanonicalised().encodeToDER();
    }

    private byte[] getPayToScriptSignature(BitcoinPrivateKey privateKey, byte[] hashTypeCode, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Queue<byte[]> removedScripts = removeOtherInputsScript(inputNo);

        byte[] signature = signTransaction(privateKey, hashTypeCode);

        retrieveRemovedInputs(inputNo, removedScripts);

        return mergeArrays( signature
                          , new byte[] {hashTypeCode[0]}
                          );
    }

    public byte[] getPayToScriptSignature(BitcoinPrivateKey privKey, byte hashType, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if(hashType != Constants.getHashType("ALL"))
            throw new NotImplementedException();
        return this.getPayToScriptSignature(privKey, new byte[]{hashType, 0x00, 0x00, 0x00}, inputNo);
    }

    private void sign(BitcoinPrivateKey privateKey, byte[] hashtypeCode, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        Queue<byte[]> removedScripts = removeOtherInputsScript(inputNo);

        byte[] signature = signTransaction(privateKey, hashtypeCode);
        BitcoinPublicKey publicKey = privateKey.getPublicKey();
        byte[] pubKey = publicKey.getKey();

        byte[] scriptSig = mergeArrays( Constants.pushDataOpcode(signature.length + 1)
                                      , signature
                                      , new byte[] {hashtypeCode[0]}
                                      , Constants.pushDataOpcode(pubKey.length)
                                      , pubKey
                                      );

        getInputs().get(inputNo).setScript(scriptSig);
        retrieveRemovedInputs(inputNo, removedScripts);
    }

    public void sign(BitcoinPrivateKey privKey, byte hash, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        if(hash != Constants.getHashType("ALL"))
            throw new NotImplementedException();
        this.sign(privKey, new byte[]{hash, 0x00, 0x00, 0x00}, inputNo);
    }

    public void sign(BitcoinPrivateKey privKey, int inputNo) throws InvalidKeySpecException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        this.sign(privKey, Constants.getHashType("ALL"), inputNo);
    }

    public void sign(BitcoinPrivateKey privKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        this.sign(privKey, Constants.getHashType("ALL"), 0);
    }

    public void appendInput(Input i) {
        inputs.add(i);
    }

    public void appendOutput(Output o) {
        outputs.add(o);
    }

    public ArrayList<Output> getOutputs() {
        return outputs;
    }

    public ArrayList<Input> getInputs() {
        return inputs;
    }

    private byte[] serialize(boolean complete) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(serializeUint32(version));
            if(version == 2 && complete && witnessScript != null) {
                if(marker != null)
                    byteArrayOutputStream.write(marker);
                if(flag != null)
                    byteArrayOutputStream.write(flag);
            }
            byteArrayOutputStream.write(serializeVarInt(inputs.size()));
            for(Input i: inputs)
                byteArrayOutputStream.write(i.serialize());
            byteArrayOutputStream.write(serializeVarInt(outputs.size()));
            for(Output o: outputs)
                byteArrayOutputStream.write(o.serialize());
            if(version == 2 && complete && witnessScript != null)
                byteArrayOutputStream.write(witnessScript);
            byteArrayOutputStream.write(serializeUint32(lockTime));
        } catch (IOException e) {
            // This shouldn't happen
            e.printStackTrace();
            return null;
        }
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] serialize() {
        return serialize(true);
    }

    public String hexlify() {
        return byteArrayToHex(serialize());
    }

    public String txid() throws NoSuchAlgorithmException {
        return txid(true);
    }

    static private String txid(byte[] rawTx) throws NoSuchAlgorithmException {
        return byteArrayToHex(arrayReverse(doubleSHA256(rawTx)));
    }

    public String txid(boolean rpc_order) throws NoSuchAlgorithmException {
        if(rpc_order)
            return byteArrayToHex(arrayReverse(doubleSHA256(serialize(false))));
        return byteArrayToHex(doubleSHA256(serialize(false)));
    }

    static private String toString(Map<String, String> m, int ident) {
        StringBuilder sb = new StringBuilder();
        StringBuilder auxSb = new StringBuilder();
        for(int i = 0; i < ident; i++)
            auxSb.append("\t");
        String prependSpaces = auxSb.toString();
        sb.append(prependSpaces + "{\n");
        m.forEach((k, v) -> sb.append(String.format(prependSpaces + "\t%s: %s\n", k, v)));
        sb.append(prependSpaces + "}\n");
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\tversion: " + version + "\n");
        sb.append("\tinputs counter: " + inputs.size() + "\n");
        sb.append("\tinputs: {\n");
        for(Input i: inputs)
            sb.append(toString(i.toDict(), 2));
        sb.append("\t}\n");
        sb.append("\toutputs_counter: " + outputs.size() + "\n");
        sb.append("\toutputs: {\n");
        for(Output o: outputs)
            sb.append(toString(o.toDict(), 2));
        sb.append("\t}\n");
        sb.append("\tlock_time: " + lockTime + "\n");
        sb.append("}\n");
        return sb.toString();
    }
}
