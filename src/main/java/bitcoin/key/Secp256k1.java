package bitcoin.key;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.logging.Logger;


/**
 * Created by fmontoto on 09-11-16.
 */
public class Secp256k1 {
    private static final Logger LOGGER = Logger.getLogger( Secp256k1.class.getName() );
    // The curve is defined at https://en.bitcoin.it/wiki/Secp256k1
    static public final BigInteger p =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    static public final BigInteger a = BigInteger.valueOf(0);
    static public final BigInteger b = BigInteger.valueOf(7);
    static private final BigInteger x =
            new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    static private final BigInteger y =
            new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    static private final BigInteger n =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    static private final int h = 1;
    static private final ECField ec_field = new ECFieldFp(p);
    static private final EllipticCurve elliptic_curve = new EllipticCurve(ec_field, a, b);
    static public final ECParameterSpec spec = new ECParameterSpec(elliptic_curve, new ECPoint(x, y), n, h);


    static public KeyPair generatePair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("EC");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.throwing("Secp256k1", "generatePair", e);
            throw e;
        }
    }
}


