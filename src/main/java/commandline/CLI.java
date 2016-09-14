package commandline;

import core.Constants;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by fmontoto on 01-09-16.
 */
public class CLI {

    private Scanner in;
    private Context zctx;
    private Socket plain_sock_rcv;
    private Socket plain_sock_send;
    private Socket auth_sock_rcv;
    private Socket auth_sock_send;

    private ExecutorService executor;

    public CLI() {
        zctx = ZMQ.context(2);
        plain_sock_rcv = zctx.socket(ZMQ.PAIR);
        plain_sock_send = zctx.socket(ZMQ.PAIR);
        auth_sock_rcv = zctx.socket(ZMQ.PAIR);
        auth_sock_send = zctx.socket(ZMQ.PAIR);

        in = new Scanner(System.in);
        executor = Executors.newFixedThreadPool(2);
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
        System.out.println("Insert other party address");
        String address = in.nextLine();
        int port = getPort("other party", Constants.DEFAULT_PORT);
        return address + ":" + port;
    }

    private int getMyPort() {
        return getPort("local", Constants.DEFAULT_PORT);
    }

    private void startMyPort(int port) {

    }

    public void run() {
        startMyPort(getMyPort());
        String other_party_uri = getOtherPartyAddress();

    }



}
