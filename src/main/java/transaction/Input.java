package transaction;


import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static core.Utils.byteArrayToHex;
import static core.Utils.mergeArrays;
import static java.util.Collections.reverse;
import static transaction.Utils.arrayReverse;
import static transaction.Utils.serializeUint32;
import static transaction.Utils.serializeVarInt;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Input {
    byte[] prevTxHash;
    int prevIdx;
    byte[] script;
    int sequenceNo;

    public Input() {
        sequenceNo = 0xFFFFFFFF;
        prevIdx = 0;
        script = null;
        prevTxHash = null;
    }

    public byte[] serialize() {
        return mergeArrays(arrayReverse(prevTxHash != null ? prevTxHash: new byte[32]),
                           serializeUint32(prevIdx),
                           serializeVarInt(script != null ? script.length : 0),
                           script != null ? script : new byte[0],
                           serializeUint32(sequenceNo));
    }

    public String hexify() {
        return byteArrayToHex(serialize());
    }

    public Map<String, String> toDict() {
        // Linked hashmaps keep the insertion order.
        Map<String, String> ret = new LinkedHashMap<String, String>();
        ret.put("prev_tx_hash", byteArrayToHex(prevTxHash != null ? prevTxHash: new byte[32]));
        ret.put("prev_idx", String.valueOf(prevIdx));
        ret.put("script_length", String.valueOf(script != null ? script.length : 0));
        ret.put("script", byteArrayToHex(script));
        ret.put("sequence_no", String.valueOf(sequenceNo));
        return ret;
    }
}
