package bitcoin.transaction.protocol;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Input;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import core.Bet;
import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.parseScript;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemMultiSigOutput;
import static bitcoin.transaction.builder.InputBuilder.redeemPaymentOracleDidntPay;
import static bitcoin.transaction.builder.OutputBuilder.betOraclePaymentScript;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static bitcoin.transaction.builder.TransactionBuilder.buildTx;
import static bitcoin.transaction.builder.TransactionBuilder.createSequenceNumber;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 3/23/17.
 */
public class OracleDoesntAnswer {

    private final int oraclePosition;
    List<BitcoinPublicKey> playersPublicKey;
    private Transaction tx;
    private byte[] signature;
    byte[] redeemScript;
    private boolean isReady = false;

    static public int RedeemOutputPosition(int oraclePosition) {
        return 2 + oraclePosition * 2;
    }

    public int RedeemOutputPosition() {
        return RedeemOutputPosition(oraclePosition);
    }

    public byte[] getRedeemScript() {
        return redeemScript;
    }

    private OracleDoesntAnswer(List<BitcoinPublicKey> playersPublicKey, byte[] signature,
                               byte[] redeemScript, Transaction tx, int oraclePosition) {
        this.playersPublicKey = playersPublicKey;
        this.signature = signature;
        this.redeemScript = redeemScript;
        this.tx = tx;
        this.isReady = false;
        this.oraclePosition = oraclePosition;
    }

    static public OracleDoesntAnswer build(
            Transaction betTransaction, int oraclePosition, BitcoinPublicKey oraclePubKey,
            byte[] playerAWinHash, byte[] playerBWinHash, BitcoinPrivateKey playerPrivKey, Bet bet,
            List<String> wifOutputs) throws IOException, NoSuchAlgorithmException,
            SignatureException, InvalidKeyException, InvalidKeySpecException {

        if (!Arrays.equals(bet.getPlayersPubKey()[0].getKey(), playerPrivKey.getPublicKey().getKey())
                && !Arrays.equals(bet.getPlayersPubKey()[1].getKey(),
                playerPrivKey.getPublicKey().getKey()))
            throw new InvalidParameterException("Priv key must be from one of the players");

        int outputPos = RedeemOutputPosition(oraclePosition);
        byte[] expectedHash = hexToByteArray(
                betTransaction.getOutputs().get(outputPos).getParsedScript().get(2));
        long available = betTransaction.getOutputs().get(outputPos).getValue();
        //TODO fee...
        long fee = 10;
        long eachOutput =  (available - fee) / wifOutputs.size();

        List<Output> outputs = new LinkedList<>();
        for(String address: wifOutputs)
            outputs.add(createPayToPubKeyOutput(eachOutput, address));

        OracleAnswer.RedeemOutput redeemOutput = new OracleAnswer.RedeemOutput(
                playerAWinHash, playerBWinHash, oraclePubKey, bet.getPlayersPubKey()[0],
                bet.getPlayersPubKey()[1], bet.getRelativeBetResolutionSecs(),
                bet.getRelativeReplyUntilTimeoutSeconds(), expectedHash);


        AbsoluteOutput ao = new AbsoluteOutput(betTransaction, outputPos);
        Input input = new Input(ao, redeemOutput.redeemScript);
        input.setSequenceNo((int) readScriptNum(createSequenceNumber(TimeUnit.SECONDS,
                redeemOutput.replyUntilSeconds)));
        int txVersion = 2, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, input, outputs);
        byte[] signature = tx.getPayToScriptSignature(playerPrivKey, getHashType("ALL"), 0);
        return new OracleDoesntAnswer(Arrays.asList(bet.getPlayersPubKey()), signature,
                redeemOutput.redeemScript, tx, oraclePosition);
    }

    public Transaction sign(BitcoinPrivateKey otherPlayerKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException,
            InvalidKeySpecException {
        if (isReady)
            throw new InvalidStateException("Transaction is ready.");
        byte[] secondSignature = tx.getPayToScriptSignature(otherPlayerKey, getHashType("ALL"), 0);
        int thisPlayer;
        if (Arrays.equals(playersPublicKey.get(0).getKey(),
                otherPlayerKey.getPublicKey().getKey())) {
            thisPlayer = 0;
        } else if (Arrays.equals(playersPublicKey.get(1).getKey(),
                otherPlayerKey.getPublicKey().getKey())) {
            thisPlayer = 1;
        } else {
            throw new InvalidParameterException("Priv key must be from one of the players");
        }

        if (Arrays.equals(secondSignature, signature))
            throw new InvalidParameterException(
                    "Same player signed twice. (or signature collision, sorry)");

        if (thisPlayer == 0)
            tx.getInputs().get(0).setScript(redeemPaymentOracleDidntPay(redeemScript,
                    secondSignature,
                    signature));
        else
            tx.getInputs().get(0).setScript(redeemPaymentOracleDidntPay(redeemScript,
                    signature,
                    secondSignature));
        return tx;
    }

}
