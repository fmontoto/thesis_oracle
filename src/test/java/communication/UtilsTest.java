package communication;

import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.util.concurrent.*;

import static communication.Utils.checkDataConsistencyOtherParty;
import static core.Utils.mergeArrays;
import static org.junit.Assert.*;

public class UtilsTest {
    ZMQ.Context zctx;
    ZMQ.Socket in, in_endpoint;
    ZMQ.Socket out, out_endpoint;
    String filter;
    byte[] data;

    @Before
    public void setUp(){
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
        data = new byte[] {0x03, 0x04, (byte)0xec, (byte)0xf0, (byte) 0xee};
    }

    @Test
    public void checkConsistency() throws Exception{
        long timeout = 10;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        SecureChannel partyOneChannel = new SecureChannel(in, out, filter);
        SecureChannel partyTwoChannel = new SecureChannel(out_endpoint, in_endpoint, filter);
        Future<Boolean> submit = executor.submit(new CheckConsistency(partyOneChannel, data, timeout, timeUnit));
        boolean partyTwoResult = checkDataConsistencyOtherParty(partyTwoChannel, data, timeout, timeUnit);

        assertEquals(true, submit.get());
        assertEquals(true, partyTwoResult);
    }

    @Test
    public void checkNonConsistent() throws Exception {
        long timeout = 10;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        SecureChannel partyOneChannel = new SecureChannel(in, out, filter);
        SecureChannel partyTwoChannel = new SecureChannel(out_endpoint, in_endpoint, filter);
        Future<Boolean> submit = executor.submit(new CheckConsistency(partyOneChannel, data, timeout, timeUnit));
        boolean partyTwoResult = checkDataConsistencyOtherParty(partyTwoChannel,
                                                                mergeArrays(data, data), timeout, timeUnit);

        assertEquals(false, submit.get());
        assertEquals(false, partyTwoResult);
    }
}

class CheckConsistency implements Callable<Boolean> {

    private final byte[] data;
    private final long timeoutVal;
    private final TimeUnit timeUnit;
    private final SecureChannel channel;

    CheckConsistency(SecureChannel channel, byte[] data, long timeoutVal, TimeUnit timeUnit) {

        this.channel = channel;
        this.data = data;
        this.timeoutVal = timeoutVal;
        this.timeUnit = timeUnit;
    }

    @Override
    public Boolean call() {
        try {
            return checkDataConsistencyOtherParty(channel, data, timeoutVal, timeUnit);
        } catch (TimeoutException e) {
            e.printStackTrace();
            return false;
        }
    }
}
