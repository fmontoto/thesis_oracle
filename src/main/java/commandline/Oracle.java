package commandline;

import bitcoin.BitcoindClient;
import org.apache.commons.cli.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 29-12-16.
 */
public class Oracle {
    private static final Logger LOGGER = Logger.getLogger(Oracle.class.getName());
    private boolean testnet;
    private final String account;

    private BitcoindClient bitcoindClient;


    public Oracle(String[] args) throws ParseException {
        Options options = new Options();
        // Boolean
        options.addOption(new Option("help", "Print this message"));
        options.addOption(new Option("testnet", "Use the bitcoin testnet instead of the mainnet"));

        //String
        options.addOption(
                Option.builder("a").longOpt("account")
                        .desc("Account to get the addresses from.")
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
        if(cl.hasOption("testnet")) {
            this.testnet = true;
        }
        else {
            this.testnet = false;
        }
        this.account = cl.getOptionValue("account", "oracle");
        bitcoindClient = new BitcoindClient(this.testnet);
    }

    public void run() {
        System.out.println("Starting, please wait...");
        List<String> addresses = bitcoindClient.getAddresses(account);

        for(String address : addresses) {
            ;
        }



    }
}
