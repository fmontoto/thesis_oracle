package commandline;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import core.Constants;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static bitcoin.key.BitcoinPublicKey.WIFToTxAddress;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 29-12-16.
 */
public class Oracle {
    private static final Logger LOGGER = Logger.getLogger(Oracle.class.getName());
    private boolean testnet;
    private final String account;
    private String address;
    private String addrTxForm;
    private List<AbsoluteOutput> unspentOutputs;

    private BitcoindClient bitcoindClient;


    public Oracle(String[] args) throws ParseException {
        Options options = new Options();
        // Boolean
        options.addOption(new Option("h", "help", false, "Print this message"));
        options.addOption(new Option("t", "testnet", false, "Use the bitcoin testnet instead of the mainnet"));

        //String
        options.addOption(
                Option.builder("a").longOpt("account")
                        .desc("Account to get the addresses from.")
                        .type(String.class)
                        .numberOfArgs(1)
                        .build());

        options.addOption(
                Option.builder("d").longOpt("address")
                        .desc("Address to use.")
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
        this.address = cl.getOptionValue("address", "");
        bitcoindClient = new BitcoindClient(this.testnet);
    }

    private void startConfiguration() throws IOException, NoSuchAlgorithmException{
        System.out.println("Starting, please wait...");
        double accountBalance = bitcoindClient.getAccountBalance(account);
        if(accountBalance < 0) {
            throw new InvalidParameterException("The account does not have money");
        }
        List<AbsoluteOutput> unspent = bitcoindClient.getUnspent(account);
        List<AbsoluteOutput> availableOutputs = unspent.stream().filter(
                u -> u.isPayToKey()).collect(Collectors.toList());


        Set<String> txFormAddresses = new HashSet<String>(availableOutputs.stream().map(
                o -> o.getPayAddress()).collect(Collectors.toList()));
        String []addrList = new String[txFormAddresses.size()];
        int i = 0;
        for(String txFormAddr: txFormAddresses) {
            addrList[i] = BitcoinPublicKey.txAddressToWIF(hexToByteArray(txFormAddr), testnet);
            i++;
        }

        if(address.isEmpty()) {
            System.out.println("You didn't chose an address, select one from the followings");
            for(i = 0; i < addrList.length; i++) {
                System.out.println(i + ": " + addrList[i]);
            }

            System.out.println("Enter the number of the address you would like to use:");
            Scanner sc = new Scanner(System.in);
            int addrPos = sc.nextInt();
            if(addrPos < 0 || addrPos >= addrList.length) {
                throw new InvalidParameterException(
                        "Not a valid input, expected an int between 0 and " + addrList.length);
            }

            address = addrList[addrPos];
        }

        else if(!Arrays.asList(addrList).contains(address)){
            throw new InvalidParameterException(
                    "The address specified is not in the account/does not have spendable outputs.");
        }

        addrTxForm = byteArrayToHex(WIFToTxAddress(address));


        unspentOutputs = availableOutputs.stream().filter(
                u -> u.getPayAddress().equals(addrTxForm)).collect(Collectors.toList());
    }

    public void run() throws IOException, NoSuchAlgorithmException {
        startConfiguration();
        List<Transaction> incomingTxs = bitcoindClient.getAllIncomingTransactions(account, address);
        int confirmations = Utils.isInscribed(bitcoindClient, incomingTxs, addrTxForm);
        if(confirmations < 0) {
            System.out.println("The address " + address + "is not inscribed as oracle.");
        }

    }
}

class Utils {
    /**
     * Check if the provided address is inscribed in the provided transactions.
     * @param client
     * @param transactions
     * @param addrTxForm
     * @return -1 if the address is not inscribed, amount of confirmations of the most recent
     *      inscription if found.
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    static int isInscribed(BitcoindClient client, List<Transaction> transactions, String addrTxForm) throws NoSuchAlgorithmException  {
        int confirmations = -1;
        for(Transaction tx: transactions) {
            ArrayList<Output> outputs = tx.getOutputs();
            for(int i = 0; i < outputs.size(); i++) {
                Output out = outputs.get(i);
                if(out.isPayToKey() || out.isPayToScript())
                    continue;
                List<String> parsedScript = out.getParsedScript();
                if(parsedScript.size() == 3 && parsedScript.get(0).equals("OP_RETURN")) {
                    if(parsedScript.get(0).equals(byteArrayToHex(Constants.ORACLE_INSCRIPTION))) {
                        if(i + 1 < outputs.size()
                                    && outputs.get(i + 1).isPayToKey()
                                    && outputs.get(i + 1).getPayAddress().equals(addrTxForm)) {
                            int txConfirmations = client.getTxConfirmations(tx.txid());
                            if(confirmations < 0 || txConfirmations < confirmations)
                                confirmations = txConfirmations;
                        }
                    }
                }
            }
        }
        return confirmations;
    }
}
