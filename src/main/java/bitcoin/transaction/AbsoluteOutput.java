package bitcoin.transaction;

/**
 * Created by fmontoto on 28-11-16.
 */

public class AbsoluteOutput extends Output{
    private final int vout;
    private String txId;

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

    public String getTxId() {
        return txId;
    }

    public int getVout() {
        return vout;
    }

}
