import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by fmontoto on 30-08-16.
 */
public class OracleList {

    public OracleList() {

    }

    static Future<OracleList> buildList(ExecutorService pool,
                                        BitcoinJSONRPCClient rpcClient,
                                        String hash_init, String hash_final) {
        return pool.submit(new Callable<OracleList>() {
            @Override
            public OracleList call() throws Exception {
                return new OracleList();
            }
        });
    }

    public int size() {
        return 3;
    }
}
