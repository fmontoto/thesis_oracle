package bitcoin.transaction;

import org.omg.CORBA.DynAnyPackage.Invalid;

import java.security.InvalidParameterException;
import java.util.*;

import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Output {
    private long value;
    private byte[] script;

    private int byte_size;
    private boolean isPayToKey;
    private boolean isPayToScript;

    List<String> parsedScript;

    public Output() {
        byte_size = 0;
        isPayToKey = false;

        value = 0;
        script = null;
    }

    public Output(long value, byte[] script){
        this.value = value;
        this.script = script;
        parseScript();
    }

    public Output(byte[] rawOutput, int offset) {
        int original_offset = offset;
        value = readUint64(rawOutput, offset);
        offset += 8;
        long script_length = readVarInt(rawOutput, offset);
        offset += varIntByteSize(script_length);
        script = Arrays.copyOfRange(rawOutput, offset, offset + (int)script_length);
        offset += script_length;
        byte_size = offset - original_offset;
        try {
            parseScript();
        } catch (NullPointerException e) {
            parsedScript = new LinkedList<>(Arrays.asList(new String[] {"ERROR. Non standard"}));
            isPayToKey = false;
            isPayToScript = false;
        }
    }

    private void parseScript() {
        parsedScript = Utils.parseScript(script, false);
        if(parsedScript.size() == 6 && parsedScript.get(0).equals("OP_DUP")
                                    && parsedScript.get(1).equals("OP_HASH160")
                                    && parsedScript.get(2).equals("OP_PUSH_20_bytes")
                                    && parsedScript.get(4).equals("OP_EQUALVERIFY")
                                    && parsedScript.get(5).equals("OP_CHECKSIG"))
            isPayToKey = true;
        else
            isPayToKey = false;
        if(parsedScript.size() == 3 && parsedScript.get(0).equals("OP_HASH160")
                                    && parsedScript.get(2).equals("OP_EQUAL"))
            isPayToScript = true;
        else
            isPayToScript = false;
    }

    public Output(byte[] rawOutput) {
        this(rawOutput, 0);
    }

    public Output(String rawOutputHex) {
        this(hexToByteArray(rawOutputHex));
    }

    public int getByteSize() {
        if(byte_size == 0)
            byte_size = serialize().length;
        return byte_size;
    }

    public long getValue() {
        return value;
    }

    public byte[] getScript() {
        return script;
    }

    public byte[] serialize() {
        return mergeArrays(serializeUint64(value),
                           serializeVarInt(script != null ? script.length : 0),
                           script != null ? script : new byte[0]);

    }

    public String hexlify() {
        return byteArrayToHex(serialize());
    }

    public Map<String, String> toDict() {
        // Linked hashmaps keep the insertion order.
        Map<String, String> ret = new LinkedHashMap<String, String>();
        ret.put("value", String.valueOf(Long.toUnsignedString(value)));
        ret.put("script_length", String.valueOf(script != null ? script.length : 0));
        ret.put("script", byteArrayToHex(script != null ? script : new byte[0]));
        return ret;
    }

    public boolean isPayToKey() {
        return isPayToKey;
    }

    public boolean isPayToScript() {
        return isPayToScript;
    }

    /**
     *
     * @return Return hex representation of the pay address as it appears in the transaction.
     */
    public String getPayAddress() {
        if(!isPayToKey())
            throw new InvalidParameterException("It must be a pay to key output to in order to get an address");
        return parsedScript.get(3);
    }

    public List<String> getParsedScript() {
        return parsedScript;
    }

}
