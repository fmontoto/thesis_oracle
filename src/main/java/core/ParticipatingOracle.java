package core;

import bitcoin.BitcoindClient;
import bitcoin.ClientUtils;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.ParseTransactionException;
import bitcoin.transaction.Transaction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.List;

import static bitcoin.ClientUtils.getUnspentOutputs;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.builder.TransactionBuilder.oracleInscription;
import static bitcoin.transaction.builder.TransactionBuilder.registerAsOracle;

/**
 * Created by fmontoto on 3/4/17.
 */
public class ParticipatingOracle extends Oracle {
    static final int MIN_SECRET_BYTES = 19; // Inclusive
    static final int MAX_SECRET_BYTES = 25; // Exclusive

    private Transaction betPromise;
    private byte[] playerAWins, playerBWins, playerAWinsHash, playerBWinsHash;

    private ParticipatingOracle(Oracle oracle, Transaction betPromise) throws NoSuchAlgorithmException {
        super(oracle.getAddress());
        this.betPromise = betPromise;

        // Generate secrets.
        SecureRandom random = new SecureRandom();
        int size = MIN_SECRET_BYTES + random.nextInt(MAX_SECRET_BYTES - MIN_SECRET_BYTES);
        playerAWins = new byte[size];
        random.nextBytes(playerAWins);
        size = MIN_SECRET_BYTES + random.nextInt(MAX_SECRET_BYTES - MIN_SECRET_BYTES);
        playerBWins = new byte[size];
        random.nextBytes(playerBWins);

        this.playerAWinsHash = r160SHA256Hash(playerAWins);
        this.playerBWinsHash = r160SHA256Hash(playerBWins);
    }

    static public ParticipatingOracle participate(Oracle oracle, Transaction betPromise)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        return new ParticipatingOracle(oracle, betPromise);


        //BitcoinPrivateKey bitcoinPrivateKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(oracle.getAddress()));
        //String account = client.getAccount(oracle.getAddress());

        //bitcoinPrivateKey.getPublicKey().getKey();
    }

    public Transaction generateInscriptionTransaction(BitcoindClient client, Bet bet,
                                                      long timeoutSeconds)
            throws ParseTransactionException, IOException, NoSuchAlgorithmException,
                   InvalidKeySpecException, SignatureException, InvalidKeyException {


        String account = client.getAccount(getAddress());
        List<AbsoluteOutput> unspentOutputs = getUnspentOutputs(client, account);
        List<BitcoinPrivateKey> outputKeys = new LinkedList<BitcoinPrivateKey>();
        for(AbsoluteOutput ao : unspentOutputs) {
            outputKeys.add(BitcoinPrivateKey.fromWIF(client.getPrivateKey(
                    BitcoinPublicKey.txAddressToWIF(ao.getPayAddress(), client.isTestnet()))));
        }

        BitcoinPublicKey pubKey = BitcoinPrivateKey.fromWIF(
                client.getPrivateKey(getAddress())).getPublicKey();
        List<byte[]> expectedAnswersHashes = new LinkedList<>();
        expectedAnswersHashes.add(playerAWinsHash);
        expectedAnswersHashes.add(playerBWinsHash);
        return oracleInscription(unspentOutputs, outputKeys, pubKey, pubKey.toWIF(),
                                 expectedAnswersHashes, bet, betPromise, timeoutSeconds);
    }
}

