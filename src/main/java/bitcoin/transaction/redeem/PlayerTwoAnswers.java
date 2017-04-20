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
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.builder.InputBuilder.redeemTwoAnswers;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.OutputBuilder.oracleTwoAnswersInsuranceRedeemScript;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static bitcoin.transaction.builder.TransactionBuilder.buildTx;
import static bitcoin.transaction.builder.TransactionBuilder.setFeeFailIfNotEnough;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 4/11/17.
 */
public class PlayerTwoAnswers {
    private BitcoinPublicKey[] playerPubKeys;
    private byte[] redeemScript;
    private long timeoutSeconds;
    private Transaction tx;


    public PlayerTwoAnswers(BitcoinPublicKey[] playersPubKey, long timeoutSeconds) {
        this.playerPubKeys = playersPubKey;
        this.timeoutSeconds = timeoutSeconds;
    }

    private void findRedeemScript(byte[] expectedHash, byte[] playerAWinsHash,
                                  byte[] playerBWinsHash, BitcoinPublicKey oracleKey)
            throws NoSuchAlgorithmException, IOException {
        byte[] redeemScr = new byte[0];
        long timeout = timeoutSeconds;
        for(int i = 0; i < 2000 & !Arrays.equals(expectedHash, r160SHA256Hash(redeemScr)); ++i) {
            timeout = timeoutSeconds + i * TIMEOUT_GRANULARITY;
            redeemScr = oracleTwoAnswersInsuranceRedeemScript(
                    Arrays.asList(playerPubKeys), oracleKey,
                    Arrays.asList(playerAWinsHash, playerBWinsHash), TimeUnit.SECONDS,
                    timeout);
        }
        if(!Arrays.equals(expectedHash, r160SHA256Hash(redeemScr)))
            throw new InvalidParameterException("No able to find the redeemScript");
        timeoutSeconds = timeout;
        redeemScript = redeemScr;
    }

    public static PlayerTwoAnswers build(
            byte[] playerAAnswer, byte[] playerBAnswer, Transaction oracleSinscriptionTx, Bet bet,
            BitcoinPrivateKey playerKey, BitcoinPublicKey oracleKey, String wifDstAddress)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException,
            SignatureException, InvalidKeyException {
        if (!playerKey.getPublicKey().equals(bet.getPlayersPubKey()[0])
                && !playerKey.getPublicKey().equals(bet.getPlayersPubKey()[1])) {
            throw new InvalidParameterException("Unexpected player private key.");
        }

        PlayerTwoAnswers playerTwoAnswers = new PlayerTwoAnswers(bet.getPlayersPubKey(),
                bet.getRelativeTwoAnswersTimeoutSeconds());
        byte[] expectedHash = hexToByteArray(
                oracleSinscriptionTx.getOutput(1).getParsedScript().get(2));
        playerTwoAnswers.findRedeemScript(expectedHash, r160SHA256Hash(playerAAnswer),
                r160SHA256Hash(playerBAnswer), oracleKey);

        Input input = new Input(new AbsoluteOutput(oracleSinscriptionTx, 1),
                playerTwoAnswers.redeemScript);
        Output output = createPayToPubKeyOutput(oracleSinscriptionTx.getOutput(1).getValue(),
                wifDstAddress);
        int txVersion = 1, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, input, output);

        byte[] signature = tx.getPayToScriptSignature(playerKey, getHashType("ALL"), 0);
        tx.getInput(0).setScript(redeemTwoAnswers(playerTwoAnswers.redeemScript,
                playerAAnswer, playerBAnswer, signature));

        setFeeFailIfNotEnough(tx, 0, bet.getFee());

        tx.setTempScriptSigForSigning(0, playerTwoAnswers.redeemScript);
        signature = tx.getPayToScriptSignature(playerKey, getHashType("ALL"), 0);
        tx.getInput(0).setScript(redeemTwoAnswers(playerTwoAnswers.redeemScript,
                playerAAnswer, playerBAnswer, signature));


        playerTwoAnswers.setTx(tx);
        return playerTwoAnswers;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    public Transaction getTx() {
        return this.tx;
    }

    public byte[] getRedeemScript() {
        return redeemScript;
    }
}

