package bitcoin.transaction.protocol;

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
import static bitcoin.Constants.getOpcodeAsArray;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.parseScript;
import static bitcoin.transaction.Utils.readScriptNum;
import static bitcoin.transaction.builder.InputBuilder.redeemMultiSigOutput;
import static bitcoin.transaction.builder.InputBuilder.redeemPlayerPrize;
import static bitcoin.transaction.builder.OutputBuilder.betPrizeResolutionRedeemScript;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static bitcoin.transaction.builder.TransactionBuilder.buildTx;
import static bitcoin.transaction.builder.TransactionBuilder.createSequenceNumber;
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

    private static List<byte[]> formatPreimages(
            List<byte[]> playerAWinHashes, List<byte[]> playerBWinHashes,
            List<byte[]> preImages) throws NoSuchAlgorithmException {

        if(playerAWinHashes.size() != playerBWinHashes.size())
            throw new InvalidParameterException("Hashes lists must be the same size.");

        List<byte[]> ret = new LinkedList<>();
        byte[] winnerHash = r160SHA256Hash(preImages.get(preImages.size() - 1));
        int posAtAHashes = -1, posAtBHashes = -1;
        for(int i = 0; i < playerAWinHashes.size(); i++) {
            if(Arrays.equals(winnerHash, playerAWinHashes.get(i))) {
                posAtAHashes = i;
                break;
            }
        }
        for(int i = 0; i < playerBWinHashes.size(); i++) {
            if(Arrays.equals(winnerHash, playerBWinHashes.get(i))) {
                posAtBHashes = i;
                break;
            }
        }

        if(posAtAHashes == -1 && posAtBHashes == -1)
            throw new InvalidParameterException(
                    "The preimages does not match any of the expected hashes.");

        List<byte []> winnerPlayerHashes = posAtAHashes != -1 ? playerAWinHashes : playerBWinHashes;

        for(byte[] preImage : preImages)
            ret.add(preImage);

        if(!Arrays.equals(winnerHash, winnerPlayerHashes.get(winnerPlayerHashes.size() - 1)))
            ret.add(getOpcodeAsArray("OP_1"));

        Collections.reverse(ret);
        return ret;
    }

    public static WinnerPlayerPrize build(Bet bet, Transaction betTransaction, List<byte[]> winnerPreImages,
                                          List<byte[]> playerAWinsHashes, List<byte[]> playerBWinsHashes,
                                          BitcoinPrivateKey winnerKey, String wifOutputAddress)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException,
                   InvalidKeySpecException {

        if(bet.getRequiredHashes() > winnerPreImages.size())
            throw new InvalidParameterException("Not enough pre images.");

        int playerNo = -1;
        if(Arrays.equals(bet.getPlayersPubKey()[0].getKey(), winnerKey.getPublicKey().getKey())) {
            playerNo = 0;
        }
        else if(Arrays.equals(bet.getPlayersPubKey()[1].getKey(),
                              winnerKey.getPublicKey().getKey())) {
            playerNo = 1;
        }
        else {
            throw new InvalidParameterException("Unknown winnerKey.");
        }

        // We want to save as much space as possible in the txs, we only keep the required
        // pre images.
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


        List<byte[]> formattedPreImages = formatPreimages(playerAWinsHashes, playerBWinsHashes,
                                                          winnerPreImages);

        tx.getInputs().get(0).setScript(redeemPlayerPrize(winnerPlayerPrize.getRedeemScript(0),
                signature0, playerNo, formattedPreImages));
        tx.getInputs().get(1).setScript(redeemPlayerPrize(winnerPlayerPrize.getRedeemScript(1),
                signature1, playerNo, formattedPreImages));

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
