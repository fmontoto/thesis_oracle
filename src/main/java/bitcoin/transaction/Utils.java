package bitcoin.transaction;

import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

import static bitcoin.Constants.getHashTypeName;
import static bitcoin.Constants.getOpcodeName;
import static bitcoin.Constants.isHashType;
import static core.Utils.byteArrayToHex;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Utils {
    /**
     * Be careful, the long is a signed type, it could look like a negative number,
     * use the unsigned operations included at Integer class.
     * @param val Bytes to be interpreted as uint64.
     * @param offset Start reading val from this position.
     * @return An uint64 parsed from the provided bytes.
     */
    static public long readUint64(byte[] val, int offset) {
        return (val[offset] & 0xFFl)
                | ((val[offset + 1] & 0XFFL) << 8)
                | ((val[offset + 2] & 0xFFL) << 16)
                | ((val[offset + 3] & 0xFFL) << 24)
                | ((val[offset + 4] & 0xFFL) << 32)
                | ((val[offset + 5] & 0xFFL) << 40)
                | ((val[offset + 6] & 0xFFL) << 48)
                | ((val[offset + 7] & 0xFFL) << 56);
    }

    static public long readUint64(byte[] val) {
        return readUint64(val, 0);
    }

    static public byte[] serializeUint64(long val) {
        return new byte[]{ (byte) (0xFF & val)
                         , (byte) (0xFF & (val >> 8))
                         , (byte) (0xFF & (val >> 16))
                         , (byte) (0xFF & (val >> 24))
                         , (byte) (0xFF & (val >> 32))
                         , (byte) (0xFF & (val >> 40))
                         , (byte) (0xFF & (val >> 48))
                         , (byte) (0xFF & (val >> 56))
                         };
    }


    static public long readUint32(byte[] val, int offset) {
        return (0xFFl & val[offset])
                | ((0xFFL & val[offset + 1]) << 8)
                | ((0xFFL & val[offset + 2]) << 16)
                | ((0xFFL & val[offset + 3]) << 24);
    }

    static public long readUint32(byte[] val) {
        return readUint32(val, 0);
    }

    static public int readUint16(byte[] val, int offset) {
        return (val[offset] & 0xFF)
                | ((val[offset + 1] & 0xFF) << 8);
    }

    static public int readUint16(byte[] val) {
        return readUint16(val, 0);
    }

    static public byte[] serializeUint16(int val) {
        if(val > Math.pow(2, 16))
            throw new InvalidParameterException("Input value (" + val + ") does not fit in 16bits.");
        return new byte[] { (byte) (0xFF & val),
                            (byte) (0xFF & val >> 8)};
    }

    static public byte[] serializeUint32(long val) {
        return new byte[] { (byte) (0xFF & val)
                          , (byte) (0xFF & (val >> 8))
                          , (byte) (0xFF & (val >> 16))
                          , (byte) (0xFF & (val >> 24))};
    }

    static public long readVarInt(byte[] val) {
        return readVarInt(val, 0);
    }

    static public long readVarInt(byte[] val, int offset) {
        int first = 0xFF & val[offset];
        if(first < 253)
            return first;
        if(first == 253)
            return (0xFF & val[offset + 1]) | ((0xFF & val[offset + 2]) << 8);
        if(first == 254) {
            return (0xFF & val[offset + 1])
                    | ((0xFFL & val[offset + 2]) << 8)
                    | ((0xFFL & val[offset + 3]) << 16)
                    | ((0xFFL & val[offset + 4]) << 24);
        }
        return (0xFF & val[offset + 1])
                | ((0xFFL & val[offset + 2]) << 8)
                | ((0xFFL & val[offset + 3]) << 16)
                | ((0xFFL & val[offset + 4]) << 24)
                | ((0xFFL & val[offset + 5]) << 32)
                | ((0xFFL & val[offset + 6]) << 40)
                | ((0xFFL & val[offset + 7]) << 48)
                | ((0xFFL & val[offset + 8]) << 56);
    }

    static public byte[] serializeVarInt(long val) {
        if(val < 0 || val > 0xFFFFFFFFL)
            return new byte[]{(byte) 255,
                              (byte) val,
                              (byte) (val >> 8),
                              (byte) (val >> 16),
                              (byte) (val >> 24),
                              (byte) (val >> 32),
                              (byte) (val >> 40),
                              (byte) (val >> 48),
                              (byte) (val >> 56)};
        if(val < 253)
            return new byte[]{(byte) val};
        if(val <= 0xFFFFL)
            return new byte[]{(byte) 253, (byte) val, (byte) (val >> 8)};

        return new byte[]{(byte) 254,
                          (byte) val,
                          (byte) (val >> 8),
                          (byte) (val >> 16),
                          (byte) (val >> 24)};
    }

    static public int varIntByteSize(long val) {
        if(val < 0 || val > 0xFFFFFFFFL)
            return 9;
        if(val < 253)
            return 1;
        if(val < 0xFFFFL)
            return 3;
        return 5;

    }

    static public byte[] arrayReverse(byte[] val, int from, int to) {
        byte[] ret = new byte[to - from];
        for(int i = from; i < to; i++)
            ret[i - from] = val[to - 1 - (i - from)];
        return ret;
    }

    static public byte[] arrayReverse(byte[] val){
        return arrayReverse(val, 0, val.length);
    }

    public static List<String> parseScript(byte[] script, boolean isScriptSig) {
        List<String> ret = new LinkedList<String>();
        int idx = 0;

        while(idx < script.length) {
            byte b = script[idx];
            if(b > 0 && b < 79) { // Push data operation
                if(b < 76) {
                    ret.add("OP_PUSH_" + b + "_bytes");
                    // Looks like pay to pubkey hash
                    if(b == 71 && isScriptSig && idx == 0 && isHashType(script[71])) {
                        ret.add(byteArrayToHex(script, 1, 71) + "[" + getHashTypeName(script[71]) + "]"); // 1 = idx + 1, 71 = idx + b - 1
                        ret.add(getHashTypeName(script[71]));

                    }
                    else {
                        ret.add(byteArrayToHex(script, idx + 1, idx + 1 + b));
                    }
                    idx += (1 + b);
                }
                else {
                    int bytes_to_push = 0;
                    if(b == 76) {
                        bytes_to_push = script[idx + 1];
                        idx += 2;
                    }
                    else if(b == 77) {
                        bytes_to_push = readUint16(script, idx + 1);
                        idx += 3;
                    }
                    else if(b == 78) {
                        bytes_to_push = (int) readUint32(script, idx + 1);
                        idx += 5;
                    }
                    ret.add("OP_PUSH_" + bytes_to_push + "_bytes");
                    ret.add(byteArrayToHex(script, idx, idx + bytes_to_push));
                    idx += bytes_to_push;
                }
            }
            else{
                ret.add(getOpcodeName(b));
                idx += 1;
            }
        }
        return ret;
    }
}
