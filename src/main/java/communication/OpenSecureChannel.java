package communication;

import key.BitcoinPrivateKey;
import org.zeromq.ZAuth;
import org.zeromq.ZMQ;
import org.zeromq.ZMQQueue;
import org.zeromq.ZMsg;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 14-11-16.
 */
class ZapThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ZapThread.class.getName());
    private String otherPartyKey;
    private ZMQ.Socket sock;
    private static final Charset utf8 = Charset.forName("UTF-8");

    public ZapThread(ZMQ.Context zctx, String otherPartyKey) {

        this.otherPartyKey = otherPartyKey;
        sock = zctx.socket(ZMQ.REP);
        sock.bind("inproc://zeromq.zap.01");
        setDaemon(true);
    }

    // Copied from static ZAuth.ZAPRequest recvRequest(Socket handler), it isn't public
    static ZAuth.ZAPRequest recvRequest(ZMQ.Socket handler) {
        if (ZMQ.getMajorVersion() == 4) {
            ZMsg request = ZMsg.recvMsg(handler);
            ZAuth.ZAPRequest self = new ZAuth.ZAPRequest();
            self.handler = handler;
            self.version = request.popString();
            self.sequence = request.popString();
            self.domain = request.popString();
            self.address = request.popString();
            self.identity = request.popString();
            self.mechanism = request.popString();
            self.clientKey = request.popString(); // credentials

            assert self.version.equals("1.0");
            assert self.mechanism.equals("CURVE");

            request.destroy();
            return self;
        } else {
            return null;
        }
    }

    // Copied from static ZAuth.ZAPRequest recvRequest(Socket handler), it isn't public
    static void reply(ZAuth.ZAPRequest request, String statusCode, String statusText) {
        if (request != null) {
            ZMsg msg = new ZMsg();
            msg.add("1.0"); // Version frame
            msg.add(request.sequence); // request id
            msg.add(statusCode); // status code
            msg.add(statusText); // status text
            msg.add(""); // user id
            msg.add(""); // metadata
            msg.send(request.handler);
        }
    }

    public void run() {
        String version, sequence, domain, address, identity, mehcanism;
        while (!Thread.currentThread().isInterrupted()) {
            ZAuth.ZAPRequest zapRequest = recvRequest(sock);
            if (zapRequest.clientKey.equals(otherPartyKey)) {
                reply(zapRequest, "200", "OK");
            } else {
                reply(zapRequest, "400", "Not the expected client");
            }
        }
        sock.close();
    }
}

public class OpenSecureChannel implements Callable<Boolean> {
    private static final Logger LOGGER = Logger.getLogger(OpenSecureChannel.class.getName());
    private ZMQ.Context zctx;
    private final String myPrivateKey;
    private String myPublicKey;
    private String myURI;
    private BitcoinPrivateKey myBitcoinPrivateKey;
    private final String otherPartyURI;
    private final String otherPartyPublicKey;
    private final String otherPartyBitcoinAddr;
    private final ZMQ.Socket outgoing_socket;
    private final ZMQ.Socket incoming_socket;

    public OpenSecureChannel(ZMQ.Context zctx, String myPrivateKey, String myPublicKey, String myURI,
                             BitcoinPrivateKey myBitcoinPrivateKey,
                             String otherPartyURI, String otherPartyPublicKey, String otherPartyBitcoinAddr,
                             ZMQ.Socket outgoing_socket, ZMQ.Socket incoming_socket) {
        this.zctx = zctx;
        this.myPrivateKey = myPrivateKey;
        this.myPublicKey = myPublicKey;
        this.myURI = myURI;
        this.myBitcoinPrivateKey = myBitcoinPrivateKey;
        this.otherPartyURI = otherPartyURI;
        this.otherPartyPublicKey = otherPartyPublicKey;
        this.otherPartyBitcoinAddr = otherPartyBitcoinAddr;
        this.outgoing_socket = outgoing_socket;
        this.incoming_socket = incoming_socket;
    }

    private void startZapSecurity() {
        new ZapThread(zctx, otherPartyPublicKey).start();
    }

    private void setSecurity() {
        outgoing_socket.setCurvePublicKey(ZMQ.Curve.z85Decode(myPublicKey));
        outgoing_socket.setCurveSecretKey(ZMQ.Curve.z85Decode(myPrivateKey));
//        outgoing_socket.setIdentity("otherParty".getBytes());

        incoming_socket.setCurveServer(true);
        incoming_socket.setCurveSecretKey(ZMQ.Curve.z85Decode(myPrivateKey));
        startZapSecurity();

        //incoming_socket.setZAPDomain();
    }

    private boolean authenticateConnectedPeer() {
        return false;
//        exchangeData("bitcoinPublicKey", myBitcoinPrivateKey.getPublicKey().toWIF());

    }


//    static String exchangeData(String dataName, String myData, int expectedDataLength,
//                               ZMQ.Socket incomingSocket, ZMQ.Socket outgoingSocket) {
//    }

    @Override
    public Boolean call() throws Exception {
        setSecurity();
        incoming_socket.bind(myURI);
        outgoing_socket.connect(otherPartyURI);
        if(!authenticateConnectedPeer()) {
            incoming_socket.unbind(incoming_socket.getLastEndpoint().toString());
            incoming_socket.disconnect(otherPartyURI);
            incoming_socket.disconnect(otherPartyURI);
            return false;
        }
        return true;
    }
}
