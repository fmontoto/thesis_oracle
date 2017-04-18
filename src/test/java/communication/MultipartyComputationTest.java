package communication;

import edu.biu.scapi.exceptions.CommitValueException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.*;

import static communication.MultipartyComputation.choseRandomlyFromList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by fmontoto on 20-01-17.
 */
public class MultipartyComputationTest {

    ZMQ.Context zctx;
    ZMQ.Socket in, in_endpoint;
    ZMQ.Socket out, out_endpoint;
    String filter;
    SecureChannelManager manager1, manager2;
    SecureChannel channel1, channel2;
    List<String> list;

    @Before
    public void setUp() {
        String in_add = "inproc://in_add";
        String out_add = "inproc://out_add";
        filter = "filter";
        zctx = ZMQ.context(2);
        in = zctx.socket(ZMQ.PAIR);
        out = zctx.socket(ZMQ.PAIR);
        in_endpoint = zctx.socket(ZMQ.PAIR);
        out_endpoint = zctx.socket(ZMQ.PAIR);
        in.setLinger(0);
        in_endpoint.setLinger(0);
        out.setLinger(0);
        out_endpoint.setLinger(0);
        in.bind(in_add);
        in_endpoint.connect(in_add);
        out.bind(out_add);
        out_endpoint.connect(out_add);
        manager1 = new SecureChannelManager(in, out, zctx);
        manager2 = new SecureChannelManager(out_endpoint, in_endpoint, zctx);
        manager1.start();
        manager2.start();
        channel1 = manager1.subscribe(filter);
        channel2 = manager2.subscribe(filter);
        list = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
    }

    @After
    public void tearDown() throws InterruptedException {
        manager1.unsubscribe(channel1);
        manager2.unsubscribe(channel2);
        channel2.close();
        channel1.close();
        manager1.closeManager();
        manager2.closeManager();
    }

    @Test
    public void choseRandomlyFromListConsistencyCheck() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int elementsToChose = 2;
        PartyTwo partyTwo = new PartyTwo(channel2, list, elementsToChose);
        Future<List<String>> submit = executor.submit(partyTwo);
        channel1.waitUntilConnected(2, TimeUnit.SECONDS);
        List<String> partyOneResult = choseRandomlyFromList(list, elementsToChose, channel1, true);
        List<String> partyTwoResult = submit.get();

        assertEquals(partyOneResult, partyTwoResult);
        assertEquals(partyOneResult.size(), elementsToChose);
        Set<String> results = new HashSet<>(partyOneResult);
        Set<String> originalInput = new HashSet<>(list);
        assertTrue(originalInput.containsAll(results));
        assertEquals(elementsToChose, results.size());
        executor.shutdown();
    }

    @Test
    public void choseRandomlyFromListConsistenyCheckTwo() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int elementsToChose = list.size() - 1;
        PartyTwo partyTwo = new PartyTwo(channel2, list, elementsToChose);
        Future<List<String>> submit = executor.submit(partyTwo);
        channel1.waitUntilConnected(2, TimeUnit.SECONDS);
        List<String> partyOneResult = choseRandomlyFromList(list, elementsToChose, channel1, true);
        List<String> partyTwoResult = submit.get();

        assertEquals(partyOneResult, partyTwoResult);
        assertEquals(partyOneResult.size(), elementsToChose);
        Set<String> results = new HashSet<>(partyOneResult);
        Set<String> originalInput = new HashSet<>(list);
        assertTrue(originalInput.containsAll(results));
        assertEquals(elementsToChose, results.size());
        executor.shutdown();

    }

    @Test(expected = InvalidParameterException.class)
    public void choseRandomlyFromListNoElements() throws Exception {
        choseRandomlyFromList(list, 0, channel2, true);
    }

    @Test(expected = InvalidParameterException.class)
    public void choseRandomlyFromListEmptyList() throws Exception {
        choseRandomlyFromList(new ArrayList<>(), 1, channel2, true);
    }

    @Test(expected = InvalidParameterException.class)
    public void choseRandomlyFromListMoreElementsThanList() throws Exception {
        choseRandomlyFromList(list, list.size() + 1, channel2, true);
    }
}

class PartyTwo implements Callable<List<String>> {

    SecureChannel channel;
    private List<String> list;
    private int elements;


    PartyTwo(SecureChannel channel, List<String> list, int elements) {
        this.channel = channel;
        this.list = list;
        this.elements = elements;
    }

    @Override
    public List<String> call() {
        try {
            channel.waitUntilConnected(2, TimeUnit.SECONDS);
            return choseRandomlyFromList(list, elements, channel, false);
        } catch (IOException | ClassNotFoundException | CommitValueException
                | InterruptedException | TimeoutException | CommunicationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
