package core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static bitcoin.transaction.Utils.*;
import static core.Constants.BET_DESCRIPTION;
import static java.util.stream.Collectors.toSet;

/**
 * Created by fmontoto on 20-01-17.
 */
public class BetTxForm {
    private List<Oracle> oracles;
    private byte[] descriptionHash;
    private Channel channel;

    public BetTxForm(List<Oracle> oracles, byte[] descriptionHash, Channel channel) {
        this.oracles = oracles;
        this.descriptionHash = descriptionHash;
        this.channel = channel;
        if(descriptionHash.length != 20)
            throw new InvalidParameterException("Description hash must be 20 bytes long, not" + descriptionHash.length);
    }

    public byte[] serialize() throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(serializeVarInt(BET_DESCRIPTION.length));
        byteStream.write(BET_DESCRIPTION);
        byteStream.write(serializeVarInt(oracles.size()));
        for(Oracle o : oracles)
            byteStream.write(o.serialize());

        byteStream.write(descriptionHash);
        byteStream.write(channel.serialize());
        return byteStream.toByteArray();
    }

    static public BetTxForm fromSerialized(byte[] bet, int offset) throws IOException, NoSuchAlgorithmException {
        try {
            int identifierLength = Math.toIntExact(readVarInt(bet, offset));
            offset += varIntByteSize(identifierLength);

            if(!Arrays.equals(BET_DESCRIPTION, Arrays.copyOfRange(bet, offset, offset + identifierLength))) {
                throw new InvalidParameterException("Not a valid serialized Bet");
            }

            offset += identifierLength;

            long oraclesNum = readVarInt(bet, offset);
            offset += varIntByteSize(oraclesNum);
            List<Oracle> oracles = new ArrayList<>();
            for (int i = 0; i < oraclesNum; i++) {
                Oracle oracle = Oracle.loadFromSerialized(bet, offset);
                offset += oracle.serializationSize();
                oracles.add(oracle);
            }
            // 20 bytes is the size of the Hash
            byte[] descriptionHash = Arrays.copyOfRange(bet, offset, offset + 20);
            offset += 20;

            Channel channel = Channel.fromSerialized(bet, offset);
            offset += channel.serializationSize();
            return new BetTxForm(oracles, descriptionHash, channel);
        }
        catch (IndexOutOfBoundsException e){
            throw new InvalidParameterException("Provided bytes are not a valid bet");
        }
    }

    static public BetTxForm fromSerialized(byte[] bet) throws IOException, NoSuchAlgorithmException {
        return fromSerialized(bet, 0);
    }

    public List<Oracle> getOracles() {
        return oracles;
    }

    public byte[] getDescriptionHash() {
        return descriptionHash;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isOraclePresent(String wifOracleAddress) {
        for(Oracle o : oracles)
            if(o.getAddress().toLowerCase().equals(wifOracleAddress.toLowerCase()))
                return true;
        return false;
    }

    public Set<String> getPresentOracles(Set<String> oracles) {
        Set<String> retOracles = this.oracles.stream().map(Oracle::getAddress).collect(toSet());
        retOracles.retainAll(oracles);
        return retOracles;
    }

}
