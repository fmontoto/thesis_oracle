package transaction;

import java.util.ArrayList;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Transaction {

    private int version;
    private ArrayList<Input> inputs;
    private ArrayList<Output> outputs;
    private int lockTime;

    public Transaction() {
        version = 1;
        lockTime = 0xFFFFFFFF;
        inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
    }

    public Transaction(int version, int lockTime){
        this();
        this.version = version;
        this.lockTime = lockTime;
    }
}
