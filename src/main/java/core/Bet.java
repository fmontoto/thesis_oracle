package core;

import bitcoin.BitcoindClient;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by fmontoto on 16-12-16.
 */
public class Bet {
    private static final Charset utf8 = Charset.forName("UTF-8");

    private String description;
    private int min_oracles;
    private int max_oracles;
    private List<Oracle> oracles;
    private List<Oracle> backupOracles;

    private Bet() {

    }

    public Bet(String description, int min_oracles, int max_oracles, List<Oracle> oracles, List<Oracle> backupOracles) {
        this.description = description;
        this.min_oracles = min_oracles;
        this.max_oracles = max_oracles;
        this.oracles = new ArrayList<>(oracles);
        this.backupOracles = new ArrayList<>(backupOracles);
    }

    public Bet(String description, int num_oracles, List<Oracle>oracles, List<Oracle> backupOracles) {
        this(description, num_oracles, num_oracles, oracles, backupOracles);
    }

    public String getDescription(){
        return description;
    }

    public byte[] getHash() throws NoSuchAlgorithmException {
        MessageDigest sha256dig = MessageDigest.getInstance("SHA-256");
        sha256dig.update(description.getBytes(utf8));
        return sha256dig.digest();
    }

    static public void listParameters(PrintStream out) {
        out.println("Description [str]: Description of the bet, this must" +
                " be clear enough as it's the only information the oracles" +
                " get.");
        out.println("Min oracles [num]: Minimum number of oracles needed to" +
                "perform the bet");
        out.println("Max oracles [num]: Maximum number of oracles needed to" +
                " perform the bet");
        out.println("Oracles [List<str>]: List of addresses of the oracles");
        out.println("BackUp Oracles [List<str>]: List of addresses of" +
                " the oracles to replace oracles of the main list if there" +
                " are some not replying. This is an ordered list, and" +
                " first listed should be used first");
    }

    static public Bet buildBet(InputStream in, PrintStream out) {
        String aux;
        Scanner scanner = new Scanner(in);

        out.println("Insert bet description.");
        String description = scanner.nextLine();

        out.println("Insert minimum number of oracles.");
        int min_oracles = scanner.nextInt();

        out.println("Insert max num of oracles");
        int max_oracles = scanner.nextInt();

        List<String> oracles = new ArrayList<>();
        out.println("Insert oracle's addresses");
        aux = scanner.nextLine();
        while(!aux.isEmpty()) {
            oracles.add(aux);
            aux = scanner.nextLine();
        }

        List<String> backupOracles = new ArrayList<>();
        out.println("Insert oracle's backup addresses");
        aux = scanner.nextLine();
        while(!aux.isEmpty()) {
            backupOracles.add(aux);
            aux = scanner.nextLine();
        }

        int needed_oracles = max_oracles + max_oracles / 2;
        if(oracles.size() > max_oracles)
            throw new InvalidParameterException("Too many oracles specified");

        if(oracles.size() + backupOracles.size() < needed_oracles) {
            out.println("Not enough oracles specified.");
            List<Oracle> randomOracles = getRandomOracles(
                    scanner, out,needed_oracles - oracles.size() - backupOracles.size());
        }

        throw new NotImplementedException();
    }

    private static List<Oracle> getRandomOracles(Scanner sc, PrintStream out, int num_oracles) {
        out.println("There are " + num_oracles + "oracles to be chosen randomly.");
        out.println("You need to provide a block interval to chose oracles from there.");
        out.println("Select the interval's first block height.");
        int first_block = sc.nextInt();
        out.println("Enter the last block's height of the interval.");
        int last_block = sc.nextInt();

        BitcoindClient bitcoindClient = new BitcoindClient(false);
        bitcoindClient.getOracleList(first_block, last_block);
        throw new NotImplementedException();

    }
}
