package communication;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.exceptions.CommitValueException;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyOne;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyTwo;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
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

        assert result >= 0;
        return result;
    }

    static private int mask(int numberOfOnes) {
        return (1 << numberOfOnes) - 1;
    }

    private static <T> List<T> choseRandomlyFromListStandard(List<T> list, int neededElements, Channel channel, boolean partyOne) throws IOException, ClassNotFoundException, CommitValueException {
        //TODO there some duplicated lines with choseRandomlyFromListByRemoving, refactor into choseRandomlyFromList
        CTStringParty ctStringParty;
        int bitsToGet = log(list.size(), 2);
        int bytesToGet = (int) Math.ceil(bitsToGet / 8.0);
        List<T> copyList = new LinkedList<T>(list);
        List<T> retList = new LinkedList<T>();

        if(partyOne)
            ctStringParty = new CTStringParty(new CTStringPartyOne(channel, bytesToGet * 8));
        else
            ctStringParty = new CTStringParty(new CTStringPartyTwo(channel, bytesToGet * 8));

        while(neededElements > 0) {
            byte[] results = (byte[]) ctStringParty.toss().getOutput();
            if(results.length == 1 && bitsToGet < 8) // This will speed up most cases.
                results[0] = (byte) (results[0] & mask(bitsToGet));
            long gotNumber = bytesToLong(results);
            if(gotNumber >= copyList.size())
                continue;
            retList.add(copyList.remove(Math.toIntExact(gotNumber)));
            neededElements--;
            // Is this method takes too long an optimization could be to start again the CTStringPartyxxx
            // objects with the amount of bits to calculate updated with the current list size.
        }
        return retList;
    }

    private static <T> void choseRandomlyFromListByRemoving(List<T> list, int elementsToRemove, Channel channel, boolean partyOne) throws IOException, ClassNotFoundException, CommitValueException {
        CTStringParty ctStringParty;
        int bitsToGet = log(list.size(), 2);
        int bytesToGet = (int) Math.ceil(bitsToGet / 8.0);
        assert bytesToGet > 0;
        if(partyOne)
            ctStringParty = new CTStringParty(new CTStringPartyOne(channel, bytesToGet * 8));
        else
            ctStringParty = new CTStringParty(new CTStringPartyTwo(channel, bytesToGet * 8));

        while(elementsToRemove > 0) {
            byte[] results = (byte[]) ctStringParty.toss().getOutput();
            if(results.length == 1 && bitsToGet < 8) // This will speed up most cases.
                results[0] = (byte) (results[0] & mask(bitsToGet));
            long gotNumber = bytesToLong(results);
            if(gotNumber >= list.size())
                continue;
            list.remove(Math.toIntExact(gotNumber));
            elementsToRemove--;
        }
    }


    static public <T> List<T> choseRandomlyFromList(List<T> list, int neededElements, Channel channel, boolean partyOne) throws IOException, ClassNotFoundException, CommitValueException {
        if(neededElements <= 0)
            throw new InvalidParameterException("neededElements must be a positive value");
        if(list.size() <= neededElements)
            throw new InvalidParameterException(
                    "Can not get " + neededElements + " elements from a list of size " + list.size());
        List<T> retList;
        if(neededElements < list.size() - neededElements)
            return choseRandomlyFromListStandard(list, neededElements, channel, partyOne);
        else
            retList = new LinkedList<T>(list);
            choseRandomlyFromListByRemoving(retList, list.size() - neededElements, channel, partyOne);
            return retList;
    }
}
