package communication;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.security.InvalidParameterException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 22-12-16.
 */
public class SecureChannelManagerTest {

    ZMQ.Context zctx;
    ZMQ.Socket in, in_endpoint;
    ZMQ.Socket out, out_endpoint;
    String filter;

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
    }

    @After
    public void tearDown() {
        in.close();
        out.close();
        in_endpoint.close();
        out_endpoint.close();
        zctx.term();
    }

    @Test
    public void startStop() throws Exception {
        SecureChannelManager secureChannelManager = new SecureChannelManager(in, out, zctx);
        secureChannelManager.start();
        assertTrue(secureChannelManager.isAlive());
        secureChannelManager.closeManager();
        assertFalse(secureChannelManager.isAlive());
    }

    @Test
    public void startStopMsgSent() throws Exception {
        SecureChannelManager secureChannelManager = new SecureChannelManager(in, out, zctx);
        secureChannelManager.start();
        assertTrue(secureChannelManager.isAlive());
        in_endpoint.send("msg!");
        assertTrue(secureChannelManager.isAlive());
        secureChannelManager.closeManager();
        assertFalse(secureChannelManager.isAlive());
    }

    @Test
    public void subscriptionRcv() throws Exception {
        SecureChannelManager secureChannelManager = new SecureChannelManager(in, out, zctx);
        secureChannelManager.start();
        TimeUnit.MILLISECONDS.sleep(50);
        SecureChannel subscriber = secureChannelManager.subscribe("[A]");
        TimeUnit.MILLISECONDS.sleep(50);
        in_endpoint.send("msg!");
        assertNull(subscriber.rcv_no_wait());
        in_endpoint.send("[A]", ZMQ.SNDMORE);
        in_endpoint.send("msg2");
        assertEquals("msg2", subscriber.rcv());
        secureChannelManager.unsubscribe(subscriber);
        in_endpoint.send("[A]", ZMQ.SNDMORE);
        in_endpoint.send("msg2");
        assertNull(subscriber.rcv_no_wait());
        subscriber.close();
        secureChannelManager.closeManager();
        assertFalse(secureChannelManager.isAlive());
    }

    @Test
    public void subscriptionSnd() throws Exception {
        SecureChannelManager secureChannelManager = new SecureChannelManager(in, out, zctx);
        secureChannelManager.start();
        SecureChannel subscriber = secureChannelManager.subscribe("[A]");
        TimeUnit.MILLISECONDS.sleep(50);
        subscriber.snd("message");
        assertEquals("[A]", new String(out_endpoint.recv()));
        assertEquals("message", new String(out_endpoint.recv()));
        subscriber.close();
        secureChannelManager.closeManager();
        assertFalse(secureChannelManager.isAlive());
    }

    @Test(expected= InvalidParameterException.class)
    public void unsubscribeError() throws Exception {
        SecureChannelManager secureChannelManager = new SecureChannelManager(in, out, zctx);
        SecureChannel secureChannel = new SecureChannel(in_endpoint, out_endpoint, filter);
        secureChannelManager.unsubscribe(secureChannel);

    }

}