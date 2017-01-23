package core;

import bitcoin.key.BitcoinPublicKey;
import org.bouncycastle.jcajce.provider.symmetric.DES;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 19-01-17.
 */
public class BetTest {

    Bet b1;
    String description;
    int minOracles;
    int maxOracles;
    List<Oracle> oracles, backUpOracles;
    BitcoinPublicKey[] playersPubKey;
    long firstPaymentAmount, oraclePayment, amount, oracleInscription, oraclePenalty, fee, timeoutSeconds;
    String[] oraclesAddress;
    Channel channel;

    @Before
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        description = "This is the bet description, if <X> players A wins. Otherwise, player B wins.";
        minOracles = 3;
        maxOracles = minOracles;
        oraclesAddress = new String[3];
        oraclesAddress[0] = "mrV3e1QTX2ZqkNcTreaqYbTKLD7ASFXkVA";
        oraclesAddress[1] = "mtR9jJM7XMSQwd1cAhLHdkfuxJ2DbLeVNX";
        oraclesAddress[2] = "mqutBAufXX4qYBnLVrtoNUZXdAJicykkwC";
        oracles = new LinkedList<>();
        backUpOracles = new LinkedList<>();
        backUpOracles.add(new Oracle("mqbzmbVK4NQjwcWfeU7PJesP4wwBQNxm3Z"));
        for(String oracleAddress : oraclesAddress)
            oracles.add(new Oracle(oracleAddress));

        playersPubKey = new BitcoinPublicKey[2];
        playersPubKey[0] = new BitcoinPublicKey("030881eb43770203716888f131eaba4d9b35446d60cebafebcb9908ffdb050b006", true, true);
        playersPubKey[1] = new BitcoinPublicKey("0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6",
                false, true);

        channel = new ZeroMQChannel("localhost:3424", "10.17.5.12:3324");

        firstPaymentAmount = 2;
        oraclePayment = 3;
        amount = 4;
        oracleInscription = 5;
        oraclePenalty  = 6;
        fee = 7;
        timeoutSeconds = 1000;


        Bet.Amounts amounts = new Bet.Amounts(firstPaymentAmount, oraclePayment, amount, oracleInscription,
                oraclePenalty, fee);
        b1 = new Bet(description, minOracles, maxOracles, oracles, backUpOracles, playersPubKey, amounts,
                TimeUnit.SECONDS, timeoutSeconds, channel);
    }

    @Test
    public void serializationTest() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        assertEquals(b1, Bet.fromSerialized(b1.serialize()));
    }
}