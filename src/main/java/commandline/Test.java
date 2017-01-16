package commandline;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPublicKey;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static bitcoin.transaction.TransactionBuilder.multisigOrTimeoutOutput;
import static core.Utils.hexToByteArray;
import static java.util.stream.Collectors.toList;

/**
 * Created by fmontoto on 06-01-17.
 */
public class Test {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

//        multisigOrTimeoutOutput(TimeUnit.MINUTES, 100, "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2", "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy");
//        static byte[] multisigOrTimeoutOutput(TimeUnit timeUnit,
//        long timeoutVal,
//        String neededAddressBeforeTimeout,
//        String alwaysNeededAddress) throws IOException, NoSuchAlgorithmException {
//        List<Integer> toCheck = IntStream.rangeClosed(10, 20).boxed().collect(toList());
//        System.out.println(toCheck.remove(0));
//        System.out.println(toCheck.remove(0));
//        System.out.println(toCheck.remove(toCheck.size() - 1));
//        toCheck.add(99);
//        System.out.println(toCheck.remove(0));
//        System.out.println(toCheck.remove(toCheck.size() - 1));
//        System.out.println("paso");
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