package core;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPublicKey;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
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
    private List<BitcoinPublicKey> participantOracles;
    private BitcoinPublicKey[] playersPubKey;
    private long firstPaymentAmount;
    private long oraclePayment;
    private long amount;
    private long oracleInscription;
    private long oraclePenalty;
    private long fee;
    private long timeoutSeconds;


    public Bet(String description, int minOracles, int max_oracles, List<Oracle> oracles,
               List<Oracle> backupOracles, BitcoinPublicKey[] playersPubKey, long firstPaymentAmount,
               long oraclePayment, long amount, long oracleInscription, long oraclePenalty, long fee,
               TimeUnit timeoutUnit, long timeoutVal) {

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
        timeoutSeconds = timeoutUnit.toSeconds(timeoutVal);
        participantOracles = new LinkedList<>();

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

    public void addParticipantOracle(BitcoinPublicKey participantOracle) {
        participantOracles.add(participantOracle);
    }

    public byte[] serialize() throws IOException, NoSuchAlgorithmException {
        byte[] descriptionBytes = description.getBytes(utf8);
        ByteArrayOutputStream oracleOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream backUporacleOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream publicKeysOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream participantOraclesOutputStream = new ByteArrayOutputStream();

        for(Oracle oracle : oracles)
            oracleOutputStream.write(oracle.serialize());
        for(Oracle oracle : backupOracles)
            backUporacleOutputStream.write(oracle.serialize());
        for(BitcoinPublicKey publicKey : playersPubKey)
            publicKeysOutputStream.write(publicKey.serialize());
        for(BitcoinPublicKey publicKey : participantOracles)
            participantOraclesOutputStream.write(publicKey.serialize());


        return mergeArrays( serializeVarInt(descriptionBytes.length)
                          , descriptionBytes
                          , serializeVarInt(minOracles)
                          , serializeVarInt(maxOracles)
                          , serializeVarInt(oracles.size())
                          , oracleOutputStream.toByteArray()
                          , serializeVarInt(backupOracles.size())
                          , backUporacleOutputStream.toByteArray()
                          , serializeVarInt(playersPubKey.length)
                          , publicKeysOutputStream.toByteArray()
                          , serializeVarInt(participantOracles.size())
                          , participantOraclesOutputStream.toByteArray()
                          , serializeVarInt(firstPaymentAmount)
                          , serializeVarInt(oraclePayment)
                          , serializeVarInt(amount)
                          , serializeVarInt(oracleInscription)
                          , serializeVarInt(oraclePenalty)
                          , serializeVarInt(fee)
                          , serializeVarInt(timeoutSeconds)
        );
    }

    static public Bet fromSerialized(byte[] buffer, int offset) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        List<Oracle> oracles = new LinkedList<>(), backupOracles = new LinkedList<>();
        int descriptionSize = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(descriptionSize);
        String description = new String(Arrays.copyOfRange(buffer, offset, offset + descriptionSize), utf8);
        offset += descriptionSize;

        int minOracles = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(minOracles);

        int maxOracles = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(maxOracles);

        int oraclesQuantity = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(oraclesQuantity);
        while(oraclesQuantity-- != 0) {
            Oracle oracle = Oracle.loadFromSerialized(buffer, offset);
            oracles.add(oracle);
            offset += oracle.serializationSize();
        }

        int backUpOraclesQuantity = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(backUpOraclesQuantity);
        while(backUpOraclesQuantity-- != 0) {
            Oracle oracle = Oracle.loadFromSerialized(buffer, offset);
            backupOracles.add(oracle);
            offset += oracle.serializationSize();
        }

        int playersQuantity = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(playersQuantity);

        BitcoinPublicKey[] playersKey = new BitcoinPublicKey[playersQuantity];
        for(int i = 0; i < playersQuantity; i++) {
            playersKey[i] = BitcoinPublicKey.fromSerialized(buffer, offset);
            offset += playersKey[i].serializationSize();
        }

        int participantOraclesQuantity = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(participantOraclesQuantity);


        BitcoinPublicKey[] participantOracles = new BitcoinPublicKey[participantOraclesQuantity];
        for(int i = 0; i < participantOraclesQuantity; i++) {
            playersKey[i] = BitcoinPublicKey.fromSerialized(buffer, offset);
            offset += playersKey[i].serializationSize();
        }

        long firstPaymentAmount = readVarInt(buffer, offset);
        offset += varIntByteSize(firstPaymentAmount);

        long oraclePayment = readVarInt(buffer, offset);
        offset += varIntByteSize(oraclePayment);

        long amount = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(oraclePayment);

        long oracleInscription = readVarInt(buffer, offset);
        offset += varIntByteSize(oracleInscription);

        long oraclePenalty = readVarInt(buffer, offset);
        offset += varIntByteSize(oraclePenalty);

        long fee = readVarInt(buffer, offset);
        offset += varIntByteSize(fee);

        long timeoutSeconds = readVarInt(buffer, offset);

        Bet bet = new Bet(description, minOracles, maxOracles, oracles, backupOracles, playersKey, firstPaymentAmount,
                          oraclePayment, amount, oracleInscription, oraclePenalty, fee, TimeUnit.SECONDS,
                          timeoutSeconds);
        for(BitcoinPublicKey bitcoinPublicKey : participantOracles)
            bet.addParticipantOracle(bitcoinPublicKey);

        return bet;
    }

    static public Bet fromSerialized(byte[] buffer) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        return fromSerialized(buffer, 0);
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

    public long getFirstPaymentAmount() {
        return firstPaymentAmount;
    }

    public long getOraclePayment() {
        return oraclePayment;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bet bet = (Bet) o;

        if (minOracles != bet.minOracles) return false;
        if (maxOracles != bet.maxOracles) return false;
        if (firstPaymentAmount != bet.firstPaymentAmount) return false;
        if (oraclePayment != bet.oraclePayment) return false;
        if (amount != bet.amount) return false;
        if (oracleInscription != bet.oracleInscription) return false;
        if (oraclePenalty != bet.oraclePenalty) return false;
        if (fee != bet.fee) return false;
        if (timeoutSeconds != bet.timeoutSeconds) return false;
        if (!description.equals(bet.description)) return false;
        if (!oracles.equals(bet.oracles)) return false;
        if (!backupOracles.equals(bet.backupOracles)) return false;
        if (!participantOracles.equals(bet.participantOracles)) return false;
        return Arrays.equals(playersPubKey, bet.playersPubKey);
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + minOracles;
        result = 31 * result + maxOracles;
        result = 31 * result + oracles.hashCode();
        result = 31 * result + backupOracles.hashCode();
        result = 31 * result + participantOracles.hashCode();
        result = 31 * result + Arrays.hashCode(playersPubKey);
        result = 31 * result + (int) (firstPaymentAmount ^ (firstPaymentAmount >>> 32));
        result = 31 * result + (int) (oraclePayment ^ (oraclePayment >>> 32));
        result = 31 * result + (int) (amount ^ (amount >>> 32));
        result = 31 * result + (int) (oracleInscription ^ (oracleInscription >>> 32));
        result = 31 * result + (int) (oraclePenalty ^ (oraclePenalty >>> 32));
        result = 31 * result + (int) (fee ^ (fee >>> 32));
        result = 31 * result + (int) (timeoutSeconds ^ (timeoutSeconds >>> 32));
        return result;
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
