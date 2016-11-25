package bitcoin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 25-11-16.
 */
public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName() );

    public static byte[] doubleSHA256(byte[] val) throws NoSuchAlgorithmException {
        MessageDigest dig = null;
        try {
            dig = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.throwing("Utils", "doubleSHA256", e);
            throw e;
        }
        byte [] first_digest = dig.digest(val);
        dig.reset();
        return dig.digest(first_digest);
    }
}
