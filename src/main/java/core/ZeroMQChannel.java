package core;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static bitcoin.transaction.Utils.serializeVarInt;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 20-01-17.
 */
public class ZeroMQChannel implements Channel {
    static private byte channelByte = 0x01;

    private List<String> parties;


    public ZeroMQChannel(String ... partyAddress){
        parties = Arrays.stream(partyAddress).collect(Collectors.toList());
    }

    @Override
    public byte getChannelByte() {
        return channelByte;
    }

    @Override
    public String getChannelRepresentation() {
        return String.join(", ", parties);
    }

    @Override
    public int serializationSize() {
        return serialize().length;
    }

    @Override
    public byte[] serialize() {
        byte[] channelRep = getChannelRepresentation().getBytes(Constants.charset);
        byte[] channelRepSize = serializeVarInt(channelRep.length);
        return mergeArrays( new byte[] {getChannelByte()}
                          , channelRepSize
                          , channelRep);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZeroMQChannel that = (ZeroMQChannel) o;

        return parties.equals(that.parties);
    }

    @Override
    public int hashCode() {
        return parties.hashCode();
    }
}
