package bitcoin.transaction.redeem;

import bitcoin.key.BitcoinPrivateKey;
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
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemPlayerPrize;
import static bitcoin.transaction.builder.OutputBuilder.betPrizeResolutionRedeemScript;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.TransactionBuilder.*;
import static bitcoin.transaction.redeem.Utils.playerNoFromPrivateKey;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 3/25/17.
 */
public class WinnerPlayerPrize {

    private Bet bet;
    private List<byte[]> playerAWinHashes;
    private List<byte[]> playerBWinHashes;
    Transaction tx;

    private List<byte[]> redeemScripts;
    private long betTimeoutSeconds;

    private WinnerPlayerPrize(Bet bet, List<byte[]> playerAWinHashes,
                              List<byte[]> playerBWinHashes) {
        this.bet = bet;
        this.playerAWinHashes = playerAWinHashes;
        this.playerBWinHashes = playerBWinHashes;
        redeemScripts = new LinkedList<>();
    }

    private void setTransaction(Transaction tx) {
        this.tx = tx;
    }

    private void findRedeemScripts(byte[] expectedFirstHash, byte[] expectedSecondHash)
            throws NoSuchAlgorithmException, IOException {
        betTimeoutSeconds = bet.getRelativeBetResolutionSecs();
        byte[] redeemScript = new byte[0];
        for(int i = 0; i < 2000 && !Arrays.equals(expectedFirstHash, r160SHA256Hash(redeemScript));
            i++) {
            betTimeoutSeconds = bet.getRelativeBetResolutionSecs() + i * TIMEOUT_GRANULARITY;
            redeemScript = betPrizeResolutionRedeemScript(
                    playerAWinHashes, playerBWinHashes, Arrays.asList(bet.getPlayersPubKey()),
                    bet.getRequiredHashes(), betTimeoutSeconds, bet.getPlayersPubKey()[0]);
        }

        if(!Arrays.equals(expectedFirstHash, r160SHA256Hash(redeemScript)))
            throw new InvalidParameterException("Couldn't get the redeem script.");

        byte[] secondRedeemScript =  betPrizeResolutionRedeemScript(
                playerAWinHashes, playerBWinHashes, Arrays.asList(bet.getPlayersPubKey()),
                bet.getRequiredHashes(), betTimeoutSeconds, bet.getPlayersPubKey()[1]);

        if(!Arrays.equals(expectedSecondHash, r160SHA256Hash(secondRedeemScript))) {
            throw new InvalidParameterException("Second redeem script does not match");
        }

        redeemScripts.add(redeemScript);
        redeemScripts.add(secondRedeemScript);
    }

    public static WinnerPlayerPrize build(Bet bet, Transaction betTransaction, List<byte[]> winnerPreImages,
                                          List<byte[]> playerAWinsHashes, List<byte[]> playerBWinsHashes,
                                          BitcoinPrivateKey winnerKey, String wifOutputAddress)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException,
                   InvalidKeySpecException {

        if(bet.getRequiredHashes() > winnerPreImages.size())
            throw new InvalidParameterException("Not enough pre images.");

        int playerNo = playerNoFromPrivateKey(bet.getPlayersPubKey(), winnerKey);

        // We want to save as much space as possible in the txs, we only keep the required
        // pre images.
        winnerPreImages = new LinkedList<>(winnerPreImages);
        while(winnerPreImages.size() > bet.getRequiredHashes())
            winnerPreImages.remove(0);

        WinnerPlayerPrize winnerPlayerPrize = new WinnerPlayerPrize(bet, playerAWinsHashes,
                playerBWinsHashes);
        byte[] firstExpectedHash = hexToByteArray(
                betTransaction.getOutputs().get(0).getParsedScript().get(2));
        byte[] secondExpectedHash = hexToByteArray(
                betTransaction.getOutputs().get(1).getParsedScript().get(2));

        long available = betTransaction.getOutputs().get(0).getValue()
                + betTransaction.getOutputs().get(1).getValue();

        winnerPlayerPrize.findRedeemScripts(firstExpectedHash, secondExpectedHash);

        List<Input> inputs = new LinkedList<>();
        for(int i = 0; i < 2; i++) {
            inputs.add(new Input(new AbsoluteOutput(betTransaction, i),
                    winnerPlayerPrize.getRedeemScript(i)));
            inputs.get(i).setSequenceNo((int) readScriptNum(createSequenceNumber(TimeUnit.SECONDS,
                winnerPlayerPrize.getBetTimeoutSeconds())));
        }
        Output output = createPayToPubKeyOutput(available, wifOutputAddress);
        int txVersion = 2, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, inputs, Arrays.asList(output));
        List<byte[]> formattedPreImages = Utils.formatPreimages(playerAWinsHashes, playerBWinsHashes,
                winnerPreImages);
        List<byte[]> signatures = new LinkedList<>();
        for (int i = 0; i < 2; i++) {
            signatures.add(tx.getPayToScriptSignature(winnerKey, getHashType("ALL"), i));
            tx.getInputs().get(i).setScript(redeemPlayerPrize(winnerPlayerPrize.getRedeemScript(i),
                    signatures.get(i), winnerKey.getPublicKey(), playerNo, i, formattedPreImages));
        }

        setFeeFailIfNotEnough(tx, 0, bet.getFee());
        signatures.clear();
        for (int i = 0; i < 2; i++) {
            tx.setTempScriptSigForSigning(i, winnerPlayerPrize.getRedeemScript(i));
            signatures.add(tx.getPayToScriptSignature(winnerKey, getHashType("ALL"), i));
            tx.getInputs().get(i).setScript(redeemPlayerPrize(winnerPlayerPrize.getRedeemScript(i),
                    signatures.get(i), winnerKey.getPublicKey(), playerNo, i, formattedPreImages));
        }

        winnerPlayerPrize.setTransaction(tx);
        return winnerPlayerPrize;
    }

    public List<byte[]> getRedeemScripts() {
        return redeemScripts;
    }

    public byte[] getRedeemScript(int index) {
        return getRedeemScripts().get(index);
    }

    public long getBetTimeoutSeconds() {
        return betTimeoutSeconds;
    }

    public Transaction getTx() {
        return tx;
    }
}
