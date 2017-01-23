package core;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.InvalidParameterException;
import java.util.Arrays;

import static bitcoin.transaction.Utils.readVarInt;
import static bitcoin.transaction.Utils.varIntByteSize;

/**
 * Created by fmontoto on 20-01-17.
 */
public interface Channel {

    byte getChannelByte();

    String getChannelRepresentation();

    byte[] serialize();

    int serializationSize();

    static Channel fromSerialized(byte[] buff, int offset) {
        byte channelType = buff[offset++];
        if(channelType == (byte) 0x01) {
            long descriptionSize = readVarInt(buff, offset);
            offset += varIntByteSize(descriptionSize);
            String description = new String(Arrays.copyOfRange(buff, offset, Math.toIntExact(offset + descriptionSize)),
                                            Constants.charset);
            return new ZeroMQChannel(description.split(", "));
        }
        else {
            throw new InvalidParameterException("Unknown channelType");
        }
    }



    ;

}
