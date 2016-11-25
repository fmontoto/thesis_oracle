package bitcoin.transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;

import static bitcoin.Utils.doubleSHA256;
import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;

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
        sb.append("\tlock_time: " + lockTime);
        return sb.toString();
    }

}
