package bitcoin.transaction.protocol;

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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemBetOraclePaymentScript;
import static bitcoin.transaction.builder.OutputBuilder.betOraclePaymentScript;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static bitcoin.transaction.builder.TransactionBuilder.buildTx;
import static bitcoin.transaction.builder.TransactionBuilder.createSequenceNumber;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 3/22/17.
 */
public class OracleAnswer {

    static class RedeemOutput {
        long replyUntilSeconds;
        long betTimeoutSeconds;
        byte[] redeemScript;

        public RedeemOutput(byte[] playerAWinHash, byte[] playerBWinHash,
                            BitcoinPublicKey oraclePubKey, BitcoinPublicKey playerAPublicKey,
                            BitcoinPublicKey playerBPublicKey, long baseTimeoutSeconds,
                            long baseReplyUntilSeconds, byte[] expectedHash) throws NoSuchAlgorithmException, IOException {
            redeemScript = new byte[1];
            int i = 0;
            for(; i < 2000 && !Arrays.equals(r160SHA256Hash(redeemScript), expectedHash); ++i) {
                for(int j = 0; j < 2000 && !Arrays.equals(r160SHA256Hash(redeemScript), expectedHash); ++j) {
                    betTimeoutSeconds = baseTimeoutSeconds + i * TIMEOUT_GRANULARITY;
                    replyUntilSeconds = baseReplyUntilSeconds + j * TIMEOUT_GRANULARITY;
                    redeemScript = betOraclePaymentScript(playerAWinHash, playerBWinHash,
                                                          oraclePubKey, playerAPublicKey,
                                                          playerBPublicKey, betTimeoutSeconds,
                                                          replyUntilSeconds);
                }
            }
            if(!Arrays.equals(r160SHA256Hash(redeemScript), expectedHash))
                throw new InvalidParameterException();
        }
    }

    byte[] redeemScript;
    Transaction answer;

    private OracleAnswer(byte[] redeemScript, Transaction answer) {
        this.redeemScript = redeemScript;
        this.answer = answer;
    }

    public byte[] getRedeemScript() {
        return redeemScript;
    }

    public Transaction getAnswer() {
        return answer;
    }

    static public OracleAnswer build(
            Transaction betTransaction, Bet bet, int oraclePosition, byte[] winnerHashPreImage,
            BitcoinPrivateKey oracleKey, String dstWIFAddress, List<byte[]> playersWinHash)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException,
            SignatureException, InvalidKeyException {
        int winnerPos = -1;
        for(int i = 0; i < playersWinHash.size(); i++) {
            if(Arrays.equals(r160SHA256Hash(winnerHashPreImage), playersWinHash.get(i)))
                winnerPos = i;
        }
        if(winnerPos == -1)
            throw new InvalidParameterException("Not a valid winner pre hash.");

        int toRedeemOutputPos = 2 + 2 * oraclePosition;
        long available = betTransaction.getOutputs().get(oraclePosition).getValue();
        long fee = 100; // TODO calculate it...

        byte[] expectedRedeeemHash = hexToByteArray(betTransaction.getOutputs()
                .get(toRedeemOutputPos).getParsedScript().get(2));
        RedeemOutput redeemOutput = new RedeemOutput(playersWinHash.get(0), playersWinHash.get(1),
                oracleKey.getPublicKey(), bet.getPlayersPubKey()[0], bet.getPlayersPubKey()[1],
                bet.getRelativeBetResolutionSecs(), bet.getRelativeReplyUntilTimeoutSeconds(),
                expectedRedeeemHash);

        AbsoluteOutput ao = new AbsoluteOutput(betTransaction, toRedeemOutputPos);
        Input input = new Input(ao, redeemOutput.redeemScript);
        input.setSequenceNo((int) readScriptNum(createSequenceNumber(TimeUnit.SECONDS,
                redeemOutput.betTimeoutSeconds)));
        Output output = createPayToPubKeyOutput(available - fee, dstWIFAddress);
        int txVersion = 2, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, input, output);
        byte[] signature = tx.getPayToScriptSignature(oracleKey, getHashType("ALL"), 0);
        tx.getInputs().get(0).setScript(redeemBetOraclePaymentScript(
                redeemOutput.redeemScript, signature, winnerHashPreImage, winnerPos));
        return new OracleAnswer(redeemOutput.redeemScript, tx);
    }
}
