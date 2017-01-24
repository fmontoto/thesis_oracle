package commandline;

import bitcoin.BitcoindClient;
import bitcoin.Block;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.*;
import bitcoin.transaction.builder.TransactionBuilder;
import core.BetTxForm;
import core.Constants;
import org.apache.commons.cli.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Scanner;
import java.util.stream.IntStream;

import static bitcoin.key.BitcoinPublicKey.WIFToTxAddress;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static java.util.stream.Collectors.toList;

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

    private void startConfiguration() throws IOException, NoSuchAlgorithmException, ParseTransactionException {
        System.out.println("Starting, please wait...");
        double accountBalance = bitcoindClient.getAccountBalance(account);
        if(accountBalance < 0) {
            throw new InvalidParameterException("The account does not have money");
        }
        List<AbsoluteOutput> unspent = bitcoindClient.getUnspent(account);
        List<AbsoluteOutput> availableOutputs = unspent.stream().filter(
                u -> u.isPayToKey()).collect(toList());

        Set<String> txFormAddresses = new HashSet<String>(availableOutputs.stream().map(
                o -> o.getPayAddress()).collect(toList()));
        String []addrList = new String[txFormAddresses.size()];
        int i = 0;
        for(String txFormAddr: txFormAddresses) {
            addrList[i] = BitcoinPublicKey.txAddressToWIF(hexToByteArray(txFormAddr), testnet);
            i++;
        }

        if(address.isEmpty()) {
            System.out.println("You didn't chose an address, select one from the followings:");
            for(i = 0; i < addrList.length; i++) {
                System.out.println("\t" + i + ": " + addrList[i]);
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
                u -> u.getPayAddress().equals(addrTxForm)).collect(toList());
    }

    String inscribeOracle() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        Transaction inscriptionTx = TransactionBuilder.registerAsOracle(unspentOutputs.get(0), bitcoindClient.isTestnet());
        inscriptionTx.sign(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(address)));
        return bitcoindClient.sendTransaction(inscriptionTx);
    }

    private void notifierInputEvent(String msg) {
    }

    private void userInputEvent(String msg) {
    }

    private void runLoop(BlockingQueue<Map.Entry<String, String>> commQueue) {
        Map.Entry<String, String> input;
        String from, msg;
        Stack<Map.Entry> buffer = new Stack<>();
        boolean waitingUserInput = false;
        while(true) {
            if(!waitingUserInput && !buffer.isEmpty())
                input = buffer.pop();
            else
                input = commQueue.poll();

            from = input.getKey();
            msg = input.getKey();
            if(from.equals("user")) {
                userInputEvent(msg);
            }
            else {
                if(waitingUserInput)
                    buffer.push(input);
                else
                    notifierInputEvent(msg);
            }

        }
    }

    public void run() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException, ParseTransactionException {
        startConfiguration();
        List<Transaction> incomingTxs = bitcoindClient.getAllIncomingTransactions(account, address);
        int confirmations = Utils.isInscribed(bitcoindClient, incomingTxs, addrTxForm);
        BlockingQueue<Map.Entry<String, String>> commQueue = new ArrayBlockingQueue<>(10);
        if(confirmations < 0) {
            System.out.println("The address " + address + " is not inscribed as oracle.");
            System.out.println("Inscribe it as oracle? y/N");
            if(new Scanner(System.in).nextLine().equals("y")) {
                inscribeOracle();
            }
        }

        UserInputReader userInputReader = new UserInputReader(System.in, commQueue);
        userInputReader.start();

        runLoop(commQueue);


    }
}

class UserInputReader extends Thread {
    private final InputStream in;
    private final Scanner scanner;
    private final String commPrefix;
    Queue<Map.Entry<String, String>> commQueue;

    UserInputReader(InputStream in, Queue<Map.Entry<String, String>> commQueue) {
        this.in = in;
        scanner = new Scanner(in);
        this.commQueue = commQueue;
        commPrefix = "user";
        setDaemon(true);
    }

    public void run() {
        while(true) {
            commQueue.add(new AbstractMap.SimpleImmutableEntry<>(commPrefix, scanner.nextLine()));
        }
    }
}

class BlockChainDaemon extends Thread{
    private static final Logger LOGGER = Logger.getLogger(BlockChainDaemon.class.getName());

    private final BitcoindClient client;
    private int fromBlock;
    private final BlockingQueue<Map.Entry<String, String>> commQueue;
    private String commPrefix = "blockchain";
    private Set<String> wifOracleAddresses;


    public BlockChainDaemon(BitcoindClient client, BlockingQueue<Map.Entry<String, String>> commQueue,
                            Set<String> wifOracleAddresses) {
        fromBlock = 0;
        this.commQueue = commQueue;
        this.client = client;
        this.wifOracleAddresses = wifOracleAddresses;
        setDaemon(true);
    }

    private BlockChainDaemon(BitcoindClient client, BlockingQueue<Map.Entry<String, String>> commQueue,
                             Set<String> wifOracleAddresses, String readFromBlock) {
        fromBlock = client.getBlock(readFromBlock).getHeight();
        this.client = client;
        this.commQueue = commQueue;
        this.wifOracleAddresses = wifOracleAddresses;
        setDaemon(true);
    }

    public BlockChainDaemon(BitcoindClient client, BlockingQueue<Map.Entry<String, String>> commQueue,
                            Set<String> wifOracleAddresses, int readBlocksAgo) {
        this(client, commQueue, wifOracleAddresses, client.getBlockHash(client.getBlockCount() - readBlocksAgo));
    }

    synchronized private void sendNotification(String []strList) {
        for(String s: strList) {
            try {
                commQueue.put(new AbstractMap.SimpleImmutableEntry(commPrefix, s));
            } catch (InterruptedException e) {
                LOGGER.throwing("Notifier", "run", e);
            }
        }
    }

    private void notifyIfNeeded(String txId) {
        Transaction transaction = null;
        try {
            transaction = client.getTransaction(txId);
        } catch (ParseTransactionException e) {
            return;
        }
        ArrayList<Output> outputs = transaction.getOutputs();
        String expectedDescription = byteArrayToHex(Constants.BET_DESCRIPTION);
        for(int i = 0; i < outputs.size(); i++) {
            Output o = outputs.get(i);
            if(o.isPayToKey() || o.isPayToScript())
                continue;
            List<String> parsedScript = o.getParsedScript();
            if(parsedScript.size() == 3 && parsedScript.get(0).equals("OP_RETURN")) {
                if(parsedScript.get(3).equals(expectedDescription)) {
                    try {
                        BetTxForm betTxForm = BetTxForm.fromSerialized(hexToByteArray(parsedScript.get(3)));
                        Set<String> presentOracles = betTxForm.getPresentOracles(wifOracleAddresses);
                        if(!presentOracles.isEmpty()) {
                            for(String address : presentOracles) {
                                // Check if the bet is available and if I'm not already participating.
                                commQueue.add(new AbstractMap.SimpleImmutableEntry<>(commPrefix, "bet:" + txId));                            }
                        }

                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    } catch (InvalidParameterException e) {
                        continue;
                    }
                    ;// TODO parse the BET_DESCRIPTION
                }
            }
        }
        throw new NotImplementedException();
    }

    private Block getBlockOrWait(int height) {
        while(true) {
            try {
                return client.getBlock(height);
            }
            catch (BitcoinRPCException e){
                if(e.getResponseCode() != 500)
                    throw e;
                try {
                    TimeUnit.MINUTES.sleep(2);
                } catch (InterruptedException e1) {
                    LOGGER.throwing("Notifier", "getBlockOrWait", e);
                }
            }
        }
    }

    private void checkBlock(Block block) {
        for(String txId : block.getTxs())
            notifyIfNeeded(txId);
    }

    public void run() {
        final int confirmations = 1;
        LOGGER.info("Starting...");
        int blockCount = client.getBlockCount();
        int lastBlock = fromBlock >= blockCount - confirmations ? fromBlock : blockCount - confirmations;
        List<Integer> toCheck = IntStream.rangeClosed(fromBlock, lastBlock).boxed().collect(toList());

        while(true) {
            int blockToCheck = toCheck.get(toCheck.remove(toCheck.size() - 1));
            checkBlock(client.getBlock(blockToCheck));

            blockCount = client.getBlockCount();
            if(blockCount - confirmations > lastBlock) {
                lastBlock += 1;
                toCheck.add(lastBlock);
            }

            while(toCheck.isEmpty()) {
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    LOGGER.severe("InterruptedException:" + e.getMessage());
                    continue;
                }
                blockCount = client.getBlockCount();
                if(blockCount - confirmations > lastBlock) {
                    lastBlock += 1;
                    toCheck.add(lastBlock);
                }
            }
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
                    if(parsedScript.get(2).equals(byteArrayToHex(Constants.ORACLE_INSCRIPTION))) {
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
