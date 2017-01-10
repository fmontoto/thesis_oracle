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
        System.loadLibrary("MiraclJavaInterface");
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