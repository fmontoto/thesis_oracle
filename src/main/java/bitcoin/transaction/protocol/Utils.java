package bitcoin.transaction.protocol;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static bitcoin.Constants.getOpcodeAsArray;
import static bitcoin.key.Utils.r160SHA256Hash;

/**
 * Created by fmontoto on 3/30/17.
 */
public class Utils {
    static public int playerNoFromPrivateKey(BitcoinPublicKey[] playerPublicKeys,
                                             BitcoinPrivateKey playerPrivKey)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        for (int i = 0; i < playerPublicKeys.length; i++) {
            if (Arrays.equals(playerPublicKeys[i].getKey(), playerPrivKey.getPublicKey().getKey()))
                return i;
        }
        throw new InvalidParameterException("Unknown winnerKey.");
    }

    public static List<byte[]> formatPreimages(
            List<byte[]> playerAWinHashes, List<byte[]> playerBWinHashes,
            List<byte[]> winnerPreImages) throws NoSuchAlgorithmException {

        if(playerAWinHashes.size() != playerBWinHashes.size())
            throw new InvalidParameterException("Hashes lists must be the same size.");

        List<byte[]> ret = new LinkedList<>();
        byte[] winnerHash = r160SHA256Hash(winnerPreImages.get(winnerPreImages.size() - 1));
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

        for(byte[] preImage : winnerPreImages)
            ret.add(preImage);

        //if(Arrays.equals(winnerHash, winnerPlayerHashes.get(winnerPlayerHashes.size() - 1))) {
            ret.add(getOpcodeAsArray("OP_8"));
            System.out.println("Addes extra op1");
        //}

        Collections.reverse(ret);
        return ret;
    }
}
