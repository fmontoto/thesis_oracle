package bitcoin;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Created by fmontoto on 05-01-17.
 */
public class Block {
    private List<String> txs;
    private String hash;
    private String nextHash;
    private int height;
    private ZonedDateTime date;

    public Block(String hash, List<String> txs, String nextHash, int height, Date date) {
        this.txs = txs;
        this.hash = hash;
        this.nextHash = nextHash;
        this.height = height;
        this.date = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
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
