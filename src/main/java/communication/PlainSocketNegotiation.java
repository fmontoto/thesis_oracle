package communication;

import org.zeromq.ZMQ;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static communication.Utils.exchangeData;

/**
 * Created by fmontoto on 11-11-16.
 */

public class PlainSocketNegotiation implements Callable<String> {

    private static final Logger LOGGER = Logger.getLogger(PlainSocketNegotiation.class.getName());
    private String otherPartyAddr;
    private int my_port;
    private String publicZMQKey;
    private ZMQ.Context ctx;
    private ZMQ.Socket plain_sock_rcv;
    private ZMQ.Socket plain_sock_send;
    private String otherPartyPublicKey = null;
    private static final Charset utf8 = Charset.forName("UTF-8");
    private static final String sendPublicKeyMsg = "Hi!, this is my publickey:";

    public PlainSocketNegotiation(String otherPartyAddr, int my_port, String publicZMQKey,
                                  ZMQ.Context ctx) {
        this.otherPartyAddr = otherPartyAddr;
        this.my_port = my_port;
        this.publicZMQKey = publicZMQKey;
        this.ctx = ctx;

        plain_sock_rcv = this.ctx.socket(ZMQ.PULL);
        plain_sock_send = this.ctx.socket(ZMQ.PUSH);
        // Set one outstanding connection
        plain_sock_send.setBacklog(1);
        plain_sock_rcv.setBacklog(1);
        // Only two messages at outbound queue.
        plain_sock_send.setSndHWM(2);
        // Only two messages at inbound queue
        plain_sock_rcv.setRcvHWM(2);
        plain_sock_rcv.setLinger(1000);
        plain_sock_send.setLinger(1000);



        plain_sock_rcv.bind("tcp://*:" + this.my_port);
        plain_sock_send.connect(this.otherPartyAddr);
    }

    /**
     * This function will stablish plain text communication in order to exchange ZMQ Elliptic public keys from both
     * parties to start a secure communication channel.
     * @return Public bitcoin.key got from the other party.
     */
    @Override
    public String call() {
        otherPartyPublicKey = exchangeData("zmqPublicKey", publicZMQKey, 40, plain_sock_rcv, plain_sock_send);
        plain_sock_rcv.close();
        plain_sock_send.close();
        return otherPartyPublicKey;
    }
}
