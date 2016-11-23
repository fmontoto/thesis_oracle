package bitcoin.transaction;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public Output() {
        byte_size = 0;
        value = 0;
        script = null;
    }

    public Output(long value, byte[] script){
        this.value = value;
        this.script = script;
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
}
