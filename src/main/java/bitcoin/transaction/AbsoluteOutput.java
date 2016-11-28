package bitcoin.transaction;

/**
 * Created by fmontoto on 28-11-16.
 */

public class AbsoluteOutput extends Output{
    private String txId;

    private AbsoluteOutput() {
        super();
        txId = null;
    }

    public AbsoluteOutput(long value, byte[] script, String txId) {
        super(value, script);
        this.txId = txId;
    }

    public String getTxId() {
        return txId;
    }
}
