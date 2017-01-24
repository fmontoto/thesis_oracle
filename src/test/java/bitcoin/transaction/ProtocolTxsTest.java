package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.builder.TransactionBuilder;
import core.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 16-01-17.
 */
public class ProtocolTxsTest {
    static final boolean testnet = true;
    static BitcoindClient bitcoindClient;

    static String[] oraclesAddress;
    List<Oracle> oracles;
    Channel channel;

    final String player1Account = "player1";
    final String player2Account = "player2";
    static final String oracleAccountPrefix = "oracle";


    @BeforeClass
    static public void oneTimeSetUp() {
        bitcoindClient = new BitcoindClient(testnet);
        oraclesAddress = new String[3];
        oraclesAddress[0] = bitcoindClient.getAccountAddress(oracleAccountPrefix + 0);
        oraclesAddress[1] = bitcoindClient.getAccountAddress(oracleAccountPrefix + 1);
        oraclesAddress[2] = bitcoindClient.getAccountAddress(oracleAccountPrefix + 2);
    }
    @Before
    public void setUp() {
        oracles = new LinkedList<>();
        for(String oracleAddress : oraclesAddress)
            oracles.add(new Oracle(oracleAddress));
        channel = new ZeroMQChannel("localhost:4324", "172.19.2.54:8876");
    }

    private List<AbsoluteOutput> getUnspentOutputs(String account) throws ParseTransactionException {
        List<AbsoluteOutput> unspentOutputs;
        if(account == null || account.isEmpty())
            unspentOutputs = bitcoindClient.getUnspent();
        else
            unspentOutputs = bitcoindClient.getUnspent(account);
        AbsoluteOutput srcOutput = null;
        List<AbsoluteOutput> ret = unspentOutputs.stream().filter(AbsoluteOutput::isPayToKey).collect(toList());
        assertFalse(ret.isEmpty());
        return unspentOutputs;
    }

    private AbsoluteOutput getUnspentOutput() throws ParseTransactionException {
        return getUnspentOutputs(null).get(0);
    }

    @Test
    public void oracleInscriptionSuccessTest() throws Exception {
        AbsoluteOutput unspentOutput = getUnspentOutput();
        String srcAddress = unspentOutput.getPayAddress();
        String wifSrcAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(srcAddress), testnet);
        Transaction inscriptionTx = TransactionBuilder.registerAsOracle(unspentOutput, bitcoindClient.isTestnet());
        inscriptionTx.sign(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(wifSrcAddress)));
        throw new NotImplementedException();
    }

    @Test
    public void playersBetPromiseTest() throws Exception {
        int minOracles, maxOracles;
        long firstPaymentAmount, oraclePayment, amount, oracleInscription, oraclePenalty, fee, timeoutSeconds;
        firstPaymentAmount = oraclePayment = oracleInscription = oraclePenalty = fee = 4;
        timeoutSeconds = 1000;
        amount = 10000;
        minOracles = maxOracles = oracles.size();
        String description = "Bet's description... really short actually";
        Bet.Amounts amounts = new Bet.Amounts(firstPaymentAmount, oraclePayment, amount, oracleInscription,
                oraclePenalty, fee);
        BitcoinPublicKey[] playersPubKey = new BitcoinPublicKey[2];
        playersPubKey[0] = new BitcoinPublicKey("030881eb43770203716888f131eaba4d9b35446d60cebafebcb9908ffdb050b006", true, true);
        playersPubKey[1] = new BitcoinPublicKey("0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6",
                false, true);
        Bet b1 = new Bet(description, minOracles, maxOracles, oracles, new LinkedList<Oracle>(), playersPubKey, amounts,
                TimeUnit.SECONDS, timeoutSeconds, channel);
        List<AbsoluteOutput> srcOutputs = getUnspentOutputs(player1Account);
        String wifChangeAddress = bitcoindClient.getAccountAddress(player1Account);

        List<BitcoinPrivateKey> privateKeys = new LinkedList<>();
        for(AbsoluteOutput ao : srcOutputs) {
            String WIF = BitcoinPublicKey.txAddressToWIF(hexToByteArray(ao.getPayAddress()), true);
            privateKeys.add(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(WIF)));
        }

        Transaction betPromise = TransactionBuilder.betPromise(srcOutputs, wifChangeAddress, b1, true);
        for(int i = 0; i < privateKeys.size(); i++)
            betPromise.sign(privateKeys.get(i), i);
        System.out.println("./bitcoin-cli -testnet signrawtransaction " + byteArrayToHex(betPromise.serialize()) + " \"[]\" \"[]\"");
    }

    public Transaction betPromiseFlow() throws Exception {
        // Parameters
        int minOracles, maxOracles;
        long firstPaymentAmount, oraclePayment, amount, oracleInscription, oraclePenalty, fee, timeoutSeconds;
        firstPaymentAmount = oraclePayment = oracleInscription = oraclePenalty = fee = 4;
        timeoutSeconds = 1000;
        amount = 10000;
        minOracles = maxOracles = oracles.size();
        String description = "Bet's description... really short actually";
        BitcoinPublicKey[] playersPubKey = new BitcoinPublicKey[2];
        playersPubKey[0] = new BitcoinPublicKey("030881eb43770203716888f131eaba4d9b35446d60cebafebcb9908ffdb050b006", true, true);
        playersPubKey[1] = new BitcoinPublicKey("0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6",
                false, true);

        Bet.Amounts amounts = new Bet.Amounts(firstPaymentAmount, oraclePayment, amount, oracleInscription,
                oraclePenalty, fee);
        Bet agreedBet = new Bet(description, minOracles, maxOracles, oracles, new LinkedList<Oracle>(), playersPubKey, amounts,
                                TimeUnit.SECONDS, timeoutSeconds, channel);

        // Player 1
        List<AbsoluteOutput> player1SrcOutputs = getUnspentOutputs(player1Account);
        String player1WifChangeAddress = bitcoindClient.getAccountAddress(player1Account);

        Transaction player1BetPromise = TransactionBuilder.betPromise(player1SrcOutputs, player1WifChangeAddress, agreedBet, true);
        Transaction sharedTx = new Transaction(player1BetPromise);

        // Player 2
        List<AbsoluteOutput> player2SrcOutputs = getUnspentOutputs(player2Account);
        String player2WifChangeAddress = bitcoindClient.getAccountAddress(player2Account);
        Transaction player2ExpectedBet = TransactionBuilder.betPromise(player2SrcOutputs, player2WifChangeAddress,
                                                                      agreedBet, false);
        if(TransactionBuilder.updateBetPromise(player2SrcOutputs, player2WifChangeAddress, agreedBet, false,
                                               sharedTx)) {
            // Remove change Output as it is changed at updateBetPromise
            player2ExpectedBet.getOutputs().remove(player2ExpectedBet.getOutputs().size() - 1);
            // Add the real change output.
            player2ExpectedBet.getOutputs().add(sharedTx.getOutputs().get(sharedTx.getOutputs().size() - 1));
        }

        // Player 1
        TransactionBuilder.checkBetPromiseAndSign(bitcoindClient, agreedBet, player1WifChangeAddress, player1BetPromise,
                                                  sharedTx, true);
        // Player 2
        TransactionBuilder.checkBetPromiseAndSign(bitcoindClient, agreedBet, player2WifChangeAddress, player2ExpectedBet,
                                                  sharedTx, false); // Player 1 already signed, do not
                                                                                   // allow modifications.
        return sharedTx;
    }

    @Test
    public void betPromiseFlowTest() throws Exception {
        System.out.println("./bitcoin-cli -testnet signrawtransaction " + betPromiseFlow().hexlify() + " \"[]\" \"[]\"");
    }

    public Transaction oracleBetInscriptionFlow(int numOracle, Bet bet) throws Exception {
        long oracleInscription = bet.getOracleInscription();
        long oracleUnduePayment = bet.getOraclePayment();
        long oraclePenalty = bet.getOraclePayment();
        Transaction betPromiseTransaction = betPromiseFlow();

        // Oracle
        // This key must match the address in the betPromise
        BitcoinPrivateKey oraclePrivKey = BitcoinPrivateKey.fromWIF(
                bitcoindClient.getPrivateKey(oraclesAddress[numOracle]));
        List<AbsoluteOutput> oracleSrcOutputs = getUnspentOutputs(oracleAccountPrefix + numOracle);
        String oracle2WifChangeAddress = bitcoindClient.getAccountAddress(oracleAccountPrefix + numOracle);

        TransactionBuilder.oracleInscription(oracleSrcOutputs, oraclePrivKey, oracle2WifChangeAddress,
                                             bet, betPromiseTransaction);

        Transaction betPromise = betPromiseFlow();
        throw new NotImplementedException();



    }


}