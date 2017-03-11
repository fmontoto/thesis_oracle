package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.ClientUtils;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import core.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getHashType;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.SignTest.getChangeAddress;
import static bitcoin.transaction.builder.InputBuilder.redeemMultisigOrSomeSignaturesTimeoutOutput;
import static bitcoin.transaction.builder.InputBuilder.redeemMultisigOutput;
import static bitcoin.transaction.builder.OutputBuilder.multisigScript;
import static bitcoin.transaction.builder.TransactionBuilder.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static spark.route.HttpMethod.get;

/**
 * Created by fmontoto on 16-01-17.
 */
public class ProtocolTxsTest {
    static final boolean testnet = true;
    static final int numOracles = 3;
    static final int numPlayers = 2;
    BitcoindClient bitcoindClient;

    String[] oraclesAddress;
    List<Oracle> oracles;
    List<BitcoinPublicKey> oraclePublicKeys;
    Channel channel;

    String[] playersAccount;
    String[] playersWIFAddress;
    BitcoinPrivateKey[] playersPrivateKey;

    final String playerAccountPrefix = "player";
    final String oracleAccountPrefix = "oracle";


    @BeforeClass
    static public void oneTimeSetUp() {

    }

    @Before
    public void setUp() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        bitcoindClient = new BitcoindClient(testnet);

        oraclesAddress = new String[numOracles];

        playersAccount = new String[numPlayers];
        playersWIFAddress = new String[numPlayers];
        playersPrivateKey = new BitcoinPrivateKey[numPlayers];

        for(int i = 0; i < numOracles; i++)
            oraclesAddress[i] = bitcoindClient.getAccountAddress(oracleAccountPrefix + i);

        for(int i = 0; i < numPlayers; i++) {
            playersAccount[i] = playerAccountPrefix + (i + 1);
            playersWIFAddress[i] = getChangeAddress(bitcoindClient, null, playersAccount[i]);
            playersPrivateKey[i] = BitcoinPrivateKey.fromWIF(
                    bitcoindClient.getPrivateKey(playersWIFAddress[i]));
        }

        oracles = new LinkedList<>();
        for(String oracleAddress : oraclesAddress)
            oracles.add(new Oracle(oracleAddress));
        oraclePublicKeys = new LinkedList<>();
        for(Oracle oracle : oracles)
            oraclePublicKeys.add(getOraclePublicKey(oracle));
        channel = new ZeroMQChannel("localhost:4324", "172.19.2.54:8876");
    }

    // Utils

    private List<AbsoluteOutput> getUnspetOutputs(String account) throws ParseTransactionException {
        return ClientUtils.getUnspentOutputs(bitcoindClient, account);
    }

    private AbsoluteOutput getUnspentOutput() throws ParseTransactionException {
        return ClientUtils.getUnspentOutputs(bitcoindClient, null).get(0);
    }

    private BitcoinPublicKey getOraclePublicKey(Oracle oracle) throws NoSuchAlgorithmException,
                                                                      IOException,
                                                                      InvalidKeySpecException {
        char[] privateKey = bitcoindClient.getPrivateKey(oracle.getAddress());
        return BitcoinPrivateKey.fromWIF(privateKey).getPublicKey();
    }

    @Test
    public void oracleInscriptionSuccessTest() throws Exception {
        AbsoluteOutput unspentOutput = getUnspentOutput();
        String srcAddress = unspentOutput.getPayAddress();
        String wifSrcAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcAddress), testnet);
        Transaction inscriptionTx = registerAsOracle(unspentOutput, bitcoindClient.isTestnet());
        inscriptionTx.sign(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(wifSrcAddress)));
        bitcoindClient.verifyTransaction(inscriptionTx);
    }


    public Transaction betPromiseFlow(int minOracles, int maxOracles, long firstPaymentAmount,
                                      long oraclePayment, long amount, long oracleInscription,
                                      long oraclePenalty, long fee, long timeoutSeconds,
                                      String description) throws Exception {
        BitcoinPublicKey[] playersPubKey = new BitcoinPublicKey[2];
        playersPubKey[0] = playersPrivateKey[0].getPublicKey();
        playersPubKey[1] = playersPrivateKey[1].getPublicKey();

        Bet.Amounts amounts = new Bet.Amounts(firstPaymentAmount, oraclePayment, amount,
                                              oracleInscription,
                oraclePenalty, fee);
        Bet agreedBet = new Bet(description, minOracles, maxOracles, oracles,
                                new LinkedList<Oracle>(), playersPubKey, amounts,
                TimeUnit.SECONDS, timeoutSeconds, channel);

        return betPromiseFlow(agreedBet);
    }

    public Transaction betPromiseFlow(Bet agreedBet)
            throws ParseTransactionException, IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, SignatureException, InvalidKeyException {
        /**
         * Sample of a flow where two players negotiate a bet.
         */

        // Player 1
        List<AbsoluteOutput> player1SrcOutputs = ClientUtils.getUnspentOutputs(bitcoindClient,
                                                                               playersAccount[0]);
        String player1WifChangeAddress = bitcoindClient.getAccountAddress(playersAccount[0]);

        Transaction player1BetPromise = betPromise(player1SrcOutputs, player1WifChangeAddress,
                                                   agreedBet, true);
        Transaction sharedTx = new Transaction(player1BetPromise);

        // Player 2
        List<AbsoluteOutput> player2SrcOutputs = ClientUtils.getUnspentOutputs(bitcoindClient,
                                                                               playersAccount[1]);
        String player2WifChangeAddress = bitcoindClient.getAccountAddress(playersAccount[1]);
        Transaction player2ExpectedBet = betPromise(player2SrcOutputs, player2WifChangeAddress,
                                                                      agreedBet, false);
        if(updateBetPromise(player2SrcOutputs, player2WifChangeAddress, agreedBet, false,
                                               sharedTx)) {
            // Remove change Output as it is changed at updateBetPromise
            player2ExpectedBet.getOutputs().remove(player2ExpectedBet.getOutputs().size() - 1);
            // Add the real change output.
            player2ExpectedBet.getOutputs().add(
                    sharedTx.getOutputs().get(sharedTx.getOutputs().size() - 1));
        }

        // Player 1
        checkBetPromiseAndSign(bitcoindClient, agreedBet, player1WifChangeAddress,
                               player1BetPromise, sharedTx, true);
        // Player 2
        checkBetPromiseAndSign(bitcoindClient, agreedBet, player2WifChangeAddress,
                               player2ExpectedBet, sharedTx, false); // Player 1 already signed, do
                                                                     // not allow modifications.
        return sharedTx;
    }

    public Transaction oracleBetInscription(
            List<byte[]> expectedAnswersHash, int numOracle, Bet bet, Transaction betPromiseTx,
            BitcoinPrivateKey oraclePrivKey) throws NoSuchAlgorithmException, IOException,
                                                    InvalidKeySpecException,
                                                    ParseTransactionException, SignatureException,
                                                    InvalidKeyException {

        List<BitcoinPrivateKey> oracleSrcKeys = new LinkedList<>();
        String oracleWifChangeAddress = bitcoindClient.getAccountAddress(
                oracleAccountPrefix + numOracle);

        List<AbsoluteOutput> oracleSrcOutputs = ClientUtils.getUnspentOutputs(bitcoindClient,
                oracleAccountPrefix + numOracle);

        for(AbsoluteOutput srcOutput : oracleSrcOutputs) {
            String wifAddr = BitcoinPublicKey.txAddressToWIF(srcOutput.getPayAddress(),
                    bitcoindClient.isTestnet());
            oracleSrcKeys.add(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(wifAddr)));
        }

        return oracleInscription(
                oracleSrcOutputs, oracleSrcKeys, oraclePrivKey.getPublicKey(),
                oracleWifChangeAddress, expectedAnswersHash, bet, betPromiseTx,
                bet.getRelativeBetResolutionSecs());
    }

    //@Test
    public void oracleBetInscriptionFlow(Bet bet) throws Exception {
        final int totalOracles = 5;
        Transaction betPromiseTransaction = null;
        List<List<byte[]>> expectedAnswers = new LinkedList<>();
        List<Transaction> oracleTransactions = new LinkedList<>();
        List<Transaction> sharedTransactions = new LinkedList<>();

        // Oracles
        for(int numOracle = 0; numOracle < totalOracles; numOracle++) {
            BitcoinPrivateKey oraclePrivKey = BitcoinPrivateKey.fromWIF(
                    bitcoindClient.getPrivateKey(oraclesAddress[numOracle]));
            List<byte[]> expectedAnswersHash = new LinkedList<>();
            List<byte[]> expectedAnswersOracle = new LinkedList<>();
            for(int i = 0; i < bet.getPlayersPubKey().length; i++) {
                expectedAnswersOracle.add(
                        new String("player" + i + "wins, " + numOracle).getBytes(Constants.charset));
                expectedAnswers.add(expectedAnswersOracle);
            }

            for(byte[] answer : expectedAnswersOracle)
                expectedAnswersHash.add(r160SHA256Hash(answer));

            Transaction tx = oracleBetInscription(
                    expectedAnswersHash, numOracle, bet, betPromiseTransaction, oraclePrivKey);
            ProtocolTxUtils.OracleData oracleData = new ProtocolTxUtils.OracleData(
                    oraclePrivKey.getPublicKey(), expectedAnswersHash);
            tx.getInputs().get(tx.getInputs().size() - 1).setScript(oracleData.serialize());
            oracleTransactions.add(tx);
            sharedTransactions.add(new Transaction(tx));
        }

        // Player1
        for(Transaction tx : sharedTransactions) {
            Input toSignInput = tx.getInputs().get(tx.getInputs().size() - 1);
            ProtocolTxUtils.OracleData oracleData = ProtocolTxUtils.OracleData.fromSerialized(
                    toSignInput.getScript());
        }

        for(int playerNum = 0; playerNum < bet.getPlayersPubKey().length; playerNum++) {
            // At this point the players still don't have the oracle pubKey...
            for(int oracleNum = 0; oracleNum < totalOracles; oracleNum++) {
//                getOracleNumber(betPromiseTransaction, oracleWifAddress, bet);

            }
        }

        // This key must match the address in the betPromise

//        expectedAnswers.add("Player A wins".getBytes(Constants.charset));
//        expectedAnswers.add("Player B wins".getBytes(Constants.charset));
//        for(byte[] b : expectedAnswers) {
//            expectedAnswersHash.add(r160SHA256Hash(b));
//        }


//        Transaction sharedTx = new Transaction(tx);

        // Player1
//        BitcoinPrivateKey player1PrivateKey = bet.getPlayersPubKey()
        //Transaction betPromise = betPromiseFlow();
        throw new NotImplementedException();
    }

    @Test
    public void completeFlowTest() throws Exception {
        // This method goes through all the transactions in the protocol.

        // Everyone willing to be chose as an oracle must send the inscription transaction to the
        // blockchain.
        List<Transaction> inscriptionTxs = new LinkedList<>();
        for(Oracle oracle : oracles) {
            String account = bitcoindClient.getAccount(oracle.getAddress());
            List<AbsoluteOutput> availableOutput = ClientUtils.getOutputsAvailableAtLeast(
                    bitcoindClient, account, 100 /* fee */);
            Transaction transaction = registerAsOracle(availableOutput.get(0),
                                                       oracle.getAddress(),
                                                       100 /* fee */,
                                                       1 /* tx version */,
                                                       0 /* locktime */);
            char[] privKey = bitcoindClient.getPrivateKey(BitcoinPublicKey.txAddressToWIF(
                            availableOutput.get(0).getPayAddress(), bitcoindClient.isTestnet()));

            transaction.sign(BitcoinPrivateKey.fromWIF(privKey));
            bitcoindClient.verifyTransaction(transaction);
            inscriptionTxs.add(transaction);
        }

        // The required initial state for the players is to know the other party public key or
        // address. With this, an authenticated channel can be stablished and the parameters of
        // the bet can be stablished.
        // See the communication package to see an example of this interactive process

        //Parameters:
        int minOracles, maxOracles;
        long firstPaymentAmount, oraclePayment, amount, oracleInscription, oraclePenalty, fee,
                timeoutSeconds;
        firstPaymentAmount = oraclePayment = oracleInscription = oraclePenalty = fee = 4;
        timeoutSeconds = 1000;
        amount = 10000;
        minOracles = maxOracles = oracles.size();
        String description = "Bet's description... really short actually";
        //

        BitcoinPublicKey[] playersPubKey = new BitcoinPublicKey[2];
        playersPubKey[0] = playersPrivateKey[0].getPublicKey();
        playersPubKey[1] = playersPrivateKey[1].getPublicKey();
        Bet.Amounts amounts = new Bet.Amounts(firstPaymentAmount, oraclePayment, amount, oracleInscription,
                oraclePenalty, fee);
        Bet agreedBet = new Bet(description, minOracles, maxOracles, oracles, new LinkedList<Oracle>(), playersPubKey, amounts,
                TimeUnit.SECONDS, timeoutSeconds, channel);

        Transaction betPromise = betPromiseFlow(agreedBet);

        // betPromise is the first transaction of the bet protocol to go into the blockchain, it
        // reflects the intention and commitment from both players. It has the hashed description
        // of the bet and the list of the chosen oracles. When a mentioned oracle sees the
        // transaction it should get the description of the bet as described on it, after getting
        // it, make sure is the one supposed to be by hashing and comparing the result with the
        // hash in the betPromise.

        bitcoindClient.verifyTransaction(betPromise);

        // After the oracles see the betPromise transaction they sign on (or not) to participate.
        List<ParticipatingOracle> participatingOracles = new LinkedList<>();
        List<Transaction> participatingOraclesTxs = new LinkedList<>();

        for(Oracle oracle : oracles) {
            ParticipatingOracle participatingOracle = ParticipatingOracle.participate(oracle,
                                                                                      betPromise);
            participatingOracles.add(participatingOracle);
            Transaction oracleInscriptionTx = participatingOracle.generateInscriptionTransaction(
                    bitcoindClient, agreedBet,
                    agreedBet.getRelativeBetResolutionSecs() /* timeout */);
            participatingOraclesTxs.add(oracleInscriptionTx);
            // When this transaction takes some time from the promise bet, the timeout should
            // be adjusted, as it is a relative timeout since the transaction was posted in the
            // blockhain.
        }


        // Then the oracles send the transaction to the players, so they can sign it.
        // So far the player don't need to know the public key of the oracle, just the address.
        // But now they need it in order to check the redeem script the oracles set to the
        // transaction.

        // Player 1 puts its signature in the transaction and send it to player 2
        for(Transaction tx : participatingOraclesTxs) {
            //TODO check the tx is what is supposed to be. (the oracle did the right thing)
            byte[] promiseBetRedeemScript = multisigScript(playersPubKey, playersPubKey.length);
            int input_to_sign = tx.getInputs().size() - 1;
            tx.setTempScriptSigForSigning(input_to_sign, promiseBetRedeemScript);
            tx.getInputs().get(input_to_sign).setScript(
                    tx.getPayToScriptSignature(playersPrivateKey[0], getHashType("ALL"),
                                               input_to_sign));
            // Now the transaction contains player 1's signature in the input script.

        }

        // Now player 2 takes the other's player signature, generate their and complete the tx. By
        // using the two required signatures.
        for(Transaction tx :participatingOraclesTxs) {
            //TODO check the tx is what is supposed to be. (the oracle did the right thing)
            byte[] promiseBetRedeemScript = multisigScript(playersPubKey, playersPubKey.length);
            int input_to_sign = tx.getInputs().size() - 1;
            byte[] player1Signature = tx.getInputs().get(input_to_sign).getScript();
            tx.setTempScriptSigForSigning(input_to_sign, promiseBetRedeemScript);
            byte[] player2Signature = tx.getPayToScriptSignature(
                    playersPrivateKey[1], getHashType("ALL"), input_to_sign);
            List<byte[]> signatures = Arrays.asList(player1Signature, player2Signature);
            tx.getInputs().get(input_to_sign).setScript(
                    redeemMultisigOutput(promiseBetRedeemScript, signatures));
        }

        // Oracle Inscription transactions are now ready to go to the blockchain.
        for(int i = 0; i < participatingOraclesTxs.size(); i++) {
            Transaction tx = participatingOraclesTxs.get(i);
            // At bet promise, there are three outputs before the oracle's outputs.
            int input_bet_promise = 3 + i;
            byte[] promiseBetRedeemScript = multisigScript(playersPubKey, playersPubKey.length);
            PayToScriptAbsoluteOutput betPromiseAbsoluteOutput = new PayToScriptAbsoluteOutput(
                    betPromise, input_bet_promise, promiseBetRedeemScript);
            bitcoindClient.verifyTransaction(tx, betPromiseAbsoluteOutput);
        }
        //Transaction transaction = betPromiseFlow();
    }


}