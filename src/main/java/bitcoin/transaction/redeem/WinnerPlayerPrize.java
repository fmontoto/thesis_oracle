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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemPlayerPrize;
import static bitcoin.transaction.builder.OutputBuilder.betPrizeResolutionRedeemScript;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static bitcoin.transaction.builder.TransactionBuilder.buildTx;
import static bitcoin.transaction.builder.TransactionBuilder.createSequenceNumber;
import static bitcoin.transaction.redeem.Utils.playerNoFromPrivateKey;
import static core.Utils.byteArrayToHex;
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
        // TODO check fee...
        long fee = 100;
        long prize = available - fee;

        winnerPlayerPrize.findRedeemScripts(firstExpectedHash, secondExpectedHash);

        List<Input> inputs = new LinkedList<>();
        for(int i = 0; i < 2; i++) {
            inputs.add(new Input(new AbsoluteOutput(betTransaction, i),
                    winnerPlayerPrize.getRedeemScript(i)));
            inputs.get(i).setSequenceNo((int) readScriptNum(createSequenceNumber(TimeUnit.SECONDS,
                winnerPlayerPrize.getBetTimeoutSeconds())));
        }
        Output output = createPayToPubKeyOutput(prize, wifOutputAddress);
        int txVersion = 2, txLockTime = 0;
        Transaction tx = buildTx(txVersion, txLockTime, inputs, Arrays.asList(output));
        byte[] signature0 = tx.getPayToScriptSignature(winnerKey, getHashType("ALL"), 0);
        byte[] signature1 = tx.getPayToScriptSignature(winnerKey, getHashType("ALL"), 1);


        List<byte[]> formattedPreImages = Utils.formatPreimages(playerAWinsHashes, playerBWinsHashes,
                                                          winnerPreImages);

        tx.getInputs().get(0).setScript(redeemPlayerPrize(winnerPlayerPrize.getRedeemScript(0),
                signature0, winnerKey.getPublicKey(), playerNo, 0, formattedPreImages));
        tx.getInputs().get(1).setScript(redeemPlayerPrize(winnerPlayerPrize.getRedeemScript(1),
                signature1, winnerKey.getPublicKey(), playerNo, 1, formattedPreImages));

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
