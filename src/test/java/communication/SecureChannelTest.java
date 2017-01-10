package communication;

import edu.biu.SCProtocols.YaoProtocol.src.PartyTwo;
import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTOutput;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyOne;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyTwo;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import javax.rmi.CORBA.Tie;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.ClosedChannelException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 10-01-17.
 */
public class SecureChannelTest {

    ZMQ.Context zctx;
    ZMQ.Socket in, in_endpoint;
    ZMQ.Socket out, out_endpoint;
    String filter;
    Serializable serializable;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
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

        // This is just a sample of serializable object
        KeyPairGenerator kGenerator = KeyPairGenerator.getInstance("RSA");
        kGenerator.initialize(512);
        serializable = kGenerator.generateKeyPair().getPrivate();
    }

    @Test
    public void simpleScapiChannelTest() throws ClassNotFoundException, IOException {
        SecureChannel secureChannel = new SecureChannel(in, out, filter);
        secureChannel.send(serializable);
        byte[] msg = out_endpoint.recv();
        in_endpoint.send(msg, ZMQ.SNDMORE);
        msg = out_endpoint.recv();
        in_endpoint.send(msg, 0);
        Serializable received = secureChannel.receive();
        assertEquals(serializable, received);
    }

    @Test
    public void twoChannelsConnectionTest() throws IOException, ClassNotFoundException {
        Channel secureChannelOne = new SecureChannel(in, out, filter);
        Channel secureChannelTwo = new SecureChannel(out_endpoint, in_endpoint, filter);
        secureChannelOne.send(serializable);
        Serializable received = secureChannelTwo.receive();
        assertEquals(serializable, received);
    }

    @Test(expected= TimeoutException.class)
    public void failWaitingConnectionTest() throws InterruptedException, TimeoutException, CommunicationException, ClosedChannelException {
        SecureChannel partyOneChannel = new SecureChannel(in, out, filter);
        SecureChannel partyTwoChannel = new SecureChannel(out_endpoint, in_endpoint, filter);
        partyTwoChannel.waitUntilConnected(1, TimeUnit.SECONDS);

    }
    @Test
    public void waitConnectionTest() throws InterruptedException, TimeoutException, CommunicationException, ClosedChannelException {
        SecureChannel partyOneChannel = new SecureChannel(in, out, filter);
        SecureChannel partyTwoChannel = new SecureChannel(out_endpoint, in_endpoint, filter);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new WaitConnection(partyOneChannel));
        TimeUnit.MILLISECONDS.sleep(300);
        partyTwoChannel.waitUntilConnected(7, TimeUnit.SECONDS);
    }

    @Test
    public void scapiCoinToss() throws Exception {
        int length = 16;
        SecureChannel partyOneChannel = new SecureChannel(in, out, filter);
        SecureChannel partyTwoChannel = new SecureChannel(out_endpoint, in_endpoint, filter);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<CTOutput> submit = executor.submit(new Toss(partyTwoChannel, length));
        CTStringPartyOne partyOne = new CTStringPartyOne(partyOneChannel, length);
        byte[] toss = (byte [])partyOne.toss().getOutput();

        byte[] ctOutput = (byte [])submit.get(5, TimeUnit.SECONDS).getOutput();
        assertArrayEquals(toss, ctOutput);

    }

}

class Toss implements Callable<CTOutput> {
    private final int length;
    private final Channel channel;
    CTStringPartyTwo party;

    Toss(Channel channel, int length) {
        this.channel = channel;
        this.length = length;
    }

    @Override
    public CTOutput call() throws Exception {
        try {
            party = new CTStringPartyTwo(channel, length);
            return party.toss();
        } catch (Exception e) {
            Logger.getGlobal().throwing("Toss", "call", e);
            throw e;
        }
    }
}

class WaitConnection implements Runnable {

    private SecureChannel channel;

    WaitConnection(SecureChannel channel) {

        this.channel = channel;
    }
    @Override
    public void run() {
        try {
            channel.waitUntilConnected(10, TimeUnit.SECONDS);
        } catch (CommunicationException e) {
            e.printStackTrace();
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}