package bitcoin.transaction.redeem;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Input;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import core.Bet;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.builder.InputBuilder.redeemUndueCharge;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.OutputBuilder.undueChargePaymentScript;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static bitcoin.transaction.builder.TransactionBuilder.buildTx;
import static bitcoin.transaction.redeem.Utils.formatPreimages;
import static bitcoin.transaction.redeem.Utils.playerNoFromPrivateKey;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 3/30/17.
 */
public class OracleWrongAnswer {

    Bet bet;
    int oraclePos;
    List<byte[]> playerAWinHashes, playerBWinHashes;

    int srcOutputNo;

    long timeoutUndue;
    byte[] redeemScript;
    Transaction tx;

    public byte[] getRedeemScript() {
        return redeemScript;
    }

    public Transaction getTransaction() {
        return tx;
    }

    public int getSrcOutputNo() {
        return srcOutputNo;
    }

    private void setTransaction(Transaction tx) {
        this.tx = tx;
    }

    public OracleWrongAnswer(Bet bet, int oraclePos, List<byte[]> playerAWinHashes,
                             List<byte[]> playerBWinHashes, int srcOutputNo) {
        this.bet = bet;
        this.oraclePos = oraclePos;
        this.playerAWinHashes = playerAWinHashes;
        this.playerBWinHashes = playerBWinHashes;
        this.srcOutputNo = srcOutputNo;
    }

    private void findRedeemScript(byte[] expectedHash, BitcoinPublicKey oraclePubKey)
            throws IOException, NoSuchAlgorithmException {
        long timeout = bet.getRelativeUndueChargeTimeoutSeconds();
        byte[] redeemScript;
        redeemScript = undueChargePaymentScript(
                bet.getPlayersPubKey(), oraclePubKey, playerAWinHashes.get(oraclePos),
                playerBWinHashes.get(oraclePos), playerAWinHashes, playerBWinHashes,
                bet.getRequiredHashes(), timeout);

        for(int i = 1; i < 2000 && !Arrays.equals(expectedHash, r160SHA256Hash(redeemScript));
            ++i) {
            timeout = bet.getRelativeUndueChargeTimeoutSeconds() + i * TIMEOUT_GRANULARITY;
            redeemScript = undueChargePaymentScript(
                    bet.getPlayersPubKey(), oraclePubKey, playerAWinHashes.get(oraclePos),
                    playerBWinHashes.get(oraclePos), playerAWinHashes, playerBWinHashes,
                    bet.getRequiredHashes(), timeout);
        }

        if(!Arrays.equals(expectedHash, r160SHA256Hash(redeemScript)))
            throw new InvalidParameterException("Couldn't get the redeem script.");

        this.redeemScript = redeemScript;
        this.timeoutUndue = timeout;
    }

    static public OracleWrongAnswer build(
            Bet bet, Transaction betTransaction, int oraclePos, BitcoinPublicKey oraclePublicKey,
            List<byte[]> playerAWinHashes, List<byte[]> playerBWinHashes,
            List<byte[]> winnerPreImages, byte[] oracleWrongWinnerPreImage,
            BitcoinPrivateKey winnerPlayerKey, String wifOutputAddress)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        if(bet.getRequiredHashes() > winnerPreImages.size())
            throw new InvalidParameterException("Not enough pre images");

        int playerNo = playerNoFromPrivateKey(bet.getPlayersPubKey(), winnerPlayerKey);
        int srcOutputNo = 2 + 2 * oraclePos + 1;

        winnerPreImages = new LinkedList<>(winnerPreImages);
        while(winnerPreImages.size() > bet.getRequiredHashes())
            winnerPreImages.remove(0);
        List<byte[]> formattedPreImages = formatPreimages(playerAWinHashes, playerBWinHashes,
                                                          winnerPreImages);
        Output output1 = betTransaction.getOutputs().get(srcOutputNo);
        byte[] expectedHash =  hexToByteArray(
                output1.getParsedScript().get(2));
        //TODO fee
        long value = output1.getValue() - 100;

        OracleWrongAnswer oracleWrongAnswer = new OracleWrongAnswer(
                bet, oraclePos, playerAWinHashes, playerBWinHashes, srcOutputNo);
        oracleWrongAnswer.findRedeemScript(expectedHash, oraclePublicKey);

        Input input = new Input(new AbsoluteOutput(betTransaction, srcOutputNo),
                                oracleWrongAnswer.getRedeemScript());
        Output output = createPayToPubKeyOutput(value, wifOutputAddress);
        int txVersion = 2, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, input, output);
        byte[] signature = tx.getPayToScriptSignature(winnerPlayerKey, getHashType("ALL"), 0);
        tx.getInputs().get(0).setScript(redeemUndueCharge(
                oracleWrongAnswer.redeemScript, signature, oracleWrongWinnerPreImage, playerNo,
                winnerPlayerKey.getPublicKey(), formattedPreImages));

        oracleWrongAnswer.setTransaction(tx);
        return oracleWrongAnswer;
    }
}
