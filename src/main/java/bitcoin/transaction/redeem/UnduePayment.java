package bitcoin.transaction.redeem;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.*;
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
import static bitcoin.transaction.Utils.parseScript;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemBetOraclePaymentScript;
import static bitcoin.transaction.builder.InputBuilder.redeemUnduePayment;
import static bitcoin.transaction.builder.OutputBuilder.betOraclePaymentScript;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.OutputBuilder.undueChargePaymentScript;
import static bitcoin.transaction.builder.TransactionBuilder.*;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 3/22/17.
 */
public class UnduePayment {

    static class RedeemOutput {
        long timeoutSeconds;
        byte[] redeemScript;

        public RedeemOutput(
                byte[] expectedHash, BitcoinPublicKey[] playersPublicKey,
                BitcoinPublicKey oraclePublicKey, byte[] oracleAWinHash, byte[] oracleBWinHash,
                List<byte[]> aWinHashes, List<byte[]> bWinHashes, int requiredHashes,
                long undueTimeoutSeconds) throws NoSuchAlgorithmException, IOException {

            redeemScript = undueChargePaymentScript(playersPublicKey, oraclePublicKey,
                    oracleAWinHash, oracleBWinHash, aWinHashes, bWinHashes, requiredHashes,
                    undueTimeoutSeconds);
            redeemScript = new byte[1];
            int i = 0;
            for(; i < 2000 && !Arrays.equals(r160SHA256Hash(redeemScript), expectedHash); ++i) {
                for(int j = 0; j < 2000 && !Arrays.equals(r160SHA256Hash(redeemScript), expectedHash); ++j) {
                    timeoutSeconds = undueTimeoutSeconds + i * TIMEOUT_GRANULARITY;
                    redeemScript = undueChargePaymentScript(playersPublicKey, oraclePublicKey,
                            oracleAWinHash, oracleBWinHash, aWinHashes, bWinHashes, requiredHashes,
                            undueTimeoutSeconds);
                }
            }
            if(!Arrays.equals(r160SHA256Hash(redeemScript), expectedHash))
                throw new InvalidParameterException();
        }
    }

    byte[] redeemScript;
    Transaction answer;
    int outputRedeemed;

    private UnduePayment(byte[] redeemScript, Transaction answer, int outputRedeemed) {
        this.redeemScript = redeemScript;
        this.answer = answer;
        this.outputRedeemed = outputRedeemed;
    }

    public byte[] getRedeemScript() {
        return redeemScript;
    }

    public Transaction getAnswer() {
        return answer;
    }

    public int getOutputRedeemed() {
        return outputRedeemed;
    }

    static public UnduePayment build(
            Transaction betTransaction, Bet bet, int oraclePosition,
            BitcoinPrivateKey oracleKey, String dstWIFAddress, List<byte[]> playerAWinHash,
            List<byte[]> playerBWinHash)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException,
            SignatureException, InvalidKeyException {

        int toRedeemOutputPos = 3 + 2 * oraclePosition;
        long available = betTransaction.getOutputs().get(oraclePosition).getValue();
        long fee = 100; // TODO calculate it...


        byte[] expectedRedeeemHash = hexToByteArray(betTransaction.getOutputs()
                .get(toRedeemOutputPos).getParsedScript().get(2));
        RedeemOutput redeemOutput = new RedeemOutput(expectedRedeeemHash,bet.getPlayersPubKey(),
                oracleKey.getPublicKey(), playerAWinHash.get(oraclePosition),
                playerBWinHash.get(oraclePosition), playerAWinHash, playerBWinHash,
                bet.getRequiredHashes(), bet.getRelativeUndueChargeTimeoutSeconds());

        AbsoluteOutput ao = new AbsoluteOutput(betTransaction, toRedeemOutputPos);
        Input input = new Input(ao, redeemOutput.redeemScript);
        input.setSequenceNo((int) readScriptNum(createSequenceNumber(TimeUnit.SECONDS,
                redeemOutput.timeoutSeconds)));
        Output output = createPayToPubKeyOutput(available, dstWIFAddress);
        int txVersion = 2, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, input, output);
        byte[] signature = tx.getPayToScriptSignature(oracleKey, getHashType("ALL"), 0);
        tx.getInputs().get(0).setScript(redeemUnduePayment(oracleKey.getPublicKey(), signature,
                redeemOutput.redeemScript));

        return new UnduePayment(redeemOutput.redeemScript, tx, toRedeemOutputPos);
    }
}
