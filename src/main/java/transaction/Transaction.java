package transaction;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static core.Utils.byteArrayToHex;
import static core.Utils.mergeArrays;
import static transaction.Utils.serializeUint32;
import static transaction.Utils.serializeVarInt;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Transaction {

    private int version;
    private ArrayList<Input> inputs;
    private ArrayList<Output> outputs;
    private int lockTime;

    public Transaction() {
        version = 1;
        lockTime = 0xFFFFFFFF;
        inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
    }

    public Transaction(int version, int lockTime){
        this();
        this.version = version;
        this.lockTime = lockTime;
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

    private String toString(Map<String, String> m, int ident) {
        throw new NotImplementedException();


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
