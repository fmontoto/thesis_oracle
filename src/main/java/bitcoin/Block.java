package bitcoin;

import java.util.List;

/**
 * Created by fmontoto on 05-01-17.
 */
public class Block {
    private List<String> txs;
    private String hash;
    private String nextHash;
    private int height;

    public Block(String hash, List<String> txs, String nextHash, int height) {
        this.txs = txs;
        this.hash = hash;
        this.nextHash = nextHash;
        this.height = height;
    }

    public List<String> getTxs() {
        return txs;
    }

    public String getHash() {
        return hash;
    }

    public String getNextHash() {
        return nextHash;
    }

    public int getHeight() {
        return height;
    }
}
