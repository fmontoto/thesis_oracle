package transaction;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Utils {
    /**
     * Be careful, the long is a signed type, it could look like a negative number,
     * use the unsigned operations included at Integer class.
     * @param val
     * @param offset
     * @return
     */
    static public long readUint64(byte[] val, int offset) {
        return (val[offset] & 0xFFl)
                | ((val[offset + 1] & 0xFFl) << 8)
                | ((val[offset + 2] & 0xFFl) << 16)
                | ((val[offset + 3] & 0xFFl) << 24)
                | ((val[offset + 4] & 0xFFl) << 32)
                | ((val[offset + 5] & 0xFFl) << 40)
                | ((val[offset + 6] & 0xFFl) << 48)
                | ((val[offset + 7] & 0xFFl) << 56);
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
                | ((0xFFl & val[offset + 1]) << 8)
                | ((0xFFl & val[offset + 2]) << 16)
                | ((0xFFl & val[offset + 3]) << 24);
    }

    static public long readUint32(byte[] val) {
        return readUint32(val, 0);
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
                    | ((0xFFl & val[offset + 2]) << 8)
                    | ((0xFFl & val[offset + 3]) << 16)
                    | ((0xFFl & val[offset + 4]) << 24);
        }
        return (0xFF & val[offset + 1])
                | ((0xFFl & val[offset + 2]) << 8)
                | ((0xFFl & val[offset + 3]) << 16)
                | ((0xFFl & val[offset + 4]) << 24)
                | ((0xFFl & val[offset + 5]) << 32)
                | ((0xFFl & val[offset + 6]) << 40)
                | ((0xFFl & val[offset + 7]) << 48)
                | ((0xFFl & val[offset + 8]) << 56);
    }

    static public byte[] serializeVarInt(long val) {
        if(val < 0 || val > 0xFFFFFFFFl)
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
        if(val <= 0xFFFFl)
            return new byte[]{(byte) 253, (byte) val, (byte) (val >> 8)};

        return new byte[]{(byte) 254,
                          (byte) val,
                          (byte) (val >> 8),
                          (byte) (val >> 16),
                          (byte) (val >> 24)};
    }

    static public byte[] arrayReverse(byte[] val){
        byte[] ret = new byte[val.length];
        for(int i = 0; i < val.length; i++)
            ret[i] = val[val.length - 1 - i];
        return ret;
    }
}
