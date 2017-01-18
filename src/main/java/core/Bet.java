package core;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPublicKey;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 16-12-16.
 */
public class Bet {
    private static final Charset utf8 = Charset.forName("UTF-8");

    private String description;
    private int minOracles;
    private int maxOracles;
    private List<Oracle> oracles;
    private List<Oracle> backupOracles;
    private BitcoinPublicKey[] playersPubKey;
    private long firstPaymentAmount;
    private long oraclePayment;
    private long amount;
    private long oracleInscription;
    private long oraclePenalty;
    private long fee;
    private TimeUnit timeoutUnit;
    private long timeoutVal;

    public Bet(String description, int minOracles, int max_oracles, List<Oracle> oracles,
               List<Oracle> backupOracles, BitcoinPublicKey[] playersPubKey, long firstPaymentAmount,
               long oraclePayment, long amount, long oracleInscription, long oraclePenalty, long fee, TimeUnit timeoutUnit, long timeoutVal) {
        this.description = description;
        this.minOracles = minOracles;
        this.maxOracles = max_oracles;
        this.oracles = new ArrayList<>(oracles);
        this.backupOracles = new ArrayList<>(backupOracles);
        this.playersPubKey = playersPubKey;
        this.firstPaymentAmount = firstPaymentAmount;
        this.oraclePayment = oraclePayment;
        this.amount = amount;
        this.oracleInscription = oracleInscription;
        this.oraclePenalty = oraclePenalty;
        this.fee = fee;
        this.timeoutUnit = timeoutUnit;
        this.timeoutVal = timeoutVal;
        if(playersPubKey.length != 2)
            throw new InvalidParameterException("Only two players accepted");
        if(maxOracles != this.oracles.size())
            throw new InvalidParameterException("oracles list must have same amount of oraclas as maxOracles specify.");
    }

    public Bet(String description, int num_oracles, List<Oracle>oracles, List<Oracle> backupOracles,
               BitcoinPublicKey[] playersPubKey, long firstPaymentAmount, long oraclePayment, long amount, long fee,
               long oracleInscription, long oraclePenalty, TimeUnit timeoutUnit, long timeoutVal) {
        this(description, num_oracles, num_oracles, oracles, backupOracles, playersPubKey, firstPaymentAmount,
             oraclePayment, amount, fee, oracleInscription, oraclePenalty, timeoutUnit, timeoutVal);
    }

    public byte[] serialize() {
        byte[] descriptionBytes = description.getBytes(utf8);
        throw new NotImplementedException();

    }

    public byte[] getHash() throws NoSuchAlgorithmException, IOException {
        StringBuilder sb = new StringBuilder();
        oracles.forEach(oracle -> sb.append(oracle.getAddress()));
        backupOracles.forEach(oracle -> sb.append(oracle.getAddress()));
        String oracles = sb.toString();

        return r160SHA256Hash(
                mergeArrays(description.getBytes(utf8),
                            serializeVarInt(minOracles),
                            serializeVarInt(maxOracles),
                            oracles.getBytes(utf8),
                            playersPubKey[0].getKey(),
                            playersPubKey[1].getKey(),
                            serializeVarInt(firstPaymentAmount),
                            serializeVarInt(oraclePayment),
                            serializeVarInt(amount),
                            serializeVarInt(oracleInscription),
                            serializeVarInt(oraclePenalty),
                            serializeVarInt(fee),


                            sb.toString().getBytes(utf8))); // TODO add oracles and backup oracles somehow...
    }

    public String getDescription(){
        return description;
    }

    public byte[] getDescriptionHash() throws NoSuchAlgorithmException {
        MessageDigest sha256dig = MessageDigest.getInstance("SHA-256");
        sha256dig.update(description.getBytes(utf8));
        return sha256dig.digest();
    }

    public byte[] getWireRepresentation() throws NoSuchAlgorithmException, IOException {
        throw new NotImplementedException();
//        return new BetTxForm(oracles, byteArrayToHex(getHash()), (byte) 0x00, "NO SE").serialize();
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

    public BitcoinPublicKey[] getPlayersPubKey() {
        return playersPubKey;
    }

    public List<Oracle> getOracles() {
        return oracles;
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

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public int getMaxOracles() {
        return maxOracles;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public long getTimeoutVal() {
        return timeoutVal;
    }

    public void setTimeoutVal(long timeoutVal) {
        this.timeoutVal = timeoutVal;
    }

    public long getFirstPaymentAmount() {
        return firstPaymentAmount;
    }

    public long getOraclePayment() {
        return oraclePayment;
    }
}

class BetTxForm {
    private List<Oracle> oracles;
    private String descriptionHash;
    private byte channelType;
    private String channel;

    public BetTxForm(List<Oracle> oracles, String descriptionHash, byte channelType, String channel) {
        this.oracles = oracles;
        this.descriptionHash = descriptionHash;
        this.channel = channel;
        this.channelType = channelType;
        if(descriptionHash.length() != 40)
            throw new InvalidParameterException("Description hash must be 20 bytes long");
    }

    public byte[] serialize() throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(serializeVarInt(oracles.size()));
        for(Oracle o : oracles) {
            byte[] addr = o.getTxFormAddress();
            byteStream.write(serializeVarInt(addr.length));
            byteStream.write(addr);
        }

        byteStream.write(hexToByteArray(descriptionHash));
        byteStream.write(channelType);
        byteStream.write(hexToByteArray(channel));
        return byteStream.toByteArray();
    }

    static public BetTxForm fromSerialized(byte[] bet, boolean testnet) throws IOException, NoSuchAlgorithmException {
        try {
            int offset = 0, addrSize;
            long oraclesNum = readVarInt(bet);
            offset += varIntByteSize(oraclesNum);
            List<Oracle> oracles = new ArrayList<>();
            for (int i = 0; i < oraclesNum; i++) {
                addrSize = (int) readVarInt(bet, offset);
                offset += varIntByteSize(addrSize);
                byte[] addrTxForm = Arrays.copyOfRange(bet, offset, offset + addrSize);
                offset += addrSize;

                oracles.add(new Oracle(addrTxForm, testnet));
            }
            // 20 bytes is the size of the Hash
            String descriptionHash = byteArrayToHex(bet, offset, offset + 20);
            offset += 20;
            byte channelType = bet[offset++];
            String channel = byteArrayToHex(bet, offset, bet.length);
            return new BetTxForm(oracles, descriptionHash, channelType, channel);
        }
        catch (IndexOutOfBoundsException e){
            throw new InvalidParameterException("Provided bytes are not a valid bet");
        }
    }

    public List<Oracle> getOracles() {
        return oracles;
    }

    public String getDescriptionHash() {
        return descriptionHash;
    }

    public byte getChannelType() {
        return channelType;
    }

    public String getChannel() {
        return channel;
    }

}
