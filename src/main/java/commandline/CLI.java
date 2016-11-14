package commandline;

import communication.PlainSocketNegotiation;
import core.Constants;
import key.Secp256k1;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 01-09-16.
 */
public class CLI {
    private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());

    private final ZMQ.Curve.KeyPair curveKey;
    private Scanner in;
    // Communication
    private Context zctx;
    private int my_port;
    private String other_party_addr;
    private Socket auth_sock_rcv;
    private Socket auth_sock_send;
    private String otherPartyPublicZmqKey;
    // Bitcoin
    private String other_party_bitcoin_address;
    private String my_private_key;
    private String my_bitcoin_address;



    private ExecutorService executor;

    public CLI() {
        zctx = ZMQ.context(2);
        auth_sock_rcv = zctx.socket(ZMQ.PAIR);
        auth_sock_send = zctx.socket(ZMQ.PAIR);

        in = new Scanner(System.in);
        executor = Executors.newFixedThreadPool(2);
        curveKey = ZMQ.Curve.generateKeyPair();
    }

    private int getPort(String port_name, int default_value) {
        int port = default_value;
        System.out.println("Insert " + port_name + "port (default=" + port + ")");
        String port_ = in.nextLine();
        if(!port_.isEmpty()) {
            try {
                port = Integer.parseInt(port_);
            }
            catch(NumberFormatException e) {
                System.out.println("Port must be a port number or empty to use default");
                return getPort(port_name, default_value);
            }
        }
        return port;
    }

    private String getOtherPartyAddress() {
        // TODO Final version can not have localhost as default, testing purposes.
        System.out.println("Insert other party address (default=localhost)");
        String address = in.nextLine();
        if(address.isEmpty())
            address = "localhost";
        int port = getPort("other party " +
                "", Constants.DEFAULT_PORT);
        return "tcp://" + address + ":" + port;
    }

    private void get_configuration() {
        my_port = getPort("local", Constants.DEFAULT_PORT);
        other_party_addr = getOtherPartyAddress();
    }

    public void run() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, ExecutionException, InterruptedException {
        Secp256k1 a = new Secp256k1();
        get_configuration();
        Future<String> otherPartyCurveKey = executor.submit(
                new PlainSocketNegotiation(other_party_addr, my_port, curveKey.publicKey, zctx));
        try {
            otherPartyPublicZmqKey = otherPartyCurveKey.get(1600, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.severe("Unable to communicate with the other party.");
            return;
        }




    }



}
