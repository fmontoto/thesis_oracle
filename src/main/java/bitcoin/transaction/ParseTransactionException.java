package bitcoin.transaction;

/**
 * Created by fmontoto on 13-01-17.
 */
public class ParseTransactionException extends Exception{
    private String txId;
    private ParseTransactionException() {

    }

    public ParseTransactionException(String message, String txId) {
        super(message);
        this.txId = txId;
    }

    public String getTxId() {
        return this.txId;
    }
}
