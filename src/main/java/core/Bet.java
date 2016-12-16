package core;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fmontoto on 16-12-16.
 */
public class Bet {
    private static final Charset utf8 = Charset.forName("UTF-8");

    private String text;
    private int min_oracles;
    private int max_oracles;
    private List<Oracle> oracles;
    private List<Oracle> backupOracles;

    public Bet() {

    }

    public Bet(String text, int min_oracles, int max_oracles, List<Oracle> oracles, List<Oracle> backupOracles) {
        this.text = text;
        this.min_oracles = min_oracles;
        this.max_oracles = max_oracles;
        this.oracles = new ArrayList<>(oracles);
        this.backupOracles = new ArrayList<>(backupOracles);
    }

    public Bet(String text, int num_oracles, List<Oracle>oracles, List<Oracle> backupOracles) {
        this(text, num_oracles, num_oracles, oracles, backupOracles);
    }

    public String getText(){
        return text;
    }

    byte[] getHash() throws NoSuchAlgorithmException {
        MessageDigest sha256dig = MessageDigest.getInstance("SHA-256");
        sha256dig.update(text.getBytes(utf8));
        return sha256dig.digest();
    }
}
