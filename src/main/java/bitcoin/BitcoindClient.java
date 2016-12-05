package bitcoin;

import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Input;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static core.Utils.byteArrayToHex;

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

    public Transaction getTransaction(byte[] txHash) {
        bitcoindRpcClient.getRawTransactionHex(byteArrayToHex(txHash));
        throw new NotImplementedException();
    }

    public Transaction getTransaction(String txHash){
        String rawTransactionHex = bitcoindRpcClient.getRawTransactionHex(txHash);
        return new Transaction(rawTransactionHex);
    }

    public Transaction getTransaction(Input inTx) {
        return getTransaction(inTx.getPrevTxHash());
    }

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

    public char[] getPrivateKey(String addr) {
        return bitcoindRpcClient.dumpPrivKey(addr).toCharArray();
    }

    private List<AbsoluteOutput> getUnspent(List<BitcoindRpcClient.Unspent> unspents) {
        List<AbsoluteOutput> ret = new LinkedList<AbsoluteOutput>();
        for(BitcoindRpcClient.Unspent unspent: unspents) {
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

    public List<String> listReceivedByAddr() {
        List<String> ret = new LinkedList<>();
        List<BitcoindRpcClient.ReceivedAddress> receivedAddresses = bitcoindRpcClient.listReceivedByAddress();
        for(BitcoindRpcClient.ReceivedAddress r: receivedAddresses)
            ret.add(r.address());
        return ret;
    }


}
