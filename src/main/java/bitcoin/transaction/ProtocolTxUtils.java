package bitcoin.transaction;

import core.Bet;
import core.BetTxForm;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

import static bitcoin.transaction.builder.OutputBuilder.multisigScript;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 26-01-17.
 */
public class ProtocolTxUtils {
    static public int getOracleNumber(Transaction betPromiseTx, String oracleWifAddress, Bet bet) throws IOException, NoSuchAlgorithmException {
        byte[] expectedRedeemScript = multisigScript(bet.getPlayersPubKey(),
                                                     bet.getPlayersPubKey().length);
        BetTxForm betTxForm = BetTxForm.fromSerialized(
                hexToByteArray(betPromiseTx.getOutputs().get(0).getParsedScript().get(3)));
        return betTxForm.getOracles().indexOf(oracleWifAddress);
    }
}
