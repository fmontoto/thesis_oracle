import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.openSSL.OpenSSLDlogECF2m;
import org.bouncycastle.util.BigIntegers;

import org.bouncycastle.util.BigIntegers;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by fmontoto on 30-08-16.
 */
public class Oracle {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello World!");
        DlogGroup dlog = new OpenSSLDlogECF2m("K-233");
        SecureRandom random = new SecureRandom();

        // get the group generator and order
        GroupElement g = dlog.getGenerator();
        BigInteger q = dlog.getOrder();
        BigInteger qMinusOne = q.subtract(BigInteger.ONE);

        // create a random exponent r
        BigInteger r = BigIntegers.createRandomInRange(BigInteger.ZERO, qMinusOne, random);

        // exponentiate g in r to receive a new group element
        GroupElement g1 = dlog.exponentiate(g, r);
        // create a random group element
        GroupElement h = dlog.createRandomElement();
        // multiply elements
        GroupElement gMult = dlog.multiplyGroupElements(g1, h);
    }

}
