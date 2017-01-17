package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import org.junit.Before;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by fmontoto on 16-01-17.
 */
public class ProtocolTxsTest {
    boolean testnet = true;
    BitcoindClient bitcoindClient;
    @Before
    public void setUp() {
        bitcoindClient = new BitcoindClient(testnet);
    }

    public List<AbsoluteOutput> getUnspentOutputs() throws ParseTransactionException {
        List<AbsoluteOutput> unspentOutputs = bitcoindClient.getUnspent();
        AbsoluteOutput srcOutput = null;
        String changeAddr = null;
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        return unspentOutputs;
    }

    public AbsoluteOutput getUnspentOutput() throws ParseTransactionException {
        return getUnspentOutputs().get(0);
    }

    @Test
    public void oracleInscriptionSuccessTest() throws Exception {
        AbsoluteOutput unspentOutput = getUnspentOutput();
        String srcAddress = unspentOutput.getPayAddress();
        Transaction inscriptionTx = TransactionBuilder.inscribeAsOracle(unspentOutput, bitcoindClient.isTestnet());
        inscriptionTx.sign(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(srcAddress)));
        throw new NotImplementedException();
    }

    @Test
    public void playersBetPromiseTest() throws Exception {

    }


}