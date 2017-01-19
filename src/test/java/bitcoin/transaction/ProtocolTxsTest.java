package bitcoin.transaction;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.transaction.builder.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    private List<AbsoluteOutput> getUnspentOutputs() throws ParseTransactionException {
        List<AbsoluteOutput> unspentOutputs = bitcoindClient.getUnspent();
        AbsoluteOutput srcOutput = null;
        String changeAddr = null;
        for(AbsoluteOutput ao: unspentOutputs)
            if(ao.isPayToKey())
                srcOutput = ao;
        assertNotNull("Couldn't find unspent outputs.", srcOutput);
        return unspentOutputs;
    }

    private AbsoluteOutput getUnspentOutput() throws ParseTransactionException {
        return getUnspentOutputs().get(0);
    }

    @Test
    public void oracleInscriptionSuccessTest() throws Exception {
        AbsoluteOutput unspentOutput = getUnspentOutput();
        String srcAddress = unspentOutput.getPayAddress();
        Transaction inscriptionTx = TransactionBuilder.inscribeAsOracle(unspentOutput, bitcoindClient.isTestnet());
//        inscriptionTx.sign(BitcoinPrivateKey.fromWIF(bitcoindClient.getPrivateKey(srcAddress)));
        throw new NotImplementedException();
    }

    @Test
    public void playersBetPromiseTest() throws Exception {

    }


}