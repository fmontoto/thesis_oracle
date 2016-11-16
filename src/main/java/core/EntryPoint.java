package core;

import commandline.CLI;
import org.apache.commons.cli.ParseException;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static spark.Spark.get;

/**
 * Created by fmontoto on 30-08-16.
 */
public class EntryPoint {
    private static final Logger LOGGER = Logger.getLogger(EntryPoint.class.getName());
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        try {
            if (args.length < 1) {
                new CLI().run();
            } else if (args[0].equals("CLI")) {
                new CLI(Arrays.copyOfRange(args, 1, args.length)).run();
            } else {
                get("/hello", (req, res) -> "Hello World");
                System.out.println("Hello World!");
                String last_hash = "0000000000000000035a7fab2ca3876567f2f03ac5d6436d9b1f243151839094";
                String first_hash = "00000000000000000254870c1d46ebb99b7c309f13dda454b1347d9a4fa3026d";
                BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(false);
                OracleProtocol op = new OracleProtocol("", "", 3, first_hash, last_hash, client, true);
            }
        }
        catch(InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            LOGGER.severe("Algorithm problem:" + e.getMessage());
        }
        catch(ParseException e) {
            return;
        }
        catch(Exception e) {
            LOGGER.severe("Exception (" + e.getClass() + ") received:" + e.getMessage() + ".");
            e.printStackTrace();
            return;
        }
    }

}
