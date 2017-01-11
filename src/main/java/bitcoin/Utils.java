package bitcoin;

import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import core.*;
import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static core.Utils.byteArrayToHex;

/**
 * Created by fmontoto on 25-11-16.
 */
public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName() );

    static private Integer parseInt(String intAsString) {
        Integer ret = null;
        try {
            ret = Integer.parseInt(intAsString);
        } catch (NumberFormatException | NullPointerException e) {
            ;
        }
        return ret;
    }

    static public byte[] doubleSHA256(byte[] val) throws NoSuchAlgorithmException {
        MessageDigest dig = null;
        try {
            dig = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.throwing("Utils", "doubleSHA256", e);
            throw e;
        }
        byte [] first_digest = dig.digest(val);
        dig.reset();
        return dig.digest(first_digest);
    }

    static private Set<String> getBlockOracles(BitcoindClient client, Block block) {
        Collection<Transaction> transactions = new LinkedList<>();
        Set<String> oraclesAddress = new HashSet<>();
        for(String txId: block.getTxs())
            transactions.add(client.getTransaction(txId));
        for(Transaction tx : transactions) {
            List<Output> outputs = tx.getOutputs();
            //TODO duplicated at Oracle
            for(int i = 0; i < outputs.size(); i++) {
                Output out = outputs.get(i);
                if(out.isPayToKey() || out.isPayToScript())
                    continue;
                List<String> parsedScript = out.getParsedScript();
                if(parsedScript.size() == 3 && parsedScript.get(0).equals("OP_RETURN"))
                    if(parsedScript.get(2).equals(byteArrayToHex(core.Constants.ORACLE_INSCRIPTION)))
                        if(i + 1 < outputs.size() && outputs.get(i + 1).isPayToKey())
                            oraclesAddress.add(outputs.get(i + 1).getPayAddress());
            }
        }

        return oraclesAddress;
    }

    static List<String> compileOracleList(BitcoindClient client, String fromBlockHash, String toBlockHash) throws ExecutionException, InterruptedException {
        Set<String> oracles = new HashSet<>();
        Set<String> concurrentOracles = Collections.synchronizedSet(oracles);
        int fromBlockHeight = client.getBlock(fromBlockHash).getHeight();
        int toBlockHeight = client.getBlock(toBlockHash).getHeight();
        if(fromBlockHeight > toBlockHeight)
            throw new InvalidParameterException("fromBlock must be previous than toBlock");

        IntStream intStream = IntStream.rangeClosed(fromBlockHeight, toBlockHeight).parallel();
        ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        forkJoinPool.submit(() -> intStream.forEach(i -> concurrentOracles.addAll(getBlockOracles(client, client.getBlock(i))))).get();
//        intStream.forEach(i -> concurrentOracles.addAll(getBlockOracles(client, client.getBlock(i))));
        forkJoinPool.shutdown();

        return oracles.stream().sorted().collect(Collectors.toList());
    }

    static public List<String> getOracleList(boolean testnet, String fromBlock, String toBlock){
        String fromBlockHash, toBlockHash;
        Integer blockHeigh;
        BitcoindClient client = new BitcoindClient(testnet);

        if((blockHeigh = parseInt(fromBlock)) != null)
            fromBlockHash = client.getBlockHash(blockHeigh);
        else
            fromBlockHash = fromBlock;
        if((blockHeigh = parseInt(toBlock)) != null)
            toBlockHash = client.getBlockHash(blockHeigh);
        else
            toBlockHash = toBlock;

        try {
            return compileOracleList(client, fromBlockHash, toBlockHash);
        }catch(Exception e) {
            e.printStackTrace();
            LOGGER.severe("oh no!" + e.getMessage());
            return null;
        }
    }
}
