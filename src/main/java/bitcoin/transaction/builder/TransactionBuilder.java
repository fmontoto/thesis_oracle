package bitcoin.transaction.builder;

import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Input;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import core.Bet;
import core.Constants;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.*;
import static bitcoin.transaction.builder.OutputBuilder.createMultisigOutput;
import static bitcoin.transaction.builder.OutputBuilder.createPayToPubKeyOutput;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;

/**
 * Builds some useful transactions used in the protocol.
 * Created by fmontoto on 29-11-16.
 */
public class TransactionBuilder {
    static private final Charset utf8 = Charset.forName("utf-8");


    static Output multisigOrTimeoutOutput(long amount, List<String> wifMultisigAddresses, String timeoutWifAddress) {


        throw new NotImplementedException();
    }

    static public byte[] createSequenceNumber(TimeUnit timeUnit, long timeoutVal) {
        // Granularity is 512 seconds as defined by BIP 68
        long timeoutSeconds = timeUnit.toSeconds(timeoutVal) / 512;
        long sequenceNo = (long)(1 << 22) | timeoutSeconds;
        return serializeScriptNum(sequenceNo);
    }

    static private Transaction buildTx(int version, int locktime, Collection<Input> inputs, Collection<Output> outputs) {
        Transaction ret = new Transaction(version, locktime);
        for(Input i: inputs)
            ret.appendInput(i);
        for(Output o: outputs)
            ret.appendOutput(o);
        return ret;
    }

    static private Transaction buildTx(int version, int locktime, Input input, Output... outputs) {
        return buildTx(version, locktime, Arrays.asList(input),
                       Arrays.asList(outputs));
    }

    static public Transaction payToPublicKeyHash(
            AbsoluteOutput absOutput, String dstAddr, String changeAddr, long amount, long fee,
            int version, int locktime, int inputSequenceNo) throws IOException, NoSuchAlgorithmException {
        long available = absOutput.getValue();
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput, inputSequenceNo);
        Output o1 = createPayToPubKeyOutput(amount, dstAddr);
        if(available - amount - fee <= 0)
            return buildTx(version, locktime, input, o1);
        Output o2 = createPayToPubKeyOutput(available - amount - fee, changeAddr);
        return buildTx(version, locktime, input, o1, o2);
    }

    static public Transaction payToPublicKeyHash(AbsoluteOutput absOutput, String dstAddr,
                                                 String changeAddr, long amount, long fee) throws IOException, NoSuchAlgorithmException {
        return payToPublicKeyHash(absOutput, dstAddr, changeAddr, amount, fee, 1, 0, 0xffffffff);
    }

    static public Transaction payToPublicKeyHash(AbsoluteOutput absOutput, String dstAddr,
                                                 long amount) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        Output output = createPayToPubKeyOutput(amount, dstAddr);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToPublicKeyHash(List<AbsoluteOutput> absOutputs, String dstAddr,
                                                 long amount) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        List<Input> inputs = new ArrayList<Input>();
        List<Output> outputs = new ArrayList<Output>();

        for(AbsoluteOutput ao: absOutputs)
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(ao));

        outputs.add(createPayToPubKeyOutput(amount, dstAddr));
        return buildTx(version, locktime, inputs, outputs);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeem, long amount, int version,
                                              int locktime, int sequenceNo) throws NoSuchAlgorithmException {
        byte[] redeemScriptHash = r160SHA256Hash(scriptRedeem);
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput, sequenceNo);
        Output output = OutputBuilder.createPayToScriptHashOutput(amount, redeemScriptHash);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeem, long amount) throws NoSuchAlgorithmException {
        return payToScriptHash(absOutput, scriptRedeem, amount, 1, 0, 0xffffffff);
    }

    /**
     *
     * @param srcOutputs The first will be used as bet address.
     * @param wifChangeAddress
     * @param bet
     * @return
     */
    static public Transaction betPromise(List<AbsoluteOutput> srcOutputs, String wifChangeAddress,
                                         Bet bet, boolean iAmPlayerOne) throws IOException, NoSuchAlgorithmException {
        // TODO check the numbers
        final long fixedBytesNeeded = 90;
        final long bytesNeededByOracle = 43;
        final long estimatedTotalBytesNeededInTheBlockChain = fixedBytesNeeded + bet.getMaxOracles() * bytesNeededByOracle;
        final int locktime = 0;
        final int version = 1;
        BitcoinPublicKey[] playersPubKey = bet.getPlayersPubKey();
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();
        final long minimumAmount = estimatedTotalBytesNeededInTheBlockChain + bet.getOraclePayment() * bet.getMaxOracles();

        if(playersPubKey.length != 2)
            throw new NotImplementedException();
        if(bet.getAmount() < minimumAmount)
            throw new InvalidParameterException("The bet amount is too small, you need a bigger amount, at least " + minimumAmount);

        long change = 0;
        for(AbsoluteOutput absOutput : srcOutputs)
            change += absOutput.getValue();
        change = change - (bet.getAmount() + bet.getFirstPaymentAmount() * bet.getMaxOracles());
        if(change < 0)
            throw new InvalidParameterException("Not enough money to start the bet. At least " + (-change) + " more needed.");

        for(AbsoluteOutput srcOutput : srcOutputs)
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));

        Output data = OutputBuilder.createOpReturnOutput(bet.getWireRepresentation());

        Output prizePlayerA = OutputBuilder.oneSignatureOnTimeoutOrMultiSig(bet.getPlayersPubKey()[0].getKey(),
                                                              bet.getPlayersPubKey()[1].getKey(),
                                                              bet.getAmount(), TimeUnit.SECONDS, bet.getTimeoutSeconds());
        Output prizePlayerB = OutputBuilder.oneSignatureOnTimeoutOrMultiSig(bet.getPlayersPubKey()[1].getKey(),
                                                              bet.getPlayersPubKey()[0].getKey(),
                                                              bet.getAmount(), TimeUnit.SECONDS, bet.getTimeoutSeconds());

        outputs.add(data);
        outputs.add(prizePlayerA);
        outputs.add(prizePlayerB);
        int numOracles = bet.getMaxOracles();

        for(int i = 0; i < numOracles; i++) {
            outputs.add(createMultisigOutput(bet.getFirstPaymentAmount(), playersPubKey, playersPubKey.length));
        }

        if(change != 0)
            outputs.add(createPayToPubKeyOutput(change, wifChangeAddress));

        return buildTx(version, locktime, inputs, outputs);
    }

    // Oracle

    static Transaction opReturnOpTx(AbsoluteOutput absOutput, long fee, int version,
                                  int locktime, byte[] data) throws IOException, NoSuchAlgorithmException {
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = OutputBuilder.createOpReturnOutput(data);

        Output payToPubKeyOutput = createPayToPubKeyOutput(
                value, BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()),
                        false));

        return buildTx(version, locktime, input, dataOutput, payToPubKeyOutput);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput, String wifInscribeAddress,
                                               long fee, int version, int locktime) throws IOException, NoSuchAlgorithmException {
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = OutputBuilder.createOpReturnOutput(Constants.ORACLE_INSCRIPTION);
        Output changeOutput = createPayToPubKeyOutput(value, wifInscribeAddress);

        return buildTx(version, locktime, input, dataOutput, changeOutput);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput,
                                               String wifAddressToInscribe) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        final int fee = 1000;
        return inscribeAsOracle(absOutput, wifAddressToInscribe, fee, version, locktime);

    }
    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput,
                                               long fee, boolean testnet) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        String wifOracleAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()), testnet);
        return inscribeAsOracle(absOutput, wifOracleAddress, fee, version, locktime);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput, boolean testnet) throws IOException, NoSuchAlgorithmException {
        String wifOracleAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()), testnet);
        return inscribeAsOracle(absOutput, wifOracleAddress);
    }

}
