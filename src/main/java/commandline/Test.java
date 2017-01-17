package commandline;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static bitcoin.transaction.Utils.serializeUint16;
import static bitcoin.transaction.Utils.serializeUint32;
import static core.Utils.byteArrayToHex;

/**
 * Created by fmontoto on 06-01-17.
 */
public class Test {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        long uno = 81920;
        long dos = 147456;

        byte[] unoSerialized = serializeUint32(uno);
        byte[] dosSerialized = serializeUint32(dos);

        System.out.println(byteArrayToHex(serializeUint32(uno)));
        System.out.println(byteArrayToHex(serializeUint32(dos)));
        System.out.println(byteArrayToHex(serializeUint16((int)(uno & 0xffff))));
        System.out.println(byteArrayToHex(serializeUint16((int)(dos & 0xffff))));
        System.out.println(uno & 0x0000ffff);
        System.out.println(uno & (1 << 22));
        System.out.println(dos & (1 << 22));
        System.out.println(byteArrayToHex(serializeUint32(1 << 22)));

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