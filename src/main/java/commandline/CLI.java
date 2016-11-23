package commandline;

import communication.OpenSecureChannel;
import communication.PlainSocketNegotiation;
import core.Constants;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.Secp256k1;
import org.apache.commons.cli.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 01-09-16.
 */
public class CLI {
    private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());

    private final ZMQ.Curve.KeyPair myKeyPair;
    private Scanner in;
    // Communication
    private Context zctx;
    private int my_port;
    private String other_party_addr;
    private Socket auth_sock_rcv;
    private Socket auth_sock_send;
    private int other_party_port;
    private String other_party_location;
    private String otherPartyPublicZmqKey;
    // Bitcoin
    private String other_party_bitcoin_address;
    private BitcoinPrivateKey my_private_key;
    private String my_bitcoin_address;



    private ExecutorService executor;

    public CLI() {
        zctx = ZMQ.context(2);
        auth_sock_rcv = zctx.socket(ZMQ.PULL);
        auth_sock_send = zctx.socket(ZMQ.PUSH);

        in = new Scanner(System.in);
        executor = Executors.newFixedThreadPool(2);
        myKeyPair = ZMQ.Curve.generateKeyPair();

        my_port = 0;
        otherPartyPublicZmqKey = null;
        other_party_bitcoin_address = null;
        other_party_location = null;
        other_party_port = 0;
        my_bitcoin_address = null;
        my_private_key = null;
    }

    public CLI(String[] args) throws ParseException, InvalidKeySpecException, NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {
        this();
        Options options = new Options();
        // Boolean
        options.addOption(new Option("help", "Print this message"));
        // String
        options.addOption(
                Option.builder("p").longOpt("my-port")
                                   .desc("Port to bind.")
                                   .type(Number.class)
                                   .numberOfArgs(1)
                                   .build());
        options.addOption(
                Option.builder("c").longOpt("connect-port")
                        .desc("Port to connect with.")
                        .type(Number.class)
                        .numberOfArgs(1)
                        .build());
        options.addOption(
                Option.builder("l").longOpt("location")
                        .desc("Other party's address.")
                        .type(String.class)
                        .numberOfArgs(1)
                        .build());
        options.addOption(
                Option.builder("k").longOpt("bitcoin/key")
                        .desc("My bitcoin private bitcoin.key (WIF).")
                        .type(String.class)
                        .numberOfArgs(1)
                        .build());
        options.addOption(
                Option.builder("a").longOpt("address")
                        .desc("Other party's bitcoin address. (WIF)")
                        .type(String.class)
                        .numberOfArgs(1)
                        .build());
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cl;
        try {
            cl = parser.parse(options, args);
        }
        catch(ParseException e) {
            LOGGER.severe("Parsing failed:" + e.getMessage());
            formatter.printHelp("oracle CLI", options);
            throw e;
        }

        if(cl.hasOption("help")) {
            formatter.printHelp("oracle CLI", options);
            throw new ExceptionInInitializerError();
        }
        if(cl.hasOption("my-port"))
            my_port = Integer.parseInt(cl.getOptionValue("my-port"));
        if(cl.hasOption("connect-port"))
            other_party_port = Integer.parseInt(cl.getOptionValue("connect-port"));
        if(cl.hasOption("location"))
            other_party_location = cl.getOptionValue("location");
        if(cl.hasOption("bitcoin/key"))
            my_private_key = BitcoinPrivateKey.fromWIF(cl.getOptionValue("bitcoin/key"));
        if(cl.hasOption("address"))
            other_party_bitcoin_address = cl.getOptionValue("address");
    }

    //TODO remove this function and use getUserInput instead
    private int getPort(String port_name, int default_value) {
        int port = default_value;
        System.out.println("Insert " + port_name + " port (default=" + port + ")");
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

    private String getUserInput(String message, String def, Predicate<String> predicate, int retries) {
        while(retries-- != 0) {
            System.out.println(message + " (default=" + def + ")");
            String usrInput = in.nextLine();
            if(usrInput.isEmpty()) {
                System.out.println("Using default value:" + def);
                return def;
            }
            if(predicate.test(usrInput))
                return usrInput;
            System.out.println("Not a valid input, " + retries + " retries left");
        }
        throw new IllegalArgumentException();
    }

    private String getUserInput(String message, Predicate<String> predicate, int retries) {
        while(retries-- != 0) {
            System.out.println(message + " (no default available)");
            String usrInput = in.nextLine();
            if(usrInput.isEmpty()) {
                System.out.println("No default available.");
            }
            if(predicate.test(usrInput))
                return usrInput;
            System.out.println("Not a valid input, " + retries + " retries left");
        }
        throw new IllegalArgumentException();

    }

    private void get_configuration() throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        if(my_port == 0)
            my_port = getPort("local", Constants.DEFAULT_PORT);
        if(other_party_port == 0)
            other_party_port = getPort("other party", Constants.DEFAULT_PORT);
        if(other_party_location == null)
            other_party_location = getUserInput("Insert other party address", "localhost",
                                                (input) -> true, 3);
        if(my_private_key == null) {
            String defaultStr = "<one will be generated(testnet)>";
            String usrInput = getUserInput("Insert your private bitcoin.key (WIF format)", defaultStr,
                                           bitcoin.key.Utils::isValidPrivateKeyWIF, 3);
            if(usrInput.isEmpty() || usrInput.equals(defaultStr))
                my_private_key = new BitcoinPrivateKey(false, true);
            else
                my_private_key = BitcoinPrivateKey.fromWIF(usrInput);
        }
        if(other_party_bitcoin_address == null)
            other_party_bitcoin_address =
                    getUserInput("Insert other party bitcoin b58 encoded address",
                                 (String input) -> input.length() > 26 && input.length() < 35, 3);
        other_party_addr = "tcp://" + other_party_location + ":" + other_party_port;
    }

    public void run() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, ExecutionException, InterruptedException, InvalidKeySpecException, IOException {
        Secp256k1 a = new Secp256k1();
        get_configuration();

        try {
            Future<String> otherPartyCurveKeyFuture = executor.submit(
                    new PlainSocketNegotiation(other_party_addr, my_port, myKeyPair.publicKey, zctx));
            otherPartyPublicZmqKey = otherPartyCurveKeyFuture.get(1600, TimeUnit.SECONDS);
            System.out.println("Got ZMQ bitcoin.key!");


            other_party_addr = "tcp://" + other_party_location + ":" + (other_party_port + 1);
            my_port = my_port + 1;


            Future<Boolean> gotAuthenticatedChannel = executor.submit(
                    new OpenSecureChannel(zctx, myKeyPair, "tcp://*:" + my_port, my_private_key,
                            other_party_addr, otherPartyPublicZmqKey,
                            other_party_bitcoin_address, auth_sock_send, auth_sock_rcv));
            if(!gotAuthenticatedChannel.get(1600, TimeUnit.SECONDS)) {
                LOGGER.severe("Unable to authenticate the channel.");
                return;
            }
            } catch (TimeoutException e) {
                LOGGER.severe("Unable to open the authenticated channel on time");
                return;
            }
            finally{
                executor.shutdown();
            }
    }
}
