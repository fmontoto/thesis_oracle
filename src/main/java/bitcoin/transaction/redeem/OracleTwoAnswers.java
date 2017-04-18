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
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemTwoAnswersTimeout;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.OutputBuilder.oracleTwoAnswersInsuranceRedeemScript;
import static bitcoin.transaction.builder.TransactionBuilder.*;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 4/11/17.
 */
public class OracleTwoAnswers {
    private BitcoinPublicKey[] playerPubKeys;
    private byte[] redeemScript;
    private long timeoutSeconds;
    private Transaction tx;
    private int outputNo;


    public OracleTwoAnswers(BitcoinPublicKey[] playersPubKey, long timeoutSeconds, int outputNo) {
        this.playerPubKeys = playersPubKey;
        this.timeoutSeconds = timeoutSeconds;
        this.outputNo = outputNo;
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

    public static OracleTwoAnswers build(
            byte[] playerAWinHash, byte[] playerBWinHash, Transaction oracleInscriptionTx, Bet bet,
            BitcoinPrivateKey oracleKey, String wifDstAddress)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException,
            SignatureException, InvalidKeyException {

        int outputNo = 1;
        OracleTwoAnswers oracleTwoAnswers = new OracleTwoAnswers(bet.getPlayersPubKey(),
                bet.getRelativeTwoAnswersTimeoutSeconds(), 1);
        byte[] expectedHash = hexToByteArray(
                oracleInscriptionTx.getOutput(outputNo).getParsedScript().get(2));
        oracleTwoAnswers.findRedeemScript(expectedHash, playerAWinHash, playerBWinHash,
                                          oracleKey.getPublicKey());

        Input input = new Input(new AbsoluteOutput(oracleInscriptionTx, 1),
                oracleTwoAnswers.redeemScript);
        Output output = createPayToPubKeyOutput(oracleInscriptionTx.getOutput(1).getValue(),
                wifDstAddress);
        int txVersion = 2, txLockTime = 0;

        input.setSequenceNo((int) readScriptNum(createSequenceNumber(TimeUnit.SECONDS,
                oracleTwoAnswers.timeoutSeconds)));
        Transaction tx = buildTx(txVersion, txLockTime, input, output);
        //TODO fee

        byte[] signature = tx.getPayToScriptSignature(oracleKey, getHashType("ALL"), 0);

        tx.getInput(0).setScript(redeemTwoAnswersTimeout(oracleTwoAnswers.redeemScript,
                                      signature));


        //TODO set the fees
        oracleTwoAnswers.setTx(tx);
        return oracleTwoAnswers;
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

    public int getOutputNo() {
        return outputNo;
    }
}

