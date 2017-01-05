package core;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 05-01-17.
 */
public class BetTxFormTest {

    List<Oracle> oracles;
    String hash;
    byte channelType;
    String channel;


    @Before
    public void setUp() throws Exception {
        oracles = new ArrayList<>();
        oracles.add(new Oracle("n42AbZApbDz859w7vfSS4bM38zd7MXBbJV"));
        oracles.add(new Oracle("mppjAUikJwPGFk2MR9y4cgjCdSGyFMc8ev"));
        oracles.add(new Oracle("n4Ke2X6TSdLP9Q4VpVzW77D43Tc18sfpk1"));
        hash =  "f8d7dc153df4d2d62b74cb13718dd15f6ee15947";
        channel = "a39040a5c44a449f438f4116e5d09428";
        channelType = (byte) 0xf1;
    }

    @Test
    public void serialize() throws Exception {
        List<String> expectedAddresses = oracles.stream().map(o -> o.getAddress()).collect(Collectors.toList());
        BetTxForm bet = new BetTxForm(oracles, hash, channelType, channel);
        BetTxForm unserializedBet = BetTxForm.fromSerialized(bet.serialize(), true);
        List<String> oracleAddresses = unserializedBet.getOracles().stream().map(
                o -> o.getAddress()).collect(Collectors.toList());
        assertArrayEquals(expectedAddresses.toArray(), oracleAddresses.toArray());
        assertTrue(hash.equalsIgnoreCase(unserializedBet.getDescriptionHash()));
        assertEquals(channelType, unserializedBet.getChannelType());
        assertTrue(channel.equalsIgnoreCase(unserializedBet.getChannel()));
    }
}