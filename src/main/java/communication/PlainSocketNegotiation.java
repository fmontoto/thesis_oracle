package communication;

import org.zeromq.ZMQ;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

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
    private boolean myKeyWasReceived = false;
    private boolean receivedOtherPartyKey = false;
    private String otherPartyPublicKey = null;
    private static final Charset utf8 = Charset.forName("UTF-8");
    private static final String sendPublicKeyMsg = "Hi!, this is my publickey:";

    public PlainSocketNegotiation(String otherPartyAddr, int my_port, String publicZMQKey,
                                  ZMQ.Context ctx) {
        this.otherPartyAddr = otherPartyAddr;
        this.my_port = my_port;
        this.publicZMQKey = publicZMQKey;
        this.ctx = ctx;

        plain_sock_rcv = this.ctx.socket(ZMQ.PAIR);
        plain_sock_send = this.ctx.socket(ZMQ.PAIR);

        plain_sock_rcv.bind("tcp://*:" + this.my_port);
        plain_sock_send.connect(this.otherPartyAddr);
    }

    private void parseResponse(String response) {

        if(response.startsWith(sendPublicKeyMsg)) {
            otherPartyPublicKey = response.substring(sendPublicKeyMsg.length(), response.length());
            if(otherPartyPublicKey.length() != 40) {
                LOGGER.warning("Got an unexpected public length:" + otherPartyPublicKey.length());
                otherPartyPublicKey = null;
            }
            else{
                receivedOtherPartyKey = true;
                plain_sock_rcv.send("I got your key");
            }

        }
        else if(response.startsWith("I got your key!")) {
           myKeyWasReceived = true;
        }

    }

    @Override
    public String call() throws Exception {
        ZMQ.Poller pollItems = new ZMQ.Poller(1);
        pollItems.register(plain_sock_rcv, ZMQ.Poller.POLLIN);
        String msg;

        while (!Thread.currentThread ().isInterrupted () && (myKeyWasReceived && receivedOtherPartyKey)) {
            if(!myKeyWasReceived)
                plain_sock_send.send("Hi!, this is my publickey:" + publicZMQKey);
            pollItems.poll(500);
            if (pollItems.pollin(0)) {
                msg = plain_sock_rcv.recvStr(utf8);
                System.out.println(msg);
                parseResponse(msg);
            }
        }
        plain_sock_rcv.close();
        plain_sock_send.close();
        return otherPartyPublicKey;
    }
}
