package commandline;

import org.junit.Before;
import org.zeromq.ZMQ;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 06-01-17.
 */
public class StreamEchoTest {

    private ZMQ.Context zctx;
    private ZMQ.Socket in;
    private ZMQ.Socket out;
    private ZMQ.Socket in_endpoint;
    private ZMQ.Socket out_endpoint;

    @Before
    public void setUp() {
        String in_add = "inproc://in_add";
        String out_add = "inproc://out_add";
        zctx = ZMQ.context(2);
        in = zctx.socket(ZMQ.PAIR);
        out = zctx.socket(ZMQ.PAIR);
        in_endpoint = zctx.socket(ZMQ.PAIR);
        out_endpoint = zctx.socket(ZMQ.PAIR);
        in.bind(in_add);
        in_endpoint.connect(in_add);
        out.bind(out_add);
        out_endpoint.connect(out_add);
    }
}