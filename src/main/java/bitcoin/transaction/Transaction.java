package bitcoin.transaction;

import bitcoin.Constants;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import org.bitcoinj.core.ECKey;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static bitcoin.Utils.doubleSHA256;
import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Transaction {

    private long version;
    private ArrayList<Input> inputs;
    private ArrayList<Output> outputs;
    private long lockTime;

    private boolean isSigned;

    public Transaction() {
        isSigned = false;
        version = 1;
        lockTime = 0xFFFFFFFFL;
        inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
    }

    public Transaction(int version, int lockTime){
        this();
        this.version = version;
        this.lockTime = lockTime;
    }

    public Transaction(byte[] rawTransaction) {
        this();
        int offset = 0;
        long inputs_num, outputs_num;
        version = readUint32(rawTransaction, 0);
        offset += 4;
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

    // TODO Remove other inputs' scripts in a dedicated function... duplicated code
    private byte[] getPayToScriptSignature(BitcoinPrivateKey privateKey, byte[] hashType, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Queue<byte[]> removedScripts = new LinkedList<>();
        for(int i = 0; i < inputs.size() ; i++) {
            if(i != inputNo) {
                removedScripts.add(inputs.get(i).getScript());
                inputs.get(i).setScript(new byte[]{});
            }
        }

        byte[] toSign = mergeArrays(serialize(), hashType);
        byte[] signature = privateKey.sign(toSign);
        signature = ECKey.ECDSASignature.decodeFromDER(signature).toCanonicalised().encodeToDER();
        for(int i = 0; i < inputs.size(); i++) {
            if(i != inputNo)
                inputs.get(i).setScript(removedScripts.remove());
        }
        return mergeArrays( Constants.pushDataOpcode(signature.length + 1)
                          , signature
                          , new byte[] {hashType[0]}
                          );
    }

    public byte[] getPayToScriptSignature(BitcoinPrivateKey privKey, byte hashType, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if(hashType != Constants.getHashType("ALL"))
            throw new NotImplementedException();
        return this.getPayToScriptSignature(privKey, new byte[]{hashType, 0x00, 0x00, 0x00}, inputNo);
    }

    private void sign(BitcoinPrivateKey privateKey, byte[] hashtype, int inputNo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        Queue<byte[]> removedScripts = new LinkedList<>();
        for(int i = 0; i < inputs.size() ; i++) {
            if(i != inputNo) {
                removedScripts.add(inputs.get(i).getScript());
                inputs.get(i).setScript(new byte[]{});
            }
        }

        byte[] toSign = mergeArrays(serialize(), hashtype);
        byte[] signature = privateKey.sign(toSign);
        // Nodes will not accept non canonical signatures (since BIP66).
        signature = ECKey.ECDSASignature.decodeFromDER(signature).toCanonicalised().encodeToDER();
        BitcoinPublicKey publicKey = privateKey.getPublicKey();
        byte[] pubKey = publicKey.getKey();

        byte[] scriptSig = mergeArrays( Constants.pushDataOpcode(signature.length + 1)
                                      , signature
                                      , new byte[] {hashtype[0]}
                                      , Constants.pushDataOpcode(pubKey.length)
                                      , pubKey
                                      );

        getInputs().get(inputNo).setScript(scriptSig);
        for(int i = 0; i < inputs.size(); i++) {
            if(i != inputNo)
                inputs.get(i).setScript(removedScripts.remove());
        }
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

    public Transaction(String rawTransactionHex) {
        this(hexToByteArray(rawTransactionHex));
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

    public byte[] serialize() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(serializeUint32(version));
            byteArrayOutputStream.write(serializeVarInt(inputs.size()));
            for(Input i: inputs)
                byteArrayOutputStream.write(i.serialize());
            byteArrayOutputStream.write(serializeVarInt(outputs.size()));
            for(Output o: outputs)
                byteArrayOutputStream.write(o.serialize());
            byteArrayOutputStream.write(serializeUint32(lockTime));
        } catch (IOException e) {
            // This shouldn't happen
            e.printStackTrace();
            return null;
        }
        return byteArrayOutputStream.toByteArray();
    }

    public String hexlify() {
        return byteArrayToHex(serialize());
    }

    public String txid() throws NoSuchAlgorithmException {
        return txid(true);
    }

    public String txid(boolean rpc_order) throws NoSuchAlgorithmException {
        if(rpc_order)
            return byteArrayToHex(arrayReverse(doubleSHA256(serialize())));
        return byteArrayToHex(doubleSHA256(serialize()));
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
