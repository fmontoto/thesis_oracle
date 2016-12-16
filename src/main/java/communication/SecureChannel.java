package communication;

import org.zeromq.ZMQ;

/**
 * Created by fmontoto on 16-12-16.
 */
public class SecureChannel {
    public ZMQ.Socket in;
    public ZMQ.Socket out;
    private boolean open;


    public SecureChannel(ZMQ.Socket in, ZMQ.Socket out) {
        this.in = in;
        this.out = out;
        open = true;
    }

    public void close() {
        in.close();
        out.close();
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

}
