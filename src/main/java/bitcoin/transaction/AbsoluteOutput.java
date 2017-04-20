package bitcoin.transaction;

import java.security.NoSuchAlgorithmException;

/**
 * Created by fmontoto on 28-11-16.
 */

public class AbsoluteOutput extends Output{
    private final int vout;
    private final String txId;

    private AbsoluteOutput() {
        super();
        txId = null;
        vout = 0;
    }

    public AbsoluteOutput(long value, byte[] script, int vout, String txId) {
        super(value, script);
        this.vout = vout;
        this.txId = txId;
    }

    public AbsoluteOutput(Output output, int vout, String txId) {
        this(output.getValue(), output.getScript(), vout, txId);
    }

    public AbsoluteOutput(Transaction tx, int output) throws NoSuchAlgorithmException {
        this(tx.getOutput(output), output, tx.txid());
    }

    public String getTxId() {
        return txId;
    }

    public int getVout() {
        return vout;
    }


}
