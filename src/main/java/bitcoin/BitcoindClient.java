package bitcoin;

import bitcoin.transaction.*;
import bitcoin.transaction.Transaction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRpcException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static core.Utils.byteArrayToHex;
import static wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class BitcoindClient {
    private static final Logger LOGGER = Logger.getLogger(BitcoindClient.class.getName());

    private BitcoindRpcClient bitcoindRpcClient;
    private boolean testnet;

    public BitcoindClient(boolean testnet) {
        this.testnet = testnet;
        bitcoindRpcClient = new BitcoinJSONRPCClient(this.testnet);
    }

    public BitcoindClient() {
        this(true);
    }

    static public void checkConnectivity(boolean testnet) {
        BitcoindRpcClient client = new BitcoinJSONRPCClient(testnet);
        client.getInfo();
    }

    public boolean isTestnet() {
        return testnet;
    }

//    public Transaction getTransaction(byte[] txHash) {
//        bitcoindRpcClient.getRawTransactionHex(byteArrayToHex(txHash));
//        throw new NotImplementedException();
//    }

    public Transaction getTransaction(String txHash) throws ParseTransactionException {
        String rawTransactionHex = bitcoindRpcClient.getRawTransactionHex(txHash);
        return new Transaction(rawTransactionHex);
    }

    public Transaction getTransaction(byte[] txHash) throws ParseTransactionException {
        return getTransaction(byteArrayToHex(txHash));
    }

//    public Transaction getTransaction(Input inTx) {
//        return getTransaction(inTx.getPrevTxHash());
//    }

    public List<String> getAddresses(String account) {
        return bitcoindRpcClient.getAddressesByAccount(account);
    }

    public double getAccountBalance(String account) {
        double balance = bitcoindRpcClient.getBalance(account);
        return balance;
    }

    private List<Transaction> getTransactions(String account, boolean bestEffort) throws ParseTransactionException {
        List<BitcoindRpcClient.Transaction> transactions = bitcoindRpcClient.listTransactions(account);
        List<Transaction> parsedTx = new ArrayList<Transaction>();
        for(BitcoindRpcClient.Transaction tx : transactions) {
            try {
                parsedTx.add(new Transaction(tx.raw().hex()));
            } catch(ParseTransactionException e) {
                if(bestEffort)
                    continue;
                throw e;
            }
        }
        return parsedTx;
    }

    public List<Transaction> getTransactions(String account) throws ParseTransactionException {
        return getTransactions(account, false);
    }

    public List<Transaction>getTransactionsBestEffort(String account) {
        try {
            return getTransactions(account, true);
        } catch (ParseTransactionException e) {
            LOGGER.severe("This isn't suppose to happen, please report;" + e.getMessage() + ";" + e.toString());
            return null;
        }
    }


    private List<Transaction> getAllTransactions(String account, String category, String address, boolean bestEffort) throws ParseTransactionException {
        final int delta = 20;
        int from = 0;

        List<Transaction> transactions = new LinkedList<>();
        List<BitcoindRpcClient.Transaction> clientTx = null;

        while(clientTx == null || clientTx.size() == delta) {
            clientTx = bitcoindRpcClient.listTransactions(account, delta, from);
            if(category.equals("*")) {
                for(BitcoindRpcClient.Transaction tx:clientTx) {
                    try {
                        transactions.add(new Transaction(tx.raw().hex()));
                    } catch (ParseTransactionException e) {
                        if(bestEffort)
                            continue;
                        else
                            throw e;
                    }
                }
            }
            else {
                for(BitcoindRpcClient.Transaction tx : clientTx) {
                    if(tx.address().equals(address) && tx.category().equals(category)) {
                        transactions.add(new Transaction((tx.raw().hex())));
                    }
                }
            }
            from += delta;
        }
        return transactions;
    }

    public List<Transaction> getAllTransactions(String account, String address) throws ParseTransactionException {
        return getAllTransactions(account, "*", address, false);
    }

    public List<Transaction> getAllIncomingTransactions(String account, String address) throws ParseTransactionException {
        return getAllTransactions(account, "receive", address, false);
    }

    private RawTransaction getRawTransaction(String txid) {
        return bitcoindRpcClient.getRawTransaction(txid);
    }

    public int getTxConfirmations(String txid) {
        return getRawTransaction(txid).confirmations();
    }

    public char[] getPrivateKey(String addr) {
        return bitcoindRpcClient.dumpPrivKey(addr).toCharArray();
    }

    private List<AbsoluteOutput> getUnspent(List<BitcoindRpcClient.Unspent> unspents) throws ParseTransactionException {
        List<AbsoluteOutput> ret = new LinkedList<>();
        for(Unspent unspent: unspents) {
            Output o = getTransaction(unspent.txid()).getOutputs().get(unspent.vout());
            ret.add(new AbsoluteOutput(o.getValue(), o.getScript(), unspent.vout(), unspent.txid()));
        }
        return ret;
    }
    public List<AbsoluteOutput> getUnspent() throws ParseTransactionException {
        return getUnspent(bitcoindRpcClient.listUnspent());
    }

    public List<AbsoluteOutput> getUnspent(int minconf) throws ParseTransactionException {
        return getUnspent(bitcoindRpcClient.listUnspent(minconf));
    }

    public List<AbsoluteOutput> getUnspent(String account) throws ParseTransactionException {
        List<BitcoindRpcClient.Unspent> unspents = bitcoindRpcClient.listUnspent();
        return getUnspent(unspents.stream().filter(
                u -> account.equals(u.account())).collect(Collectors.toList()));
    }

    public List<String> listReceivedByAddr() {
        List<String> ret = new LinkedList<>();
        List<BitcoindRpcClient.ReceivedAddress> receivedAddresses = bitcoindRpcClient.listReceivedByAddress();
        for(ReceivedAddress r: receivedAddresses)
            ret.add(r.address());
        return ret;
    }


    public List<String> getOracleList(int first_block, int last_block) {
        List<String> oracleList = new ArrayList<>();
        wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Block block;
        if(first_block > last_block)
            throw new InvalidParameterException("first block (" + first_block +
                    ") must be smaller than last block (" + last_block + ")");
        for(int height = first_block; height <= last_block; height++) {

            block = this.bitcoindRpcClient.getBlock(height);
            throw new NotImplementedException();
        }
        throw new NotImplementedException();
    }

    public String sendTransaction(String signedHexTx) {
        return bitcoindRpcClient.sendRawTransaction(signedHexTx);
    }

    public String sendTransaction(Transaction signedTx) {
        return sendTransaction(byteArrayToHex(signedTx.serialize()));
    }

    public int getBlockCount() {
        return bitcoindRpcClient.getBlockCount();
    }

    public String getBlockHash(int height) {
        return bitcoindRpcClient.getBlockHash(height);
    }

    private Block castBlock(BitcoindRpcClient.Block block) {
        if(block == null)
            return null;
        return new Block(block.hash(), block.tx(), block.nextHash(), block.height(),
                         block.time());
    }

    public Block getBlock(String blockHash) {
        return castBlock(bitcoindRpcClient.getBlock(blockHash));
    }

    public Block getBlock(int blockHeight) {
        return castBlock(bitcoindRpcClient.getBlock(blockHeight));
    }

    public String getAccountAddress(String account) {
        return bitcoindRpcClient.getAccountAddress(account);
    }

    private String verifyTransaction(String transactionHex,
                                     List<ExtendedTxInput> inputs,
                                     List<String> keys) throws BitcoindClientException {
        try {
            return bitcoindRpcClient.signRawTransaction(transactionHex, inputs, keys);
        } catch(BitcoinRpcException e) {
            System.out.println(printCLIVerification(transactionHex, inputs));
            throw new BitcoindClientException(e.getMessage());
        }
    }

    public String verifyTransaction(String transactionHex) throws BitcoindClientException {
        return verifyTransaction(transactionHex, new LinkedList<>(), new LinkedList<>());
    }

    public String verifyTransaction(Transaction tx) throws BitcoindClientException {
        return verifyTransaction(tx.hexlify());
    }

    public String verifyTransaction(Transaction tx, Collection<PayToScriptAbsoluteOutput> inputs) throws BitcoindClientException {
        List<ExtendedTxInput> inputsList = inputConversion(inputs);
        return verifyTransaction(tx.hexlify(), inputsList, new LinkedList<>());
    }

    public String verifyTransaction(Transaction tx, PayToScriptAbsoluteOutput input) throws BitcoindClientException {
        List<PayToScriptAbsoluteOutput> inputs = new LinkedList<>();
        inputs.add(input);
        return verifyTransaction(tx, inputs);
    }

    public ZonedDateTime getTxTime(String txId) {
        Date date = bitcoindRpcClient.getRawTransaction(txId).time();
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
    }

    public ZonedDateTime getTxTime(Transaction tx) throws NoSuchAlgorithmException {
        return getTxTime(tx.txid());
    }

    public String getAccount(String address) {
        return bitcoindRpcClient.getAccount(address);
    }


    public String printCLIVerification(Transaction tx, List<PayToScriptAbsoluteOutput> inputs) {
        return printCLIVerification(tx.hexlify(), inputConversion(inputs));
    }

    private String printCLIVerification(String transactionHex, List<ExtendedTxInput> inputs) {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.########");
        sb.append("./bitcoin-cli -testnet signrawtransaction ");
        sb.append(transactionHex + " '[");
        boolean first = true;
        for(ExtendedTxInput extendedTxInput : inputs) {
            if(first)
                first = false;
            else
                sb.append((","));
            sb.append("{ \"txid\": \"" + extendedTxInput.txid() + "\"");
            sb.append(", \"vout\":" + extendedTxInput.vout());
            //sb.append(", \"amount\": " + (extendedTxInput.amount().divide(new BigDecimal("100000000"))));
            sb.append(", \"amount\": " + df.format(extendedTxInput.amount()));
            sb.append(", \"redeemScript\": \"" + extendedTxInput.redeemScript() + "\"");
            sb.append(", \"scriptPubKey\": \"" + extendedTxInput.scriptPubKey() + "\"}");
        }
        //if(!first)
        //    sb.append("}");
        sb.append("]' \"[]\"");
        return sb.toString();
    }

    private List<ExtendedTxInput> inputConversion(Collection<PayToScriptAbsoluteOutput> inputs) {
        List<ExtendedTxInput> inputsList = new LinkedList<>();
        MathContext bdContext = MathContext.DECIMAL128;
        for (PayToScriptAbsoluteOutput input : inputs) {
            inputsList.add(
                    new ExtendedTxInput(input.getTxId(),
                            input.getVout(),
                            byteArrayToHex(input.getScript()),
                            byteArrayToHex(input.getRedeemScript()),
                            new BigDecimal(input.getValue(), bdContext).divide(new BigDecimal("100000000"), bdContext)));
        }
        return inputsList;
    }

    // Keep at the end
    static public class BitcoindClientException extends Exception {
        public BitcoindClientException(String message) {
            super(message);
        }
    }
}
