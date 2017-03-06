package bitcoin;

import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.ParseTransactionException;

import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;

/**
 * Created by fmontoto on 3/4/17.
 */


public class ClientUtils {
    /**
     * Return the unspent outputs of the account.
     * @param bitcoindClient
     * @param account
     * @return
     * @throws ParseTransactionException
     */
    public static List<AbsoluteOutput> getUnspentOutputs(
            BitcoindClient bitcoindClient, String account) throws ParseTransactionException {
        List<AbsoluteOutput> unspentOutputs;
        if(account == null || account.isEmpty())
            unspentOutputs = bitcoindClient.getUnspent();
        else
            unspentOutputs = bitcoindClient.getUnspent(account);
        List<AbsoluteOutput> ret = unspentOutputs.stream().filter(AbsoluteOutput::isPayToKey).collect(toList());
        assertFalse(ret.isEmpty());
        return ret;
    }

    public static List<AbsoluteOutput> getOutputsAvailableAtLeast(
            BitcoindClient client, String account, long atLeast) throws ParseTransactionException {
        List<AbsoluteOutput> unspentOutputs = getUnspentOutputs(client, account);
        unspentOutputs.get(0).getValue();
        List<AbsoluteOutput> ret = new LinkedList<>();
        for(AbsoluteOutput ao : unspentOutputs) {
            if(ao.getValue() >= atLeast)
                ret.add(ao);
        }
        assertFalse(ret.isEmpty());
        return ret;
    }
}
