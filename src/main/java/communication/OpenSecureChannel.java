package communication;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import org.zeromq.ZAuth;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static communication.Utils.exchangeData;
import static communication.Utils.utf8;
import static core.Utils.mergeArrays;

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
            self.clientKey = ZMQ.Curve.z85Encode(request.pop().getData());

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
        LOGGER.info("Starting ZAP security thread");
        while (!Thread.currentThread().isInterrupted()) {
            ZAuth.ZAPRequest zapRequest = recvRequest(sock);
            if (zapRequest.clientKey.equals(otherPartyKey)) {
                reply(zapRequest, "200", "OK");
            } else {
                reply(zapRequest, "400", "Not the expected client");
                LOGGER.warning("Not expected client:" + zapRequest.address);
            }
        }
        sock.close();
    }
}

public class OpenSecureChannel implements Callable<SecureChannel> {
    private static final Logger LOGGER = Logger.getLogger(OpenSecureChannel.class.getName());
    private ZMQ.Context zctx;
    private final ZMQ.Curve.KeyPair myCurveKeyPair;
    private String myURI;
    private BitcoinPrivateKey myBitcoinPrivateKey;
    private final String otherPartyURI;
    private final String otherPartyPublicKey;
    private final String otherPartyBitcoinAddr;
    private final ZMQ.Socket outgoing_socket;
    private final ZMQ.Socket incoming_socket;
    private final boolean testnet;

    public OpenSecureChannel(ZMQ.Context zctx, ZMQ.Curve.KeyPair myCurveKeyPair, String myURI,
                             BitcoinPrivateKey myBitcoinPrivateKey,
                             String otherPartyURI, String otherPartyPublicKey, String otherPartyBitcoinAddr,
                             ZMQ.Socket outgoing_socket, ZMQ.Socket incoming_socket) {
        this.zctx = zctx;
        this.myCurveKeyPair = myCurveKeyPair;
        this.myURI = myURI;
        this.myBitcoinPrivateKey = myBitcoinPrivateKey;
        this.otherPartyURI = otherPartyURI;
        this.otherPartyPublicKey = otherPartyPublicKey;
        this.otherPartyBitcoinAddr = otherPartyBitcoinAddr;
        this.outgoing_socket = outgoing_socket;
        this.incoming_socket = incoming_socket;
        this.testnet = myBitcoinPrivateKey.isTestnet();


//        outgoing_socket.setBacklog(1);
//        incoming_socket.setBacklog(1);
        // Only two messages at outbound queue.
//        outgoing_socket.setSndHWM(2);
        // Only two messages at inbound queue
//        incoming_socket.setRcvHWM(2);
//        incoming_socket.setLinger(1000);
//        outgoing_socket.setLinger(1000);
    }

    private void startZapSecurity() {
        new ZapThread(zctx, otherPartyPublicKey).start();
    }

    private void setSecurity() {
//        return;
        startZapSecurity();
        incoming_socket.setIdentity("otherParty".getBytes());

        incoming_socket.setZAPDomain("otherParty".getBytes(utf8));
        outgoing_socket.setCurvePublicKey(ZMQ.Curve.z85Decode(myCurveKeyPair.publicKey));
        outgoing_socket.setCurveSecretKey(ZMQ.Curve.z85Decode(myCurveKeyPair.secretKey));
        outgoing_socket.setCurveServerKey(ZMQ.Curve.z85Decode(otherPartyPublicKey));
//
        incoming_socket.setCurveServer(true);
        incoming_socket.setCurveSecretKey(ZMQ.Curve.z85Decode(myCurveKeyPair.secretKey));

    }

    private boolean privateKeyPossession(BitcoinPublicKey bitcoinPublicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        byte[] generateRandomness = new byte[20];
        new SecureRandom().nextBytes(generateRandomness);
        byte[] rcvdRandomness = exchangeData("randomBytes", generateRandomness, 0,
                                              incoming_socket, outgoing_socket);

        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(myBitcoinPrivateKey);
        signer.update(mergeArrays(rcvdRandomness,
                                  ZMQ.Curve.z85Decode(myCurveKeyPair.publicKey),
                                  ZMQ.Curve.z85Decode(otherPartyPublicKey),
                                  otherPartyBitcoinAddr.getBytes(utf8)));
        byte[] signature= signer.sign();

        byte[] rcvdSignature = exchangeData("authenticationSignature", signature, 0,
                                            incoming_socket, outgoing_socket);

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(bitcoinPublicKey);
        verifier.update(generateRandomness);
        verifier.update(ZMQ.Curve.z85Decode(otherPartyPublicKey));
        verifier.update(ZMQ.Curve.z85Decode(myCurveKeyPair.publicKey));
        verifier.update(myBitcoinPrivateKey.getPublicKey().toWIF().getBytes(utf8));
        return verifier.verify(rcvdSignature);
    }

    private boolean authenticateConnectedPeer() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        byte[] otherPartyPublicBitcoinKeyBytes = exchangeData("bitcoinPublicKey",
                                                              myBitcoinPrivateKey.getPublicKey().getKey(), 0,
                                                              incoming_socket, outgoing_socket);
        // Check if the public bitcoin.key received matches the address.
        if(otherPartyPublicBitcoinKeyBytes.length != 33 && otherPartyPublicBitcoinKeyBytes.length != 65) {
            LOGGER.warning("Unexpected length of bitcoin.key received:" + otherPartyPublicBitcoinKeyBytes.length);
            return false;
        }
        BitcoinPublicKey otherPartyPublicBitcoinKey = new BitcoinPublicKey(otherPartyPublicBitcoinKeyBytes, testnet);
        if(!otherPartyBitcoinAddr.equals(otherPartyPublicBitcoinKey.toWIF())){
            LOGGER.warning("The bitcoin key provided by the other party does not match the address");
            return false;
        };

        // Check if the other party has control over the privateKey of its public bitcoin.key.
        if(!privateKeyPossession(otherPartyPublicBitcoinKey)) {
            LOGGER.warning("The other party did not prove the possession of the private bitcoin.key.");
            return false;
        };
        return true;
    }


//    static String exchangeData(String dataName, String myData, int expectedDataLength,
//                               ZMQ.Socket incomingSocket, ZMQ.Socket outgoingSocket) {
//    }

    @Override
    public SecureChannel call() throws Exception {
        setSecurity();
        incoming_socket.bind(myURI);
        outgoing_socket.connect(otherPartyURI);
        if(!authenticateConnectedPeer()) {
            System.out.println(new String(incoming_socket.getLastEndpoint(), utf8));
            incoming_socket.unbind(new String(incoming_socket.getLastEndpoint(), utf8));
            outgoing_socket.disconnect(otherPartyURI);
            return null;
        }
        throw new NotImplementedException();
//        return new SecureChannel(incoming_socket, outgoing_socket, null);
    }
}
