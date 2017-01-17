package bitcoin.transaction;

import bitcoin.key.BitcoinPublicKey;
import core.Bet;
import core.Constants;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getOpcode;
import static bitcoin.Constants.pushDataOpcode;
import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

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

    static private Input payToPublicKeyHashCreateInput(AbsoluteOutput absOutput, int inputSequenceNo) {
        // The script is the output to be redeem before signing it.
        return  new Input(inputSequenceNo, absOutput.getVout(),
                hexToByteArray(absOutput.getTxId()), absOutput.getScript());
    }

    static private Input payToPublicKeyHashCreateInput(AbsoluteOutput absoluteOutput) {
        return payToPublicKeyHashCreateInput(absoluteOutput, 0xffffffff);

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
        Input input = payToPublicKeyHashCreateInput(absOutput, inputSequenceNo);
        Output o1 = OutputBuilder.createPayToPubKeyOutput(amount, dstAddr);
        if(available - amount - fee <= 0)
            return buildTx(version, locktime, input, o1);
        Output o2 = OutputBuilder.createPayToPubKeyOutput(available - amount - fee, changeAddr);
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
        Input input = payToPublicKeyHashCreateInput(absOutput);
        Output output = OutputBuilder.createPayToPubKeyOutput(amount, dstAddr);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToPublicKeyHash(List<AbsoluteOutput> absOutputs, String dstAddr,
                                                 long amount) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        List<Input> inputs = new ArrayList<Input>();
        List<Output> outputs = new ArrayList<Output>();

        for(AbsoluteOutput ao: absOutputs)
            inputs.add(payToPublicKeyHashCreateInput(ao));

        outputs.add(OutputBuilder.createPayToPubKeyOutput(amount, dstAddr));
        return buildTx(version, locktime, inputs, outputs);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeem, long amount, int version,
                                              int locktime, int sequenceNo) throws NoSuchAlgorithmException {
        byte[] redeemScriptHash = r160SHA256Hash(scriptRedeem);
        Input input = payToPublicKeyHashCreateInput(absOutput, sequenceNo);
        Output output = OutputBuilder.createPayToScriptHashOutput(amount, redeemScriptHash);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeem, long amount) throws NoSuchAlgorithmException {
        return payToScriptHash(absOutput, scriptRedeem, amount, 1, 0, 0xffffffff);
    }

    static public Input redeemScriptHash(AbsoluteOutput absOutput, byte[] script, byte[] redeemScript) {
        return  new Input(0xffffffff, absOutput.getVout(),
                hexToByteArray(absOutput.getTxId()), absOutput.getScript());
    }

    static public Transaction betPromise(List<AbsoluteOutput> srcOutputs, String changeAddress, Bet bet) {
        Stack<Input> inputs = new Stack<>();
        for(AbsoluteOutput srcOutput : srcOutputs)
            inputs.push(payToPublicKeyHashCreateInput(srcOutput));

//        Output prize = createPayToScriptHashOutputFromScript(amount, script);





        throw new NotImplementedException();
    }

    // Oracle

    static private Output createOpReturnOutput(byte[] data) {
        long value = 0;
        byte[] script =  mergeArrays(
                new byte[]{getOpcode("OP_RETURN")},
                pushDataOpcode(data.length),
                data);
        return new Output(value, script);
    }

    static Transaction opReturnOpTx(AbsoluteOutput absOutput, long fee, int version,
                                  int locktime, byte[] data) throws IOException, NoSuchAlgorithmException {
        Input input = payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = createOpReturnOutput(data);

        Output payToPubKeyOutput = OutputBuilder.createPayToPubKeyOutput(
                value, BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()),
                        false));

        return buildTx(version, locktime, input, dataOutput, payToPubKeyOutput);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput, String wifInscribeAddress,
                                               long fee, int version, int locktime) throws IOException, NoSuchAlgorithmException {
        Input input = payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = createOpReturnOutput(Constants.ORACLE_INSCRIPTION);
        Output changeOutput = OutputBuilder.createPayToPubKeyOutput(value, wifInscribeAddress);

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
