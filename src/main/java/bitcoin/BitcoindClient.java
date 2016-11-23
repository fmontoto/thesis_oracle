package bitcoin;

import bitcoin.transaction.Input;
import org.bitcoinj.core.Transaction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

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

    public Transaction getTransaction(Input inTx) {
        return getTransaction(inTx.getPrevTxHash());
    }
}
