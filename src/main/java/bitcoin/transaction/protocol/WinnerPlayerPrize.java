package bitcoin.transaction.protocol;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.transaction.Transaction;
import core.Bet;
import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.builder.OutputBuilder.betPrizeResolutionRedeemScript;
import static bitcoin.transaction.builder.TransactionBuilder.TIMEOUT_GRANULARITY;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 3/25/17.
 */
public class WinnerPlayerPrize {

    private Bet bet;
    private List<byte[]> playerAWinHashes;
    private List<byte[]> playerBWinHashes;

    private List<byte[]> redeemScripts;

    private WinnerPlayerPrize(Bet bet, List<byte[]> playerAWinHashes,
                              List<byte[]> playerBWinHashes) {
        this.bet = bet;
        this.playerAWinHashes = playerAWinHashes;
        this.playerBWinHashes = playerBWinHashes;
        redeemScripts = new LinkedList<>();
    }

    private void findRedeemScripts(byte[] expectedFirstHash, byte[] expectedSecondHash)
            throws NoSuchAlgorithmException, IOException {
        long betTimeout = bet.getRelativeBetResolutionSecs();
        byte[] redeemScript = new byte[0];
        for(int i = 0; i < 2000 && Arrays.equals(expectedFirstHash, r160SHA256Hash(redeemScript));
            i++) {
            betTimeout = bet.getRelativeBetResolutionSecs() + i * TIMEOUT_GRANULARITY;

            redeemScript = betPrizeResolutionRedeemScript(
                    playerAWinHashes, playerBWinHashes, Arrays.asList(bet.getPlayersPubKey()),
                    bet.getRequiredHashes(), betTimeout, bet.getPlayersPubKey()[0]);
        }

        if(Arrays.equals(expectedFirstHash, r160SHA256Hash(redeemScript)))
            throw new InvalidParameterException("Couldn't get the redeem script.");

        byte[] secondRedeemScript =  betPrizeResolutionRedeemScript(
                playerAWinHashes, playerBWinHashes, Arrays.asList(bet.getPlayersPubKey()),
                bet.getRequiredHashes(), betTimeout, bet.getPlayersPubKey()[1]);

        /*
        betPrizeResolution(
                playerAWinHashes, playerBWinHashes, playerPubKeys, bet.getRequiredHashes(),
                timeoutSeconds, playerPubKeys.get(0), amount/2);
        betPrizeResolution(
                playerAWinHashes, playerBWinHashes, playerPubKeys, bet.getRequiredHashes(),
                timeoutSeconds, playerPubKeys.get(1), amount/2);


        byte[] redeemScript = betPrizeResolutionRedeemScript(playerAWinHashes, playerBWinHashes,
                playerPubKeys, requiredHashes, timeoutSeconds, onTimeout);
                */

    }

    public Transaction build(Bet bet, Transaction betTransaction, List<byte[]> winnerPreImages,
                             List<byte[]> playerAWinsHashes, List<byte[]> playerBWinsHashes,
                             BitcoinPrivateKey winnerKey, String wifOutputAddress) {
        WinnerPlayerPrize winnerPlayerPrize = new WinnerPlayerPrize(bet, playerAWinsHashes,
                                                                    playerBWinsHashes);
        byte[] firstExpectedHash = hexToByteArray(
                betTransaction.getOutputs().get(0).getParsedScript().get(2));
        byte[] secondExpectedHash = hexToByteArray(
                betTransaction.getOutputs().get(1).getParsedScript().get(2));

        //winnerPlayerPrize.findRedeemScripts(firstExpectedHash, secondExpectedHash);

        throw new NotImplementedException();


    }
}
