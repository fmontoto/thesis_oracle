package commandline;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPublicKey;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 06-01-17.
 */
public class Test {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        new B().bar();
        BitcoindClient client = new BitcoindClient(false);
        byte[] pubKey = hexToByteArray("042f90074d7a5bf30c72cf3a8dfd1381bdbd30407010e878f3a11269d5f74a58788505cdca22ea6eab7cfb40dc0e07aba200424ab0d79122a653ad0c7ec9896bdf");
        BitcoinPublicKey bpc = new BitcoinPublicKey(pubKey, false);
        System.out.println(bpc.toWIF());
    }

}

class A{
    public void foo() {
        System.out.println("A.foo");
    }

    public void bar() {
        System.out.println("A.bar");
        foo();
    }
}

class B extends A {
    public void foo() {
        System.out.println("B.foo");
    }
}