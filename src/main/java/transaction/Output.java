package transaction;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.LinkedHashMap;
import java.util.Map;

import static core.Utils.byteArrayToHex;
import static core.Utils.mergeArrays;
import static transaction.Utils.serializeUint64;
import static transaction.Utils.serializeVarInt;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Output {
    long value;
    byte[] script;

    public Output() {
        value = 0;
        script = null;
    }

    public Output(long value, byte[] script){
        this.value = value;
        this.script = script;
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
