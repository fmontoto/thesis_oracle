package commandline;

/**
 * Created by fmontoto on 06-01-17.
 */
public class Test {
    public static void main(String[] args) {
        new B().bar();
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