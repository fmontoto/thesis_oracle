package bitcoin.transaction;

import java.security.InvalidParameterException;
import java.util.*;

import static bitcoin.transaction.Utils.*;
import static core.Utils.*;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Output {
    private long value;
    private byte[] script;

    private int byte_size;
    private boolean isPayToKey;
    private boolean isPayToScript;

    private List<String> parsedScript;

    public Output() {
        byte_size = 0;
        isPayToKey = false;

        value = 0;
        script = null;
    }

    public Output(long value, byte[] script){
        this.value = value;
        this.script = script;
        try {
            parseScript();
        } catch (IndexOutOfBoundsException e) {
            parsedScript = new LinkedList<>(Arrays.asList("ERROR. Non standard"));
            isPayToKey = false;
            isPayToScript = false;
        }
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
        } catch (IndexOutOfBoundsException e) {
            parsedScript = new LinkedList<>(Arrays.asList("ERROR. Non standard"));
            isPayToKey = false;
            isPayToScript = false;
        }
    }

    private void parseScript() {
        parsedScript = Utils.parseScript(script, false);
        isPayToKey = parsedScript.size() == 6 && parsedScript.get(0).equals("OP_DUP")
                && parsedScript.get(1).equals("OP_HASH160")
                && parsedScript.get(2).equals("OP_PUSH_20_bytes")
                && parsedScript.get(4).equals("OP_EQUALVERIFY")
                && parsedScript.get(5).equals("OP_CHECKSIG");
        isPayToScript = parsedScript.size() == 3 && parsedScript.get(0).equals("OP_HASH160")
                && parsedScript.get(2).equals("OP_EQUAL");
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
        //ret.put("script", byteArrayToHex(script != null ? script : new byte[0]));
        ret.put("script", parsedScript.toString());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Output output = (Output) o;

        if (value != output.value) return false;
        if (isPayToKey != output.isPayToKey) return false;
        if (isPayToScript != output.isPayToScript) return false;
        if (!Arrays.equals(script, output.script)) return false;
        return parsedScript != null ? parsedScript.equals(output.parsedScript) : output.parsedScript == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (value ^ (value >>> 32));
        result = 31 * result + Arrays.hashCode(script);
        result = 31 * result + (isPayToKey ? 1 : 0);
        result = 31 * result + (isPayToScript ? 1 : 0);
        result = 31 * result + (parsedScript != null ? parsedScript.hashCode() : 0);
        return result;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
