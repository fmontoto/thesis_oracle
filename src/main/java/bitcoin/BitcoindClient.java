package bitcoin;

import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Input;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static core.Utils.byteArrayToHex;
import static wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.*;

/**
 * Created by fmontoto on 23-11-16.
 */
public class BitcoindClient {

    private BitcoindRpcClient bitcoindRpcClient;

    public BitcoindClient(boolean testnet) {
        bitcoindRpcClient = new BitcoinJSONRPCClient(testnet);
    }

    public BitcoindClient() {
        this(true);
    }

    static public void checkConnectivity(boolean testnet) {
        BitcoindRpcClient client = new BitcoinJSONRPCClient(testnet);
        client.getInfo();
    }

//    public Transaction getTransaction(byte[] txHash) {
//        bitcoindRpcClient.getRawTransactionHex(byteArrayToHex(txHash));
//        throw new NotImplementedException();
//    }

    public Transaction getTransaction(String txHash){
        String rawTransactionHex = bitcoindRpcClient.getRawTransactionHex(txHash);
        return new Transaction(rawTransactionHex);
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

    public List<Transaction> getTransactions(String account) {
        List<BitcoindRpcClient.Transaction> transactions = bitcoindRpcClient.listTransactions(account);
        List<Transaction> parsedTx = new ArrayList<Transaction>();
        transactions.forEach(transaction -> parsedTx.add(new Transaction(transaction.raw().hex())));
        return parsedTx;
    }

    private List<Transaction> getAllTransactions(String account, String category, String address) {
        final int delta = 20;
        int from = 0;

        List<Transaction> transactions = new LinkedList<>();
        List<BitcoindRpcClient.Transaction> clientTx = null;

        while(clientTx == null || clientTx.size() != delta) {
            clientTx = bitcoindRpcClient.listTransactions(account, delta, from);
            if(category.equals("*")) {
                clientTx.forEach(tx -> transactions.add(new Transaction(tx.raw().hex())));
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
    public List<Transaction> getAllTransactions(String account, String address) {
        return getAllTransactions(account, "*", address);
    }

    public List<Transaction> getAllIncomingTransactions(String account, String address) {
        return getAllTransactions(account, "receive", address);
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

    private List<AbsoluteOutput> getUnspent(List<BitcoindRpcClient.Unspent> unspents) {
        List<AbsoluteOutput> ret = new LinkedList<AbsoluteOutput>();
        for(Unspent unspent: unspents) {
            Output o = getTransaction(unspent.txid()).getOutputs().get(unspent.vout());
            ret.add(new AbsoluteOutput(o.getValue(), o.getScript(), unspent.vout(), unspent.txid()));
        }
        return ret;
    }
    public List<AbsoluteOutput> getUnspent() {
        return getUnspent(bitcoindRpcClient.listUnspent());
    }

    public List<AbsoluteOutput> getUnspent(int minconf) {
        return getUnspent(bitcoindRpcClient.listUnspent(minconf));
    }

    public List<AbsoluteOutput> getUnspent(String account) {
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
        Block block;
        if(first_block > last_block)
            throw new InvalidParameterException("first block (" + first_block +
                    ") must be smaller than last block (" + last_block + ")");
        for(int height = first_block; height <= last_block; height++) {

            block = this.bitcoindRpcClient.getBlock(height);
            throw new NotImplementedException();
        }
        throw new NotImplementedException();
    }
}
