package commandline;

import communication.*;
import core.Bet;
import core.Constants;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.Secp256k1;
import org.apache.commons.cli.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 01-09-16.
 */
public class Player {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

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

    public Player() {
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

    public Player(String[] args) throws ParseException, InvalidKeySpecException, NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {
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
            formatter.printHelp("oracle Player", options);
            throw e;
        }

        if(cl.hasOption("help")) {
            formatter.printHelp("oracle Player", options);
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

    public SecureChannelManager openSecureChannel() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, ExecutionException, InterruptedException, InvalidKeySpecException, IOException {
        Secp256k1 a = new Secp256k1();
        get_configuration();

        try {
            Future<String> otherPartyCurveKeyFuture = executor.submit(
                    new PlainSocketNegotiation(other_party_addr, my_port, myKeyPair.publicKey, zctx));
            otherPartyPublicZmqKey = otherPartyCurveKeyFuture.get(1600, TimeUnit.SECONDS);

            other_party_addr = "tcp://" + other_party_location + ":" + (other_party_port + 1);
            my_port = my_port + 1;

            Future<SecureChannelManager> gotAuthenticatedChannel = executor.submit(
                    new OpenSecureChannel(zctx, myKeyPair, "tcp://*:" + my_port, my_private_key,
                            other_party_addr, otherPartyPublicZmqKey,
                            other_party_bitcoin_address, auth_sock_send, auth_sock_rcv));
            return gotAuthenticatedChannel.get(1600, TimeUnit.SECONDS);

            } catch (TimeoutException e) {
                LOGGER.severe("Unable to open the authenticated channel on time");
                return null;
            }
            finally{
                executor.shutdown();
            }
    }

    private void chat(SecureChannel channel) throws ClosedChannelException, InterruptedException {
        final String finish = "--";
        String aux;
        StreamEcho streamEcho = new StreamEcho(System.in, channel, finish);
        streamEcho.start();
        System.out.println("This is a chat to negotiate the bet.");
        System.out.println("Remember, you need to reach an agreement on the following parameters:");
        Bet.listParameters(System.out);
        System.out.println("When you're done, send " + finish + ". Then the parameters will be asked to both parties.");
        while(streamEcho.isAlive()) {
            while((aux = channel.rcv_no_wait()) != null)
                System.out.println(aux);
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private String negotiateParameter(SecureChannel channel, String parameter) throws InterruptedException, ClosedChannelException, CommunicationException {
        final String finish = "--_--";
        final String selectionPrefix = "/set ";
        String aux, match = null;
        String my_choice = "";
        NegotiateEcho negotiateEcho = new NegotiateEcho(System.in, channel, finish, selectionPrefix);
        negotiateEcho.start();
        System.out.println("Insert " + parameter + " using \""+ selectionPrefix + "<parameter>\"");
        while(match == null && negotiateEcho.isAlive() && negotiateEcho.keepRunning()) {
            while((aux = channel.rcv_no_wait()) == null){
                if(!negotiateEcho.isAlive() || !negotiateEcho.keepRunning())
                    break;
                TimeUnit.MILLISECONDS.sleep(100);
            }
            if(aux == null)
                break;
            if(aux.startsWith(selectionPrefix)) {
                String mySelection = negotiateEcho.getLastSelected();
                String otherPlayerSelection = aux.replaceFirst(selectionPrefix, "");
                if(mySelection != null && mySelection.equals(otherPlayerSelection)) {
                    match = mySelection;
                }
                else {
                    negotiateEcho.setExpectedVal(otherPlayerSelection);
                    if(negotiateEcho.keepRunning()) {
                        System.out.println("The other player just set: " + otherPlayerSelection);
                    }
                    if(negotiateEcho.getLastSelected() != null)
                        System.out.println(
                                "The other player set " + otherPlayerSelection + " which differs from your selection (" + mySelection + ")");
                }
            }
            else {
                System.out.println(aux);
            }
        }

        if(negotiateEcho.getNegotiatedParameter() != null)
            match = negotiateEcho.getNegotiatedParameter();

        if(match != null) {
            System.out.println("The parties have agreed " + parameter + " successfully:" + match);
        }
        else {
            throw new CommunicationException("Unable to reach an agreement on " + parameter + ". Closing...");
        }

        long wait = 5000;
        while(negotiateEcho.isAlive()) {
            negotiateEcho.stopRunning();
            System.out.println("Please press enter");
            negotiateEcho.join(wait);
            wait += 1000;
        }

        //TODO make sure the other user also is here (exchange data method?)

        return match;
    }

    private Bet negotiateBet(SecureChannelManager channelManager) throws CommunicationException, ClosedChannelException, InterruptedException, NoSuchAlgorithmException {
        List<String> parameters = new ArrayList<>(Arrays.asList("Description",
                                                                "Min oracles",
                                                                "Max oracles"));
        Map<String, String> results = new HashMap<>();
        for(String parameter : parameters) {
            SecureChannel channel = null;
            try {
                channel = channelManager.subscribe(parameter);
                results.put(parameter, negotiateParameter(channel, parameter));
            } finally {
                if(channel != null)
                    channel.close();
            }
        }

        throw new NotImplementedException();

    }

    public void run() throws InterruptedException, ExecutionException, NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, InvalidKeySpecException, CommunicationException {
        SecureChannelManager channelManager = openSecureChannel();
        channelManager.setDaemon(true);
        channelManager.start();
        SecureChannel negotiateBetChannel = channelManager.subscribe("negotiation");
        SecureChannel chatChannel = channelManager.subscribe("chat");
        try {
            chat(chatChannel);
            negotiateBet(channelManager);
        } catch (CommunicationException e) {
            throw e;
        }
        finally {
            channelManager.unsubscribe(negotiateBetChannel);
            negotiateBetChannel.close();
            channelManager.unsubscribe(chatChannel);
            chatChannel.close();
        }
    }
}

class NegotiateEcho extends StreamEcho{
    private final String selectionPrefix;
    private String lastSelected;
    private String expectedVal;
    private String match;


    NegotiateEcho(InputStream in, WritableByteChannel out, String finishEcho, String selectionPrefix) {
        super(in, out, finishEcho);
        this.selectionPrefix = selectionPrefix;
        lastSelected = null;
        expectedVal = null;
        match = null;
    }

    protected boolean shouldBreak(String val) {
        if(super.shouldBreak(val))
            return true;
        if(val.startsWith(selectionPrefix)) {
            synchronized (this) {
                lastSelected = val.replaceFirst(selectionPrefix, "");
                if(expectedVal != null && lastSelected.equals(expectedVal)) {
                    match = lastSelected;
                    stopRunning();
                    try {
                        send(val);
                    } catch (IOException e) {
                    }
                    return true;
                }
            }
        }
        return false;
    }

    synchronized public String getLastSelected() {
        return lastSelected;
    }

     synchronized public void setExpectedVal(String val) {
         expectedVal = val;
         if(expectedVal != null && lastSelected != null && expectedVal.equals(lastSelected)) {
             match = expectedVal;
             stopRunning();
         }
    }

    synchronized public String getNegotiatedParameter() {
        return match;
    }

}
