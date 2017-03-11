package bitcoin.transaction;


import java.util.*;

import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Input {
    private byte[] prevTxHash;
    private long prevIdx;
    private byte[] script;
    private long sequenceNo;
    private int byte_size;

    public Input() {
        byte_size = 0;
        sequenceNo = 0xFFFFFFFF;
        prevIdx = 0;
        script = null;
        prevTxHash = null;
    }

    public Input(int prevIdx, byte[] prevTxHash, byte[] script) {
        this();
        this.prevIdx = prevIdx;
        this.prevTxHash = prevTxHash;
        this.script = script;
    }

    public Input(int sequenceNo, int prevIdx, byte[] prevTxHash, byte[] script) {
        this(prevIdx, prevTxHash, script);
        this.sequenceNo = 0xffffffffL & sequenceNo;
    }

    public Input(AbsoluteOutput ao, byte[] script) {
        this(ao.getVout(), hexToByteArray(ao.getTxId()), script);
    }

    public Input(byte[] rawInput, int offset) {
        int original_offset = offset;
        prevTxHash = arrayReverse(rawInput, offset, offset + 32);
        offset += 32;
        prevIdx = readUint32(rawInput, offset);
        offset += 4;
        long script_length = readVarInt(rawInput, offset);
        offset += varIntByteSize(script_length);
        script = Arrays.copyOfRange(rawInput, offset, offset + (int)script_length);
        offset += script_length;
        sequenceNo = readUint32(rawInput, offset);
        offset += 4;
        byte_size = offset - original_offset;
    }

    public Input(byte[] rawInput) {
        this(rawInput, 0);
    }

    public Input(String rawInputHex) {
        this(hexToByteArray(rawInputHex));
    }

    public byte[] getPrevTxHash() {
        return Arrays.copyOf(prevTxHash, prevTxHash.length);
    }

    public long getPrevIdx() {
        return prevIdx;
    }

    public int getByteSize() {
        if(byte_size == 0)
            byte_size = serialize().length;
        return byte_size;
    }

    public void setScript(byte[] newScript) {
        script = newScript;
    }

    public byte[] getScript() {
        return script;
    }

    public long getSequenceNo() {
        return sequenceNo;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Input input = (Input) o;

        if (prevIdx != input.prevIdx) return false;
        if (sequenceNo != input.sequenceNo) return false;
        if (!Arrays.equals(prevTxHash, input.prevTxHash)) return false;
        return Arrays.equals(script, input.script);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(prevTxHash);
        result = 31 * result + (int) (prevIdx ^ (prevIdx >>> 32));
        result = 31 * result + Arrays.hashCode(script);
        result = 31 * result + (int) (sequenceNo ^ (sequenceNo >>> 32));
        return result;
    }

    public Map<String, String> toDict() {
        // Linked hashmaps keep the insertion order.
        Map<String, String> ret = new LinkedHashMap<String, String>();
        ret.put("prev_tx_hash", byteArrayToHex(prevTxHash != null ? prevTxHash: new byte[32]));
        ret.put("prev_idx", Long.toUnsignedString(prevIdx));
        ret.put("script_length", String.valueOf(script != null ? script.length : 0));
        ret.put("script", byteArrayToHex(script));
        ret.put("sequence_no", Long.toUnsignedString(sequenceNo));
        return ret;
    }
}
