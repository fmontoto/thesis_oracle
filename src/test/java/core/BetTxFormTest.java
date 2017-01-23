package core;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static core.Utils.hexToByteArray;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 05-01-17.
 */
public class BetTxFormTest {

    List<Oracle> oracles;
    byte[] hash;
    Channel channel;


    @Before
    public void setUp() throws Exception {
        oracles = new ArrayList<>();
        oracles.add(new Oracle("n42AbZApbDz859w7vfSS4bM38zd7MXBbJV"));
        oracles.add(new Oracle("mppjAUikJwPGFk2MR9y4cgjCdSGyFMc8ev"));
        oracles.add(new Oracle("n4Ke2X6TSdLP9Q4VpVzW77D43Tc18sfpk1"));
        hash =  hexToByteArray("f8d7dc153df4d2d62b74cb13718dd15f6ee15947");
        channel = new ZeroMQChannel("localhost:3432", "34.43.21.2");
    }

    @Test
    public void serialize() throws Exception {
        List<String> expectedAddresses = oracles.stream().map(o -> o.getAddress()).collect(Collectors.toList());
        BetTxForm bet = new BetTxForm(oracles, hash, channel);
        BetTxForm unserializedBet = BetTxForm.fromSerialized(bet.serialize());
        List<String> oracleAddresses = unserializedBet.getOracles().stream().map(
                o -> o.getAddress()).collect(Collectors.toList());
        assertArrayEquals(expectedAddresses.toArray(), oracleAddresses.toArray());
        assertArrayEquals(hash, unserializedBet.getDescriptionHash());
        assertEquals(channel, unserializedBet.getChannel());
    }

    @Test
    public void testIsOraclePresent() throws Exception {
        BetTxForm bet = new BetTxForm(oracles, hash, channel);
        Oracle oracleNotInTheBet = new Oracle("mqutBAufXX4qYBnLVrtoNUZXdAJicykkwC");
        Set<String> compareOracles = new HashSet<>();
        compareOracles.add(oracles.get(1).getAddress());
        Set<String> presentOracles = bet.getPresentOracles(compareOracles);
        Set<String> expected = new HashSet<>();
        expected.add(oracles.get(1).getAddress());
        assertEquals(expected, bet.getPresentOracles(compareOracles));

        compareOracles.add(oracles.get(2).getAddress());
        expected.add(oracles.get(2).getAddress());

        assertEquals(expected, bet.getPresentOracles(compareOracles));
        compareOracles.add(oracleNotInTheBet.getAddress());
        assertEquals(expected, bet.getPresentOracles(compareOracles));
    }


}