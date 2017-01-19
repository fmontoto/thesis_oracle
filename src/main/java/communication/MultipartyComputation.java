package communication;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.exceptions.CommitValueException;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyOne;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyTwo;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

/**
 * Created by fmontoto on 19-01-17.
 */
public class MultipartyComputation {

    static private int log(int x, int base) {
        return Math.toIntExact((long)Math.ceil(Math.log(x) / Math.log(base)));
    }

    static private long bytesToLong(byte[] bytes) {
        assert bytes.length <= 4;
        long result = 0;
        for(int i = 0; i < bytes.length; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        assert result > 0;
        return result;
    }

    private static <T> List<T> choseRandomlyFromListStandard(List<T> list, int neededElements, Channel channel, boolean partyOne) throws IOException, ClassNotFoundException, CommitValueException {
        CTStringParty ctStringParty;
        int bitsToGet = log(list.size(), 2);
        bitsToGet = bitsToGet < 8 ? 8 : bitsToGet;

        if(partyOne)
            ctStringParty = new CTStringParty(new CTStringPartyOne(channel, bitsToGet * 8));
        else
            ctStringParty = new CTStringParty(new CTStringPartyTwo(channel, bitsToGet * 8));

        while(neededElements > 0) {
            long gotNumber = bytesToLong((byte[])ctStringParty.toss().getOutput());
            if(gotNumber >= list.size())
                continue;
            
        }
        throw new NotImplementedException();
    }

    private static <T> List<T> choseRandomlyFromListByRemoving(List<T> list, int neededElements, Channel channel, boolean partyOne) {
        throw new NotImplementedException();
    }


    static public <T> List<T> choseRandomlyFromList(List<T> list, int neededElements, Channel channel, boolean partyOne) throws IOException, ClassNotFoundException, CommitValueException {
        if(neededElements <= 0)
            throw new InvalidParameterException("neededElements must be a positive value");
        if(list.size() <= neededElements)
            throw new InvalidParameterException(
                    "Can not get " + neededElements + " elements from a list of size " + list.size());

        if(neededElements < list.size() - neededElements)
            return choseRandomlyFromListStandard(list, neededElements, channel, partyOne);
        else
            return choseRandomlyFromListByRemoving(list, neededElements, channel, partyOne);
    }

    private List<String> choseOraclesRandomly(int neededOracles, SecureChannelManager channelManager) throws IOException, ClassNotFoundException, CommitValueException, InterruptedException, TimeoutException, CommunicationException, NoSuchAlgorithmException, ConsistencyException {
        List<String> oracles = new ArrayList<>();
        SecureChannel oracleNegotiationChannel = null, channel = null;
        CTOutput result;
        List<String> oraclesList = buildOracleList(neededOracles, channelManager);
        byte[] reducedList = oraclesList.stream().reduce("", (a, b) -> a + b).getBytes(utf8);
        channel = channelManager.subscribe("checkListConsistency");
        if(!checkDataConsistencyOtherParty(channel, r160SHA256Hash(reducedList), 25, TimeUnit.SECONDS))
            throw new ConsistencyException("Both parties have different lists of oracles.");

        List<String> chosenOracles = new LinkedList<>();
        CTStringParty ctStringParty;
        try{
            oracleNegotiationChannel = channelManager.subscribe("randomOracleNegotiation");
            oracleNegotiationChannel.waitUntilConnected(15, TimeUnit.SECONDS);
            if(amIPartyOne())
                ctStringParty = new CTStringParty(new CTStringPartyOne(oracleNegotiationChannel, neededOracles));
            else
                ctStringParty = new CTStringParty(new CTStringPartyTwo(oracleNegotiationChannel, neededOracles));
            result = ctStringParty.toss();

        } finally {
            if(oracleNegotiationChannel != null) {
                channelManager.unsubscribe(oracleNegotiationChannel);
                oracleNegotiationChannel.close();
            }
        }
        System.out.println(result.getOutput());
        return oracles;
    }

}
