import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRpcException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.*;

/**
 * Created by fmontoto on 30-08-16.
 */
public class OracleProtocol {

    private String otherPartyUri;
    private int oracles;
    private String startBlock;
    private String endBlock;
    private String observationHash;
    private BitcoinJSONRPCClient rpcClient;
    private Future<OracleList> oracleList;
    private ExecutorService pool;

    private OracleProtocol(String observationHash, String otherPartyUri,
                           int oracles,
                           String startBlock, String endBlock,
                           BitcoinJSONRPCClient rpcClient) throws BitcoinRpcException {

        this.otherPartyUri = otherPartyUri;
        this.oracles = oracles;
        this.startBlock = startBlock;
        this.endBlock = endBlock;
        this.rpcClient = rpcClient;
        this.observationHash = observationHash;
        pool = Executors.newSingleThreadExecutor();

        getList();
    }

    public OracleProtocol(String observationHash, String otherPartyUri,
                          int oracles, String startBlock, String endBlock,
                          BitcoinJSONRPCClient rpcClient, boolean server) {
        this(observationHash, otherPartyUri, oracles, startBlock, endBlock,
             rpcClient);
    }

    private void getList() {
        int first_height = this.rpcClient.getBlock(startBlock).height();
        int last_height = this.rpcClient.getBlock(endBlock).height();
        if(first_height > last_height) {
            throw new IllegalArgumentException(
                    "First block must be before than last block");
        }

        this.oracleList = OracleList.buildList(pool, rpcClient, startBlock, endBlock);
    }

    private void checkObservationHash() {

    }

    public void run() {
        checkObservationHash();

    }
}
