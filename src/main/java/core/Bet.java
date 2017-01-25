package core;

import bitcoin.key.BitcoinPublicKey;

import java.io.*;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.*;
import static core.Constants.charset;
import static core.Utils.byteArrayToHex;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 16-12-16.
 */
public class Bet {

    private String description;
    private int minOracles;
    private int maxOracles;
    private List<Oracle> oracles;
    private List<Oracle> backupOracles;
    private List<BitcoinPublicKey> participantOracles;
    private BitcoinPublicKey[] playersPubKey;
    private long relativeBetResolutionSecs;
    private Channel channel;
    Amounts amounts;


    public Bet(String description, int minOracles, int max_oracles, List<Oracle> oracles,
               List<Oracle> backupOracles, BitcoinPublicKey[] playersPubKey, Amounts amounts,
               TimeUnit timeoutUnit, long timeoutVal, Channel channel) {

        this.description = description;
        this.minOracles = minOracles;
        this.maxOracles = max_oracles;
        this.oracles = new ArrayList<>(oracles);
        this.backupOracles = new ArrayList<>(backupOracles);
        this.playersPubKey = playersPubKey;
        this.amounts = amounts;
        relativeBetResolutionSecs = timeoutUnit.toSeconds(timeoutVal);
        participantOracles = new LinkedList<>();
        this.channel = channel;

        if(playersPubKey.length != 2)
            throw new InvalidParameterException("Only two players accepted");
        if(maxOracles != this.oracles.size())
            throw new InvalidParameterException("oracles list must have same amount of oraclas as maxOracles specify.");
    }

    public Bet(String description, int num_oracles, List<Oracle>oracles, List<Oracle> backupOracles,
               BitcoinPublicKey[] playersPubKey, Amounts amounts, TimeUnit timeoutUnit, long timeoutVal,
               Channel channel) {
        this(description, num_oracles, num_oracles, oracles, backupOracles, playersPubKey, amounts,
                timeoutUnit, timeoutVal, channel);
    }

    public void addParticipantOracle(BitcoinPublicKey participantOracle) {
        participantOracles.add(participantOracle);
    }

    public byte[] serialize() throws IOException, NoSuchAlgorithmException {
        byte[] descriptionBytes = description.getBytes(charset);
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
                          , amounts.serialize()
                          , serializeVarInt(relativeBetResolutionSecs)
                          , channel.serialize()
        );
    }


    static public Bet fromSerialized(byte[] buffer, int offset) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        List<Oracle> oracles = new LinkedList<>(), backupOracles = new LinkedList<>();
        int descriptionSize = Math.toIntExact(readVarInt(buffer, offset));
        offset += varIntByteSize(descriptionSize);
        String description = new String(Arrays.copyOfRange(buffer, offset, offset + descriptionSize), charset);
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

        Amounts amounts = Amounts.fromSerialized(buffer, offset);
        offset += amounts.serializationSize();

        long timeoutSeconds = readVarInt(buffer, offset);
        offset += varIntByteSize(timeoutSeconds);

        Channel channel = Channel.fromSerialized(buffer, offset);

        Bet bet = new Bet(description, minOracles, maxOracles, oracles, backupOracles, playersKey, amounts,
                          TimeUnit.SECONDS, timeoutSeconds, channel);
        for(BitcoinPublicKey bitcoinPublicKey : participantOracles)
            bet.addParticipantOracle(bitcoinPublicKey);

        return bet;
    }

    static public Bet fromSerialized(byte[] buffer) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        return fromSerialized(buffer, 0);
    }

    public byte[] getHash() throws NoSuchAlgorithmException, IOException {
        return r160SHA256Hash(serialize());
    }

    public String getDescription(){
        return description;
    }

    public byte[] getDescriptionHash() throws NoSuchAlgorithmException {
        return r160SHA256Hash(description.getBytes(charset));
    }

    public byte[] getWireRepresentation() throws NoSuchAlgorithmException, IOException {
        return new BetTxForm(oracles, getDescriptionHash(), channel).serialize();
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

    public BitcoinPublicKey[] getPlayersPubKey() {
        return playersPubKey;
    }

    public List<Oracle> getOracles() {
        return oracles;
    }

    public long getAmount() {
        return amounts.getAmount();
    }

    public long getFee() {
        return amounts.getFee();
    }

    public int getMaxOracles() {
        return maxOracles;
    }

    public long getFirstPaymentAmount() {
        return amounts.getFirstPaymentAmount();
    }

    public long getOraclePayment() {
        return amounts.getOraclePayment();
    }

    public long getRelativeBetResolutionSecs() {
        return relativeBetResolutionSecs;
    }

    public long getOracleInscription() {
        return amounts.getOracleInscription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bet bet = (Bet) o;

        if (minOracles != bet.minOracles) return false;
        if (maxOracles != bet.maxOracles) return false;
        if (!amounts.equals(bet.amounts)) return false;
        if (relativeBetResolutionSecs != bet.relativeBetResolutionSecs) return false;
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
        result = 31 * result + amounts.hashCode();
        result = 31 * result + (int) (relativeBetResolutionSecs ^ (relativeBetResolutionSecs >>> 32));
        return result;
    }

    public int getOraclePos(String a) {
        for(Oracle o: oracles)
            if(o.getAddress().equals(a))
                return oracles.indexOf(o);
        throw new InvalidParameterException("Oracle not found in the Bet");
    }


    public static class Amounts {
        private final long firstPaymentAmount;
        private final long oraclePayment;
        private final long amount;
        private final long oracleInscription;
        private final long oraclePenalty;
        private final long fee;

        public Amounts(long firstPaymentAmount, long oraclePayment, long amount,
                       long oracleInscription, long oraclePenalty, long fee) {

            this.firstPaymentAmount = firstPaymentAmount;
            this.oraclePayment = oraclePayment;
            this.amount = amount;
            this.oracleInscription = oracleInscription;
            this.oraclePenalty = oraclePenalty;
            this.fee = fee;
        }

        public byte[] serialize() {
            return mergeArrays( serializeVarInt(firstPaymentAmount)
                              , serializeVarInt(oraclePayment)
                              , serializeVarInt(amount)
                              , serializeVarInt(oracleInscription)
                              , serializeVarInt(oraclePenalty)
                              , serializeVarInt(fee)
            );
        }

        public int serializationSize() {
            return serialize().length;
        }

        static public Amounts fromSerialized(byte[] buffer, int offset) {
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

            return new Amounts(firstPaymentAmount, oraclePayment, amount, oracleInscription, oraclePenalty, fee);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Amounts amounts = (Amounts) o;

            if (firstPaymentAmount != amounts.firstPaymentAmount) return false;
            if (oraclePayment != amounts.oraclePayment) return false;
            if (amount != amounts.amount) return false;
            if (oracleInscription != amounts.oracleInscription) return false;
            if (oraclePenalty != amounts.oraclePenalty) return false;
            return fee == amounts.fee;
        }

        @Override
        public int hashCode() {
            int result = (int) (firstPaymentAmount ^ (firstPaymentAmount >>> 32));
            result = 31 * result + (int) (oraclePayment ^ (oraclePayment >>> 32));
            result = 31 * result + (int) (amount ^ (amount >>> 32));
            result = 31 * result + (int) (oracleInscription ^ (oracleInscription >>> 32));
            result = 31 * result + (int) (oraclePenalty ^ (oraclePenalty >>> 32));
            result = 31 * result + (int) (fee ^ (fee >>> 32));
            return result;
        }

        public long getAmount() {
            return amount;
        }

        public long getFee() {
            return fee;
        }

        public long getFirstPaymentAmount() {
            return firstPaymentAmount;
        }

        public long getOraclePayment() {
            return oraclePayment;
        }

        public long getOracleInscription() {
            return oracleInscription;
        }
    }
}

